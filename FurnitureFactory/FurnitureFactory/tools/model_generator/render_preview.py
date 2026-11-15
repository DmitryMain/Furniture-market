"""
Простой программный растеризатор для превью моделей.

Используется для:
  * миниатюр товаров в каталоге (PNG с прозрачным фоном);
  * визуальной проверки анимаций (кадры клипов).

Это не замена Filament — только быстрый «студийный» рендер с ламбертовым
освещением, бликами и мягкой контактной тенью.
"""
from __future__ import annotations

import math

import numpy as np
from PIL import Image

from glb_lib import GlbBuilder, hex_linear, srgb_to_linear


def _look_at(eye, target, up=(0, 1, 0)):
    f = np.asarray(target, float) - np.asarray(eye, float)
    f /= np.linalg.norm(f)
    r = np.cross(f, up)
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    m = np.eye(4)
    m[0, :3], m[1, :3], m[2, :3] = r, u, -f
    m[:3, 3] = -m[:3, :3] @ np.asarray(eye, float)
    return m


def _tex_lin(arr):
    a = np.clip(arr[..., :3], 0, 1)
    return np.where(a <= 0.04045, a / 12.92, ((a + 0.055) / 1.055) ** 2.4)


def collect(builder: GlbBuilder, pose: dict | None):
    mats = builder.world_matrices(pose)
    items = []
    for i, nd in enumerate(builder.nodes):
        for mesh, mat in nd.parts:
            m = mesh.transformed(mats[i])
            items.append((m, mat))
    return items


