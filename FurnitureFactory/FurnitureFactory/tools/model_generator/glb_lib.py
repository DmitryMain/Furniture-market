"""
Мини-библиотека для генерации демонстрационных GLB-моделей мебели.

Содержит:
  * генераторы геометрии (скруглённый параллелепипед, цилиндр, плоскость);
  * процедурные текстуры (ткань, дерево, кожа, контактная тень);
  * сборщик glTF 2.0 / GLB (узлы, меши, PBR-материалы, текстуры, анимации).

Зависимости: numpy, Pillow, scipy (только для размытия текстур).
"""
from __future__ import annotations

import io
import json
import math
import struct
from dataclasses import dataclass, field

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.ndimage import gaussian_filter

# ----------------------------------------------------------------------------
# Математика
# ----------------------------------------------------------------------------


def srgb_to_linear(c: float) -> float:
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def hex_linear(hex_color: str) -> list[float]:
    h = hex_color.lstrip("#")
    rgb = [int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4)]
    return [srgb_to_linear(c) for c in rgb]


def quat_from_axis_angle(axis, angle_rad) -> np.ndarray:
    axis = np.asarray(axis, dtype=np.float64)
    axis = axis / np.linalg.norm(axis)
    s = math.sin(angle_rad / 2.0)
    return np.array([axis[0] * s, axis[1] * s, axis[2] * s, math.cos(angle_rad / 2.0)])


def quat_to_matrix(q) -> np.ndarray:
    x, y, z, w = q
    return np.array([
        [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
        [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
        [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)],
    ])


def trs_matrix(t=(0, 0, 0), q=(0, 0, 0, 1), s=(1, 1, 1)) -> np.ndarray:
    m = np.eye(4)
    m[:3, :3] = quat_to_matrix(q) @ np.diag(s)
    m[:3, 3] = t
    return m


def translate(x=0.0, y=0.0, z=0.0) -> np.ndarray:
    return trs_matrix(t=(x, y, z))


def rot_x(deg) -> np.ndarray:
    return trs_matrix(q=quat_from_axis_angle((1, 0, 0), math.radians(deg)))


def rot_y(deg) -> np.ndarray:
    return trs_matrix(q=quat_from_axis_angle((0, 1, 0), math.radians(deg)))


def rot_z(deg) -> np.ndarray:
    return trs_matrix(q=quat_from_axis_angle((0, 0, 1), math.radians(deg)))


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def ease_in_out(t):
    t = np.clip(t, 0.0, 1.0)
    return 0.5 - 0.5 * np.cos(np.pi * t)


# ----------------------------------------------------------------------------
# Геометрия
# ----------------------------------------------------------------------------


@dataclass
class Mesh:
    pos: np.ndarray  # (N, 3) float32
    nrm: np.ndarray  # (N, 3) float32
    uv: np.ndarray   # (N, 2) float32
    idx: np.ndarray  # (M,) uint32

    def transformed(self, m: np.ndarray) -> "Mesh":
        p = np.c_[self.pos, np.ones(len(self.pos))] @ m.T
        nm = np.linalg.inv(m[:3, :3]).T
        n = self.nrm @ nm.T
        n /= np.linalg.norm(n, axis=1, keepdims=True) + 1e-12
        return Mesh(p[:, :3].astype(np.float32), n.astype(np.float32), self.uv.copy(), self.idx.copy())

    @staticmethod
    def merge(meshes: list["Mesh"]) -> "Mesh":
        pos, nrm, uv, idx = [], [], [], []
        base = 0
        for m in meshes:
            pos.append(m.pos)
            nrm.append(m.nrm)
            uv.append(m.uv)
            idx.append(m.idx + base)
            base += len(m.pos)
        return Mesh(np.vstack(pos).astype(np.float32), np.vstack(nrm).astype(np.float32),
                    np.vstack(uv).astype(np.float32), np.concatenate(idx).astype(np.uint32))


def _axis_coords(a: float, r: float, seg: int, div: int) -> np.ndarray:
    if r <= 1e-6:
        vals = np.linspace(-a, a, div + 2)
    else:
        th = np.linspace(0.0, np.pi / 2, seg + 1)
        left = -a + r - r * np.cos(th)
        mid = np.linspace(-a + r, a - r, div + 2)
        right = -left[::-1]
        vals = np.concatenate([left, mid, right])
    return np.unique(np.round(vals, 7))


def weld_normals(pos: np.ndarray, idx: np.ndarray) -> np.ndarray:
    """Сглаженные нормали с объединением совпадающих вершин (убирает швы между гранями)."""
    keys = np.round(pos, 5)
    _, inv = np.unique(keys, axis=0, return_inverse=True)
    inv = inv.reshape(-1)
    tri = idx.reshape(-1, 3)
    v0, v1, v2 = pos[tri[:, 0]], pos[tri[:, 1]], pos[tri[:, 2]]
    fn = np.cross(v1 - v0, v2 - v0)  # взвешено площадью
    acc = np.zeros((inv.max() + 1, 3))
    for k in range(3):
        np.add.at(acc, inv[tri[:, k]], fn)
    n = acc[inv]
    n /= np.linalg.norm(n, axis=1, keepdims=True) + 1e-12
    return n.astype(np.float32)


def rounded_box(w: float, h: float, d: float, r: float, seg: int = 5, div: int = 2,
                bulge: float = 0.0, bulge_faces=("+y",), tile: float = 0.25,
                grain_axis: int | None = None) -> Mesh:
    """Параллелепипед со скруглёнными рёбрами, центр в начале координат.

    bulge — «подушечность»: выпуклость центра указанных граней (м).
    """
    half = np.array([w, h, d], dtype=np.float64) / 2.0
    r = float(min(r, half.min() * 0.999))
    inner_half = half - r
    coords = [_axis_coords(half[k], r, seg, div) for k in range(3)]
    pos_all, nrm_all, uv_all, idx_all = [], [], [], []
    base = 0
    for k in range(3):
        u_ax, v_ax = (k + 1) % 3, (k + 2) % 3
        for s in (1, -1):
            U, V = coords[u_ax], coords[v_ax]
            gu, gv = np.meshgrid(U, V, indexing="ij")
            P = np.zeros(gu.shape + (3,))
            P[..., k] = s * half[k]
            P[..., u_ax] = gu
            P[..., v_ax] = gv
            P = P.reshape(-1, 3)
            inner = np.clip(P, -inner_half, inner_half)
            dvec = P - inner
            ln = np.linalg.norm(dvec, axis=1, keepdims=True)
            face_n = np.zeros(3)
            face_n[k] = s
            n = np.where(ln > 1e-9, dvec / np.maximum(ln, 1e-12), face_n)
            p = inner + n * r if r > 1e-6 else P.copy()
            face_name = ("+" if s > 0 else "-") + "xyz"[k]
            if bulge and face_name in bulge_faces:
                fu = np.clip(1 - (p[:, u_ax] / half[u_ax]) ** 2, 0, 1)
                fv = np.clip(1 - (p[:, v_ax] / half[v_ax]) ** 2, 0, 1)
                p = p + face_n * (bulge * fu * fv)[:, None]
            if grain_axis is not None and grain_axis == v_ax:
                uv = np.c_[p[:, v_ax], p[:, u_ax]] / tile
            else:
                uv = np.c_[p[:, u_ax], p[:, v_ax]] / tile
            nu, nv = len(U), len(V)
            ii, jj = np.meshgrid(np.arange(nu - 1), np.arange(nv - 1), indexing="ij")
            a = (ii * nv + jj).ravel()
            b = ((ii + 1) * nv + jj).ravel()
            c = ((ii + 1) * nv + jj + 1).ravel()
            dd = (ii * nv + jj + 1).ravel()
            if s > 0:
                tris = np.stack([a, b, c, a, c, dd], axis=1).reshape(-1, 3)
            else:
                tris = np.stack([a, c, b, a, dd, c], axis=1).reshape(-1, 3)
            pos_all.append(p)
            nrm_all.append(n)
            uv_all.append(uv)
            idx_all.append(tris.ravel() + base)
            base += len(p)
    pos = np.vstack(pos_all).astype(np.float32)
    idx = np.concatenate(idx_all).astype(np.uint32)
    nrm = weld_normals(pos, idx) if bulge else np.vstack(nrm_all).astype(np.float32)
    return Mesh(pos, nrm, np.vstack(uv_all).astype(np.float32), idx)


def box(w, h, d, tile=0.25, grain_axis=None) -> Mesh:
    return rounded_box(w, h, d, 0.0, div=0, tile=tile, grain_axis=grain_axis)


def cylinder(r_bottom: float, r_top: float, h: float, seg: int = 24, tile: float = 0.25,
             caps: bool = True) -> Mesh:
    """Цилиндр/конус вдоль Y, основание на y=0."""
    ang = np.linspace(0, 2 * np.pi, seg + 1)
    ca, sa = np.cos(ang), np.sin(ang)
    slope = (r_bottom - r_top) / h
    side_n = np.c_[ca, np.full_like(ca, slope), sa]
    side_n /= np.linalg.norm(side_n, axis=1, keepdims=True)
    pb = np.c_[ca * r_bottom, np.zeros_like(ca), sa * r_bottom]
    pt = np.c_[ca * r_top, np.full_like(ca, h), sa * r_top]
    pos = np.vstack([pb, pt])
    nrm = np.vstack([side_n, side_n])
    uv = np.vstack([np.c_[ang * r_bottom / tile, np.zeros_like(ang)],
                    np.c_[ang * r_top / tile, np.full_like(ang, h / tile)]])
    n = seg + 1
    idx = []
    for i in range(seg):
        a, b, c, d = i, i + 1, n + i + 1, n + i
        idx += [a, c, b, a, d, c]
    meshes = [Mesh(pos.astype(np.float32), nrm.astype(np.float32), uv.astype(np.float32),
                   np.array(idx, dtype=np.uint32))]
    if caps:
        for y, rr, up in ((0.0, r_bottom, -1.0), (h, r_top, 1.0)):
            cp = np.vstack([[0, y, 0], np.c_[ca * rr, np.full_like(ca, y), sa * rr]])
            cn = np.tile([0, up, 0], (len(cp), 1))
            cuv = np.c_[cp[:, 0], cp[:, 2]] / tile
            ci = []
            for i in range(seg):
                if up > 0:
                    ci += [0, i + 2, i + 1]
                else:
                    ci += [0, i + 1, i + 2]
            meshes.append(Mesh(cp.astype(np.float32), cn.astype(np.float32), cuv.astype(np.float32),
                               np.array(ci, dtype=np.uint32)))
    return Mesh.merge(meshes)


def plane_xz(w: float, d: float, y: float = 0.0) -> Mesh:
    pos = np.array([[-w / 2, y, -d / 2], [w / 2, y, -d / 2], [w / 2, y, d / 2], [-w / 2, y, d / 2]],
                   dtype=np.float32)
    nrm = np.tile(np.array([0, 1, 0], dtype=np.float32), (4, 1))
    uv = np.array([[0, 0], [1, 0], [1, 1], [0, 1]], dtype=np.float32)
    idx = np.array([0, 2, 1, 0, 3, 2], dtype=np.uint32)
    return Mesh(pos, nrm, uv, idx)


# ----------------------------------------------------------------------------
# Процедурные текстуры (все бесшовные)
# ----------------------------------------------------------------------------

TEX = 512


def _periodic_noise(size, sigma, seed, sx=None, sy=None):
    rng = np.random.default_rng(seed)
    n = rng.standard_normal((size, size))
    sig = (sy if sy is not None else sigma, sx if sx is not None else sigma)
    n = gaussian_filter(n, sigma=sig, mode="wrap")
    n -= n.mean()
    n /= (np.abs(n).max() + 1e-9)
    return n


def _to_png(arr: np.ndarray, jpeg: bool = False) -> bytes:
    """PNG для текстур с альфой, JPEG (q=90) для остальных — меньше размер GLB."""
    arr = np.clip(arr, 0, 1)
    img = Image.fromarray((arr * 255 + 0.5).astype(np.uint8))
    bio = io.BytesIO()
    if jpeg and img.mode == "RGB":
        img.save(bio, format="JPEG", quality=90, optimize=True)
    else:
        img.save(bio, format="PNG", optimize=True)
    return bio.getvalue()


def _normal_from_height(h: np.ndarray, strength: float) -> np.ndarray:
    dx = (np.roll(h, -1, axis=1) - np.roll(h, 1, axis=1)) * 0.5
    dy = (np.roll(h, -1, axis=0) - np.roll(h, 1, axis=0)) * 0.5
    n = np.stack([-dx * strength, dy * strength, np.ones_like(h)], axis=-1)
    n /= np.linalg.norm(n, axis=-1, keepdims=True)
    return n * 0.5 + 0.5


def fabric_textures(seed=1, threads=64, coarse=0.0):
    """Ткань полотняного переплетения. Возвращает (base_png, normal_png, base_array)."""
    s = TEX
    y, x = np.mgrid[0:s, 0:s].astype(np.float64)
    p = s / threads
    cx = (x % p) / p
    cy = (y % p) / p
    ix = (x // p).astype(int)
    iy = (y // p).astype(int)
    warp = np.sin(np.pi * cx) ** 0.7
    weft = np.sin(np.pi * cy) ** 0.7
    over = ((ix + iy) % 2 == 0)
    h = np.where(over, 0.55 * warp + 0.45 * weft * 0.6, 0.55 * weft + 0.45 * warp * 0.6)
    slub = _periodic_noise(s, 1.2, seed, sx=6.0, sy=0.8) * 0.18
    fuzz = _periodic_noise(s, 0.7, seed + 7) * 0.08
    big = _periodic_noise(s, 40.0, seed + 3) * (0.03 + coarse)
    h = h + slub + fuzz
    shade = 0.88 + 0.12 * (h - h.mean()) + big
    base = np.stack([shade] * 3, axis=-1)
    normal = _normal_from_height(gaussian_filter(h, 0.6, mode="wrap"), 2.2)
    return _to_png(base, jpeg=True), _to_png(normal, jpeg=True), base


def leather_textures(seed=11):
    s = TEX
    pebble = _periodic_noise(s, 2.2, seed) + 0.5 * _periodic_noise(s, 1.0, seed + 1)
    pebble = gaussian_filter(np.abs(pebble), 0.8, mode="wrap")
    mottling = _periodic_noise(s, 60.0, seed + 2) * 0.07
    shade = 0.90 + 0.10 * (pebble - pebble.mean()) + mottling
    base = np.stack([shade] * 3, axis=-1)
    normal = _normal_from_height(pebble, 1.6)
    return _to_png(base, jpeg=True), _to_png(normal, jpeg=True), base


def wood_textures(light_hex: str, dark_hex: str, seed=21, rings=9, figure=1.0):
    """Древесина с волокнами вдоль оси U. Возвращает (base_png, normal_png, base_array)."""
    s = TEX
    y, x = np.mgrid[0:s, 0:s].astype(np.float64) / s
    warp = _periodic_noise(s, 30.0, seed, sx=80.0, sy=18.0) * 0.9 * figure
    g = 0.5 + 0.5 * np.sin(2 * np.pi * (rings * y + warp))
    g = g ** 1.8
    fibers = _periodic_noise(s, 1.0, seed + 5, sx=40.0, sy=0.7) * 0.35
    pores = (_periodic_noise(s, 0.6, seed + 9, sx=6.0, sy=0.5) > 0.55) * 0.25
    t = np.clip(0.55 * g + 0.25 + fibers - pores, 0, 1)
    c1 = np.array([int(light_hex[i:i + 2], 16) for i in (1, 3, 5)]) / 255.0
    c2 = np.array([int(dark_hex[i:i + 2], 16) for i in (1, 3, 5)]) / 255.0
    base = c1[None, None, :] * (1 - t[..., None]) + c2[None, None, :] * t[..., None]
    height = gaussian_filter(fibers - pores, 0.7, mode="wrap")
    normal = _normal_from_height(height, 1.2)
    return _to_png(base, jpeg=True), _to_png(normal, jpeg=True), base


def contact_shadow_png(footprint_w: float, footprint_d: float, plane_w: float, plane_d: float,
                       strength: float = 0.55, blur_frac: float = 0.05, size: int = 256):
    """RGBA-текстура мягкой контактной тени (альфа = плотность тени)."""
    img = Image.new("L", (size, size), 0)
    dr = ImageDraw.Draw(img)
    fx = footprint_w / plane_w * size
    fz = footprint_d / plane_d * size
    x0, y0 = (size - fx) / 2, (size - fz) / 2
    dr.rounded_rectangle([x0, y0, x0 + fx, y0 + fz], radius=min(fx, fz) * 0.12, fill=255)
    core = np.asarray(img.filter(ImageFilter.GaussianBlur(size * blur_frac)), dtype=np.float64) / 255
    wide = np.asarray(img.filter(ImageFilter.GaussianBlur(size * blur_frac * 3.5)), dtype=np.float64) / 255
    a = np.clip(core * 0.75 + wide * 0.45, 0, 1) * strength
    # гарантированно прозрачные края
    yy, xx = np.mgrid[0:size, 0:size] / (size - 1)
    edge = np.clip(np.minimum.reduce([xx, 1 - xx, yy, 1 - yy]) / 0.06, 0, 1)
    a *= edge
    rgba = np.zeros((size, size, 4))
    rgba[..., 3] = a
    return _to_png(rgba), rgba


# ----------------------------------------------------------------------------
# Сборщик glTF / GLB
# ----------------------------------------------------------------------------


@dataclass
class Material:
    name: str
    color_hex: str = "#FFFFFF"
    roughness: float = 0.8
    metallic: float = 0.0
    base_png: bytes | None = None
    normal_png: bytes | None = None
    normal_scale: float = 1.0
    unlit: bool = False
    blend: bool = False
    double_sided: bool = False
    # для предпросмотра (рендер миниатюр)
    base_array: np.ndarray | None = None


@dataclass
class NodeData:
    name: str
    parts: list = field(default_factory=list)  # [(Mesh, Material)]
    translation: tuple = (0.0, 0.0, 0.0)
    rotation: tuple = (0.0, 0.0, 0.0, 1.0)
    children: list = field(default_factory=list)  # индексы
    parent: int | None = None


@dataclass
class Clip:
    name: str
    # список (node_index, path, times[np], values[np])
    channels: list = field(default_factory=list)

    @property
    def duration(self):
        return max(float(c[2][-1]) for c in self.channels) if self.channels else 0.0


class GlbBuilder:
    def __init__(self, name: str):
        self.name = name
        self.nodes: list[NodeData] = []
        self.roots: list[int] = []
        self.clips: list[Clip] = []

    # --- граф сцены
    def add_node(self, name, parent: int | None = None, translation=(0, 0, 0), rotation=(0, 0, 0, 1)) -> int:
        nd = NodeData(name=name, translation=tuple(float(v) for v in translation),
                      rotation=tuple(float(v) for v in rotation), parent=parent)
        self.nodes.append(nd)
        i = len(self.nodes) - 1
        if parent is None:
            self.roots.append(i)
        else:
            self.nodes[parent].children.append(i)
        return i

    def add_part(self, node: int, mesh: Mesh, material: Material, transform: np.ndarray | None = None):
        if transform is not None:
            mesh = mesh.transformed(transform)
        self.nodes[node].parts.append((mesh, material))

    def add_clip(self, clip: Clip):
        self.clips.append(clip)

    # --- сериализация
    def write(self, path: str):
        js = {"asset": {"version": "2.0", "generator": "FurnitureFactory demo model generator"},
              "scene": 0, "scenes": [{"name": self.name, "nodes": list(self.roots)}],
              "nodes": [], "meshes": [], "materials": [], "accessors": [], "bufferViews": [],
              "buffers": [], "textures": [], "images": [], "samplers": [], "animations": []}
        binbuf = bytearray()
        used_ext = set()

        def align():
            while len(binbuf) % 4:
                binbuf.append(0)

        def add_view(data: bytes, target=None):
            align()
            off = len(binbuf)
            binbuf.extend(data)
            bv = {"buffer": 0, "byteOffset": off, "byteLength": len(data)}
            if target is not None:
                bv["target"] = target
            js["bufferViews"].append(bv)
            return len(js["bufferViews"]) - 1

        def add_accessor(arr: np.ndarray, target=None, minmax=False):
            arr = np.ascontiguousarray(arr)
            if arr.dtype == np.float32:
                ct = 5126
            elif arr.dtype == np.uint16:
                ct = 5123
            elif arr.dtype == np.uint32:
                ct = 5125
            else:
                raise ValueError(arr.dtype)
            if arr.ndim == 1:
                typ, count = "SCALAR", len(arr)
            else:
                typ, count = {2: "VEC2", 3: "VEC3", 4: "VEC4"}[arr.shape[1]], arr.shape[0]
            acc = {"bufferView": add_view(arr.tobytes(), target), "componentType": ct, "count": count, "type": typ}
            if minmax:
                if arr.ndim == 1:
                    acc["min"], acc["max"] = [float(arr.min())], [float(arr.max())]
                else:
                    acc["min"] = [float(v) for v in arr.min(axis=0)]
                    acc["max"] = [float(v) for v in arr.max(axis=0)]
            js["accessors"].append(acc)
            return len(js["accessors"]) - 1

        js["samplers"].append({"magFilter": 9729, "minFilter": 9987, "wrapS": 10497, "wrapT": 10497})
        tex_cache: dict[int, int] = {}

        def add_texture(png: bytes) -> int:
            key = id(png)
            if key in tex_cache:
                return tex_cache[key]
            v = add_view(png)
            mime = "image/jpeg" if png[:2] == b"\xff\xd8" else "image/png"
            js["images"].append({"bufferView": v, "mimeType": mime})
            js["textures"].append({"sampler": 0, "source": len(js["images"]) - 1})
            tex_cache[key] = len(js["textures"]) - 1
            return tex_cache[key]

        mat_index: dict[int, int] = {}

        def material_id(m: Material) -> int:
            if id(m) in mat_index:
                return mat_index[id(m)]
            col = hex_linear(m.color_hex) + [1.0]
            pbr = {"baseColorFactor": col, "metallicFactor": m.metallic, "roughnessFactor": m.roughness}
            if m.base_png:
                pbr["baseColorTexture"] = {"index": add_texture(m.base_png)}
            md = {"name": m.name, "pbrMetallicRoughness": pbr}
            if m.normal_png:
                md["normalTexture"] = {"index": add_texture(m.normal_png), "scale": m.normal_scale}
            if m.unlit:
                md["extensions"] = {"KHR_materials_unlit": {}}
                used_ext.add("KHR_materials_unlit")
            if m.blend:
                md["alphaMode"] = "BLEND"
            if m.double_sided:
                md["doubleSided"] = True
            js["materials"].append(md)
            mat_index[id(m)] = len(js["materials"]) - 1
            return mat_index[id(m)]

        for nd in self.nodes:
            node_js = {"name": nd.name}
            if any(abs(v) > 1e-9 for v in nd.translation):
                node_js["translation"] = list(nd.translation)
            if any(abs(a - b) > 1e-9 for a, b in zip(nd.rotation, (0, 0, 0, 1))):
                node_js["rotation"] = list(nd.rotation)
            if nd.children:
                node_js["children"] = list(nd.children)
            if nd.parts:
                groups: dict[int, list] = {}
                mats: dict[int, Material] = {}
                for mesh, mat in nd.parts:
                    groups.setdefault(id(mat), []).append(mesh)
                    mats[id(mat)] = mat
                prims = []
                for key, meshes in groups.items():
                    m = Mesh.merge(meshes)
                    idx = m.idx.astype(np.uint16) if len(m.pos) < 65535 else m.idx.astype(np.uint32)
                    prim = {"attributes": {
                        "POSITION": add_accessor(m.pos, 34962, minmax=True),
                        "NORMAL": add_accessor(m.nrm, 34962),
                        "TEXCOORD_0": add_accessor(m.uv, 34962)},
                        "indices": add_accessor(idx, 34963),
                        "material": material_id(mats[key])}
                    prims.append(prim)
                js["meshes"].append({"name": nd.name + "_mesh", "primitives": prims})
                node_js["mesh"] = len(js["meshes"]) - 1
            js["nodes"].append(node_js)

        for clip in self.clips:
            samplers, channels = [], []
            for node, path_, times, values in clip.channels:
                t_acc = add_accessor(np.asarray(times, dtype=np.float32), minmax=True)
                v_acc = add_accessor(np.asarray(values, dtype=np.float32))
                samplers.append({"input": t_acc, "output": v_acc, "interpolation": "LINEAR"})
                channels.append({"sampler": len(samplers) - 1, "target": {"node": node, "path": path_}})
            js["animations"].append({"name": clip.name, "samplers": samplers, "channels": channels})

        align()
        js["buffers"].append({"byteLength": len(binbuf)})
        if used_ext:
            js["extensionsUsed"] = sorted(used_ext)
        for k in [k for k, v in js.items() if isinstance(v, list) and not v]:
            del js[k]

        jbytes = json.dumps(js, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        while len(jbytes) % 4:
            jbytes += b" "
        total = 12 + 8 + len(jbytes) + 8 + len(binbuf)
        with open(path, "wb") as f:
            f.write(struct.pack("<III", 0x46546C67, 2, total))
            f.write(struct.pack("<II", len(jbytes), 0x4E4F534A))
            f.write(jbytes)
            f.write(struct.pack("<II", len(binbuf), 0x004E4942))
            f.write(binbuf)
        return total

    # --- вспомогательное для предпросмотра
    def world_matrices(self, overrides: dict | None = None) -> list[np.ndarray]:
        """overrides: {node: (translation, rotation)} — поза в момент анимации."""
        overrides = overrides or {}
        mats = [None] * len(self.nodes)

        def visit(i, parent_m):
            nd = self.nodes[i]
            t, q = overrides.get(i, (nd.translation, nd.rotation))
            m = parent_m @ trs_matrix(t, q)
            mats[i] = m
            for c in nd.children:
                visit(c, m)

        for r in self.roots:
            visit(r, np.eye(4))
        return mats

    def pose_at(self, clip: Clip, t: float) -> dict:
        pose = {}
        for node, path_, times, values in clip.channels:
            times = np.asarray(times)
            values = np.asarray(values)
            tt = float(np.clip(t, times[0], times[-1]))
            k = int(np.searchsorted(times, tt, side="right") - 1)
            k = min(max(k, 0), len(times) - 2) if len(times) > 1 else 0
            if len(times) == 1:
                v = values[0]
            else:
                a = (tt - times[k]) / max(times[k + 1] - times[k], 1e-9)
                v = values[k] * (1 - a) + values[k + 1] * a
                if path_ == "rotation":
                    v = v / np.linalg.norm(v)
            cur = pose.get(node, (self.nodes[node].translation, self.nodes[node].rotation))
            if path_ == "translation":
                pose[node] = (tuple(v), cur[1])
            elif path_ == "rotation":
                pose[node] = (cur[0], tuple(v))
        return pose


def sample_channel(fn, duration: float, fps: int = 30):
    """Дискретизирует функцию позы fn(t)->vector в ключевые кадры LINEAR."""
    n = max(2, int(round(duration * fps)) + 1)
    times = np.linspace(0.0, duration, n)
    values = np.array([fn(t) for t in times], dtype=np.float64)
    return times.astype(np.float32), values.astype(np.float32)