def render(builder: GlbBuilder, clip=None, t: float = 0.0, yaw: float = -32.0, pitch: float = 17.0,
           size=(800, 600), ss: int = 2, fov: float = 28.0, margin: float = 1.08, bounds_items=None):
    pose = builder.pose_at(clip, t) if clip is not None else None
    items = collect(builder, pose)
    solid = [(m, mat) for m, mat in items if not mat.blend]
    ref = bounds_items if bounds_items is not None else solid
    allp = np.vstack([m.pos for m, _ in ref])
    lo, hi = allp.min(0), allp.max(0)
    center = (lo + hi) / 2
    radius = np.linalg.norm(hi - lo) / 2

    W, H = size[0] * ss, size[1] * ss
    aspect = W / H
    half_v = math.radians(fov) / 2
    half_h = math.atan(math.tan(half_v) * aspect)
    dist = radius * margin / math.sin(min(half_v, half_h))
    yr, pr = math.radians(yaw), math.radians(pitch)
    direction = np.array([math.sin(yr) * math.cos(pr), math.sin(pr), math.cos(yr) * math.cos(pr)])
    eye = center + direction * dist
    view = _look_at(eye, center)
    f = 1 / math.tan(half_v)

    color = np.zeros((H, W, 3))
    depth = np.full((H, W), np.inf)
    cover = np.zeros((H, W))
    key = np.array([-0.45, 0.85, 0.55]); key /= np.linalg.norm(key)
    fill = np.array([0.7, 0.35, 0.6]); fill /= np.linalg.norm(fill)
    rim = np.array([0.2, 0.5, -0.85]); rim /= np.linalg.norm(rim)

    def project(p):
        pv = np.c_[p, np.ones(len(p))] @ view.T
        z = -pv[:, 2]
        x = (pv[:, 0] / z) * f / aspect
        y = (pv[:, 1] / z) * f
        sx = (x * 0.5 + 0.5) * W
        sy = (1 - (y * 0.5 + 0.5)) * H
        return sx, sy, z

    def raster(mesh, mat, shadow_pass=False, shadow_acc=None):
        sx, sy, z = project(mesh.pos)
        tex = mat.base_array
        base_lin = np.array(hex_linear(mat.color_hex))
        if tex is not None and not shadow_pass:
            tex_l = _tex_lin(tex)
            th, tw = tex_l.shape[:2]
        tris = mesh.idx.reshape(-1, 3)
        for tri in tris:
            x0, x1, x2 = sx[tri]
            y0, y1, y2 = sy[tri]
            area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if abs(area) < 1e-9:
                continue
            if not shadow_pass and area > 0 and not mat.double_sided:
                continue  # отсечение задних граней (экранная ось Y направлена вниз)
            minx = max(int(math.floor(min(x0, x1, x2))), 0)
            maxx = min(int(math.ceil(max(x0, x1, x2))), W - 1)
            miny = max(int(math.floor(min(y0, y1, y2))), 0)
            maxy = min(int(math.ceil(max(y0, y1, y2))), H - 1)
            if minx > maxx or miny > maxy:
                continue
            px, py = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
            w0 = ((x1 - px) * (y2 - py) - (x2 - px) * (y1 - py)) / area
            w1 = ((x2 - px) * (y0 - py) - (x0 - px) * (y2 - py)) / area
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            zt = z[tri]
            # перспективно-корректная интерполяция
            iz = w0 / zt[0] + w1 / zt[1] + w2 / zt[2]
            zz = 1 / iz
            sub_d = depth[miny:maxy + 1, minx:maxx + 1]
            vis = inside & (zz < sub_d - (1e-4 if shadow_pass else 0))
            if not vis.any():
                continue
            b0, b1, b2 = w0 / zt[0] * zz, w1 / zt[1] * zz, w2 / zt[2] * zz
            uv = mesh.uv[tri]
            u = b0 * uv[0, 0] + b1 * uv[1, 0] + b2 * uv[2, 0]
            v = b0 * uv[0, 1] + b1 * uv[1, 1] + b2 * uv[2, 1]
            if shadow_pass:
                ta = mat.base_array[..., 3]
                th2, tw2 = ta.shape
                tx = np.clip((u * (tw2 - 1)).astype(int), 0, tw2 - 1)
                ty = np.clip((v * (th2 - 1)).astype(int), 0, th2 - 1)
                a = ta[ty, tx]
                sub = shadow_acc[miny:maxy + 1, minx:maxx + 1]
                sub[vis] = 1 - (1 - sub[vis]) * (1 - a[vis])
                continue
            nrm = mesh.nrm[tri]
            n = b0[..., None] * nrm[0] + b1[..., None] * nrm[1] + b2[..., None] * nrm[2]
            n /= np.linalg.norm(n, axis=-1, keepdims=True) + 1e-12
            if tex is not None:
                tx = (np.mod(u, 1.0) * (tw - 1)).astype(int)
                ty = (np.mod(v, 1.0) * (th - 1)).astype(int)
                albedo = tex_l[ty, tx] * base_lin
            else:
                albedo = np.broadcast_to(base_lin, n.shape)
            pos = (b0[..., None] * mesh.pos[tri[0]] + b1[..., None] * mesh.pos[tri[1]]
                   + b2[..., None] * mesh.pos[tri[2]])
            vdir = eye - pos
            vdir /= np.linalg.norm(vdir, axis=-1, keepdims=True)
            ndk = np.clip(n @ key, 0, 1)
            ndf = np.clip(n @ fill, 0, 1)
            ndr = np.clip(n @ rim, 0, 1)
            hemi = 0.55 + 0.45 * n[..., 1]
            diffuse = 0.95 * ndk + 0.30 * ndf + 0.15 * ndr + 0.42 * hemi
            hv = key + vdir
            hv /= np.linalg.norm(hv, axis=-1, keepdims=True)
            shin = 2.0 / max(mat.roughness ** 4, 0.002)
            spec = np.clip(np.sum(n * hv, axis=-1), 0, 1) ** min(shin, 400) * (1 - mat.roughness) ** 2
            if mat.metallic > 0.5:
                col = albedo * (0.35 * diffuse[..., None]) + albedo * spec[..., None] * 2.5 + 0.06 * hemi[..., None]
            else:
                col = albedo * diffuse[..., None] + 0.5 * spec[..., None]
            if mat.unlit:
                col = albedo
            sub_c = color[miny:maxy + 1, minx:maxx + 1]
            sub_c[vis] = col[vis]
            sub_d[vis] = zz[vis]
            cover[miny:maxy + 1, minx:maxx + 1][vis] = 1.0

    for m, mat in solid:
        raster(m, mat)
    shadow_acc = np.zeros((H, W))
    for m, mat in items:
        if mat.blend:
            raster(m, mat, shadow_pass=True, shadow_acc=shadow_acc)

    mapped = 1 - np.exp(-color * 1.45)
    srgb = np.where(mapped <= 0.0031308, mapped * 12.92, 1.055 * np.power(np.clip(mapped, 0, 1), 1 / 2.4) - 0.055)
    alpha = np.where(cover > 0, 1.0, shadow_acc)
    rgb = np.where(cover[..., None] > 0, srgb, 0.0)
    rgba = np.dstack([rgb, alpha])
    img = Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")
    if ss > 1:
        img = img.resize(size, Image.LANCZOS)
    return img


def on_background(img: Image.Image, hex_bg="#F4F0EC") -> Image.Image:
    bg = Image.new("RGBA", img.size, hex_bg)
    bg.alpha_composite(img)
    return bg.convert("RGB")
