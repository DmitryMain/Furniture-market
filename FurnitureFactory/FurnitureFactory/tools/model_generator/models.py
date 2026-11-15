"""
Демонстрационные модели мебели Furniture Factory.

Каждая функция строит модель в метрах (ось Y — вверх, перед мебели смотрит в +Z,
пол на y = 0) и возвращает GlbBuilder. Анимированные части — отдельные узлы,
анимации записываются как настоящие glTF-клипы (translation/rotation).
"""
from __future__ import annotations

import math

import numpy as np

from glb_lib import (Clip, GlbBuilder, Material, contact_shadow_png, cylinder, ease_in_out,
                     fabric_textures, leather_textures, plane_xz, quat_from_axis_angle, rot_x,
                     rot_y, rot_z, rounded_box, box, sample_channel, smoothstep, translate,
                     wood_textures)

FPS = 30

# ----------------------------------------------------------------------------
# Общие материалы
# ----------------------------------------------------------------------------

_FAB_A = fabric_textures(seed=1, threads=64)
_FAB_B = fabric_textures(seed=4, threads=48, coarse=0.04)
_LEATHER = leather_textures(seed=11)
_OAK = wood_textures("#D2AF84", "#A57A4F", seed=21, rings=7)
_WALNUT = wood_textures("#8A5D3F", "#62412B", seed=31, rings=14, figure=0.35)
_LIGHT_WOOD = wood_textures("#E6D3B8", "#C9AE8A", seed=41, rings=6, figure=0.6)


def fabric(name, hex_color, variant="A"):
    t = _FAB_A if variant == "A" else _FAB_B
    return Material(name, hex_color, roughness=0.92, base_png=t[0], normal_png=t[1], normal_scale=0.7,
                    base_array=t[2])


def leather(name, hex_color):
    return Material(name, hex_color, roughness=0.48, base_png=_LEATHER[0], normal_png=_LEATHER[1],
                    normal_scale=0.5, base_array=_LEATHER[2])


def wood(name, kind="oak"):
    t = {"oak": _OAK, "walnut": _WALNUT, "light": _LIGHT_WOOD}[kind]
    return Material(name, "#FFFFFF", roughness=0.55, base_png=t[0], normal_png=t[1], normal_scale=0.35,
                    base_array=t[2])


def metal(name, hex_color="#1E1E1E", roughness=0.38):
    return Material(name, hex_color, roughness=roughness, metallic=1.0)


def add_contact_shadow(b: GlbBuilder, parent, footprint_w, footprint_d, cx=0.0, cz=0.0, y=0.002,
                       strength=0.55):
    pw, pd = footprint_w * 1.7 + 0.3, footprint_d * 1.7 + 0.3
    png, rgba = contact_shadow_png(footprint_w, footprint_d, pw, pd, strength=strength)
    mat = Material("ContactShadow", "#000000", roughness=1.0, base_png=png, unlit=True, blend=True,
                   base_array=rgba)
    node = b.add_node("ContactShadow", parent=parent)
    b.add_part(node, plane_xz(pw, pd, y), mat, translate(cx, 0, cz))
    return node


def quat_x(deg):
    return quat_from_axis_angle((1, 0, 0), math.radians(deg))


def quat_y(deg):
    return quat_from_axis_angle((0, 1, 0), math.radians(deg))


def reverse_clip_fn(fn, active_duration):
    """Поза обратного клипа: проигрывает fn задом наперёд, затем держит начальное положение."""
    return lambda t: fn(max(0.0, active_duration - t))


def make_clip(b: GlbBuilder, name: str, duration: float, channels: list):
    """channels: [(node, path, fn(t)->vec)]"""
    clip = Clip(name)
    for node, path_, fn in channels:
        times, values = sample_channel(fn, duration, FPS)
        if path_ == "rotation":
            values = values / np.linalg.norm(values, axis=1, keepdims=True)
        clip.channels.append((node, path_, times, values))
    b.add_clip(clip)
    return clip


# ----------------------------------------------------------------------------
# 1. Диван-кровать «Комфорт» (механизм «еврокнижка»)
# ----------------------------------------------------------------------------

def sofa_komfort():
    b = GlbBuilder("Sofa_Komfort")
    body = fabric("Fabric_Greige", "#CDC3B6")
    cushion = fabric("Fabric_Greige_Cushion", "#D6CDC1")
    legs = metal("Legs_Dark", "#2A2725", 0.45)

    root = b.add_node("Sofa_Komfort")
    frame = b.add_node("Frame", parent=root)
    # цоколь (внутри — бельевой ящик, его крышка = основание спального места)
    b.add_part(frame, rounded_box(1.72, 0.20, 0.88, 0.02, seg=3), body, translate(0, 0.16, -0.04))
    for sx in (-1, 1):
        b.add_part(frame, rounded_box(0.20, 0.60, 0.96, 0.06, seg=5, bulge=0.01), body,
                   translate(sx * 0.96, 0.36, 0.0))
        for sz in (-1, 1):
            b.add_part(frame, cylinder(0.022, 0.028, 0.06, seg=20), legs, translate(sx * 0.98, 0.0, sz * 0.40))
    b.add_part(frame, rounded_box(1.72, 0.56, 0.18, 0.05, seg=4), body, translate(0, 0.54, -0.39))
    add_contact_shadow(b, root, 2.14, 0.98)

    # выдвижная часть: фасад + рама сиденья + подушки сиденья
    seat = b.add_node("Seat", parent=root)
    b.add_part(seat, rounded_box(1.72, 0.20, 0.08, 0.02, seg=3), body, translate(0, 0.16, 0.44))
    b.add_part(seat, rounded_box(1.72, 0.10, 0.78, 0.025, seg=3), body, translate(0, 0.31, 0.09))
    for sx in (-1, 1):
        b.add_part(seat, rounded_box(0.85, 0.16, 0.76, 0.06, seg=5, bulge=0.018), cushion,
                   translate(sx * 0.43, 0.44, 0.09))
    add_contact_shadow(b, seat, 1.74, 0.70, cz=0.12, y=0.003, strength=0.30)

    # спинка (вторая половина спального места)
    back_up_t = np.array([0.0, 0.7465, -0.195])
    back_flat_t = np.array([0.0, 0.43, -0.06])
    a_up, a_flat = -10.0, 90.0
    backrest = b.add_node("Backrest", parent=root, translation=back_up_t, rotation=quat_x(a_up))
    for sx in (-1, 1):
        b.add_part(backrest, rounded_box(0.85, 0.46, 0.18, 0.07, seg=5, bulge=0.018, bulge_faces=("+z",)),
                   cushion, translate(sx * 0.43, 0, 0))

    slide = 0.48
    t_seat, t_back0, t_back1, total = 1.5, 1.5, 3.3, 3.5

    def seat_t(t):
        e = ease_in_out(t / t_seat)
        return np.array([0.0, 0.0, slide * e])

    def back_e(t):
        return ease_in_out((t - t_back0) / (t_back1 - t_back0))

    def back_t(t):
        e = back_e(t)
        y = back_up_t[1] + (back_flat_t[1] - back_up_t[1]) * e + 0.16 * math.sin(math.pi * e)
        z = back_up_t[2] + (back_flat_t[2] - back_up_t[2]) * float(smoothstep(0.0, 0.5, e))
        return np.array([0.0, y, z])

    def back_r(t):
        return quat_x(a_up + (a_flat - a_up) * back_e(t))

    make_clip(b, "Раскладывание", total, [(seat, "translation", seat_t), (backrest, "translation", back_t),
                                          (backrest, "rotation", back_r)])
    make_clip(b, "Складывание", total, [(seat, "translation", reverse_clip_fn(seat_t, 3.3)),
                                        (backrest, "translation", reverse_clip_fn(back_t, 3.3)),
                                        (backrest, "rotation", reverse_clip_fn(back_r, 3.3))])
    return b


# ----------------------------------------------------------------------------
# 2. Диван «Сканди» (без механизма трансформации)
# ----------------------------------------------------------------------------

def sofa_skandi():
    b = GlbBuilder("Sofa_Skandi")
    body = fabric("Fabric_Sage", "#9BA39A", "B")
    cushion = fabric("Fabric_Sage_Cushion", "#A7AEA5", "B")
    legs = wood("Oak_Legs", "oak")
    root = b.add_node("Sofa_Skandi")
    n = b.add_node("Body", parent=root)
    b.add_part(n, rounded_box(2.02, 0.16, 0.86, 0.03, seg=3), body, translate(0, 0.25, 0))
    for sx in (-1, 1):
        b.add_part(n, rounded_box(0.14, 0.42, 0.86, 0.06, seg=5, bulge=0.01), body, translate(sx * 1.08, 0.38, 0))
        for sz in (-1, 1):
            leg = cylinder(0.016, 0.024, 0.17, seg=18)
            b.add_part(n, leg, legs, translate(sx * 1.02, 0.0, sz * 0.34) @ rot_z(sx * 6) @ rot_x(-sz * 6))
    b.add_part(n, rounded_box(2.02, 0.42, 0.14, 0.05, seg=4), body, translate(0, 0.54, -0.36))
    for i, x in enumerate((-0.67, 0.0, 0.67)):
        b.add_part(n, rounded_box(0.66, 0.15, 0.68, 0.06, seg=5, bulge=0.02), cushion, translate(x, 0.40, 0.06))
        b.add_part(n, rounded_box(0.64, 0.40, 0.16, 0.07, seg=5, bulge=0.02, bulge_faces=("+z",)), cushion,
                   translate(x, 0.66, -0.24) @ rot_x(-12))
    add_contact_shadow(b, root, 2.18, 0.88)
    return b


# ----------------------------------------------------------------------------
# 3. Кресло «Лофт»
# ----------------------------------------------------------------------------

def armchair_loft():
    b = GlbBuilder("Armchair_Loft")
    seat_mat = leather("Leather_Cognac", "#A0623A")
    frame_mat = metal("Frame_Black", "#1C1C1C", 0.35)
    arm_mat = wood("Walnut_Arms", "walnut")
    root = b.add_node("Armchair_Loft")
    n = b.add_node("Body", parent=root)
    tube = 0.025
    for sx in (-1, 1):
        x = sx * 0.36
        # полозья
        b.add_part(n, rounded_box(tube, tube, 0.78, 0.01, seg=3), frame_mat, translate(x, tube / 2, 0.0))
        b.add_part(n, rounded_box(tube, 0.58, tube, 0.01, seg=3), frame_mat, translate(x, 0.29, 0.37))
        b.add_part(n, rounded_box(tube, 0.70, tube, 0.01, seg=3), frame_mat, translate(x, 0.35, -0.37))
        b.add_part(n, rounded_box(tube, tube, 0.76, 0.01, seg=3), frame_mat, translate(x, 0.58, 0.0))
        b.add_part(n, rounded_box(0.07, 0.03, 0.80, 0.012, seg=3, grain_axis=2), arm_mat, translate(x, 0.605, 0.0))
    for z in (-0.30, 0.30):
        b.add_part(n, rounded_box(0.72, tube, tube, 0.01, seg=3), frame_mat, translate(0, 0.25, z))
    b.add_part(n, rounded_box(0.66, 0.14, 0.66, 0.05, seg=5, bulge=0.02), seat_mat, translate(0, 0.33, 0.04))
    b.add_part(n, rounded_box(0.66, 0.50, 0.14, 0.06, seg=5, bulge=0.02, bulge_faces=("+z",)), seat_mat,
               translate(0, 0.66, -0.25) @ rot_x(-14))
    add_contact_shadow(b, root, 0.80, 0.82)
    return b


# ----------------------------------------------------------------------------
# 4. Кресло-реклайнер «Релакс» (выдвижная подножка + откидная спинка)
# ----------------------------------------------------------------------------

def armchair_relax():
    b = GlbBuilder("Armchair_Relax")
    body = fabric("Fabric_Taupe", "#7A6C62")
    cushion = fabric("Fabric_Taupe_Cushion", "#857669")
    legs = metal("Feet_Dark", "#262321", 0.5)
    root = b.add_node("Armchair_Relax")
    base = b.add_node("Base", parent=root)
    b.add_part(base, rounded_box(0.62, 0.22, 0.78, 0.03, seg=3), body, translate(0, 0.16, -0.02))
    for sx in (-1, 1):
        b.add_part(base, rounded_box(0.17, 0.58, 0.88, 0.07, seg=5, bulge=0.012), body,
                   translate(sx * 0.395, 0.34, 0.0))
        for sz in (-1, 1):
            b.add_part(base, cylinder(0.02, 0.025, 0.05, seg=16), legs, translate(sx * 0.40, 0, sz * 0.36))
    b.add_part(base, rounded_box(0.62, 0.15, 0.66, 0.06, seg=5, bulge=0.02), cushion, translate(0, 0.345, 0.06))
    add_contact_shadow(b, root, 0.96, 0.92)

    # спинка: шарнир у задней кромки сиденья
    pivot_back = (0.0, 0.40, -0.26)
    back = b.add_node("Backrest", parent=root, translation=pivot_back, rotation=quat_x(-8))
    b.add_part(back, rounded_box(0.62, 0.66, 0.20, 0.08, seg=5, bulge=0.025, bulge_faces=("+z",)), cushion,
               translate(0, 0.33, -0.02))
    # подножка: в сложенном виде — передняя панель, шарнир под передней кромкой сиденья
    pivot_foot = np.array([0.0, 0.27, 0.40])
    foot = b.add_node("Footrest", parent=root, translation=pivot_foot)
    b.add_part(foot, rounded_box(0.56, 0.22, 0.07, 0.03, seg=4, bulge=0.012, bulge_faces=("+z",)), cushion,
               translate(0, -0.11, 0.0))
    total = 2.6

    def foot_e(t):
        return ease_in_out(t / 1.3)

    def foot_t(t):
        e = foot_e(t)
        return pivot_foot + np.array([0.0, 0.07 * e, 0.16 * e])

    def foot_r(t):
        return quat_x(-90.0 * foot_e(t))

    def back_r(t):
        e = ease_in_out((t - 0.6) / 1.8)
        return quat_x(-8 - 30 * e)

    make_clip(b, "Разложить", total, [(foot, "translation", foot_t), (foot, "rotation", foot_r),
                                      (back, "rotation", back_r)])
    make_clip(b, "Сложить", total, [(foot, "translation", reverse_clip_fn(foot_t, 2.4)),
                                    (foot, "rotation", reverse_clip_fn(foot_r, 2.4)),
                                    (back, "rotation", reverse_clip_fn(back_r, 2.4))])
    return b


# ----------------------------------------------------------------------------
# 5. Стол «Элегант» (раздвижной, со вставкой)
# ----------------------------------------------------------------------------

def table_elegant():
    b = GlbBuilder("Table_Elegant")
    oak = wood("Oak_Solid", "oak")
    oak_dark = wood("Oak_Leaf", "oak")
    root = b.add_node("Table_Elegant")
    top_y, top_t = 0.73, 0.04
    halves = []
    for sx, name in ((-1, "LeftHalf"), (1, "RightHalf")):
        h = b.add_node(name, parent=root)
        halves.append((h, sx))
        b.add_part(h, rounded_box(0.80, top_t, 0.90, 0.008, seg=3, grain_axis=0), oak,
                   translate(sx * 0.40, top_y, 0))
        # царги (вдоль длинной стороны — только у наружной части, центр свободен для вставки)
        b.add_part(h, box(0.48, 0.08, 0.025, grain_axis=0), oak, translate(sx * 0.49, top_y - 0.06, 0.38))
        b.add_part(h, box(0.48, 0.08, 0.025, grain_axis=0), oak, translate(sx * 0.49, top_y - 0.06, -0.38))
        b.add_part(h, box(0.025, 0.08, 0.76, grain_axis=2), oak, translate(sx * 0.735, top_y - 0.06, 0))
        for sz in (-1, 1):
            b.add_part(h, rounded_box(0.06, top_y - top_t / 2, 0.06, 0.008, seg=3, grain_axis=1), oak,
                       translate(sx * 0.72, (top_y - top_t / 2) / 2, sz * 0.37))
        add_contact_shadow(b, h, 0.20, 0.80, cx=sx * 0.72, y=0.002 + (0.0005 if sx > 0 else 0), strength=0.4)
    leaf_down = np.array([0.0, top_y - 0.046, 0.0])
    leaf = b.add_node("Leaf", parent=root, translation=leaf_down)
    b.add_part(leaf, rounded_box(0.40, top_t, 0.90, 0.006, seg=3, grain_axis=0), oak_dark)
    shift = 0.20
    total = 2.5

    def half_fn(sx):
        return lambda t: np.array([sx * shift * ease_in_out(t / 1.3), 0.0, 0.0])

    def leaf_t(t):
        e = ease_in_out((t - 1.35) / 0.9)
        return leaf_down + np.array([0.0, 0.046 * e, 0.0])

    chans = [(h, "translation", half_fn(sx)) for h, sx in halves] + [(leaf, "translation", leaf_t)]
    make_clip(b, "Раздвинуть стол", total, chans)
    make_clip(b, "Сдвинуть стол", total, [(n, p, reverse_clip_fn(f, 2.3)) for n, p, f in chans])
    return b


# ----------------------------------------------------------------------------
# 6. Стул «Норд»
# ----------------------------------------------------------------------------

def chair_nord():
    b = GlbBuilder("Chair_Nord")
    oak = wood("Oak_Chair", "oak")
    pad = fabric("Fabric_Linen", "#D9CFC0")
    root = b.add_node("Chair_Nord")
    n = b.add_node("Body", parent=root)
    seat_y = 0.45
    for sx in (-1, 1):
        b.add_part(n, cylinder(0.016, 0.019, seat_y, seg=18), oak, translate(sx * 0.19, 0, 0.18))
        # задние ножки переходят в стойки спинки
        b.add_part(n, cylinder(0.016, 0.019, 0.86, seg=18), oak, translate(sx * 0.19, 0, -0.19) @ rot_x(-4))
    b.add_part(n, rounded_box(0.44, 0.035, 0.43, 0.012, seg=3, grain_axis=0), oak, translate(0, seat_y, 0.0))
    b.add_part(n, rounded_box(0.40, 0.035, 0.38, 0.015, seg=4, bulge=0.008), pad, translate(0, seat_y + 0.035, 0.01))
    b.add_part(n, rounded_box(0.42, 0.10, 0.02, 0.008, seg=3, grain_axis=0), oak,
               translate(0, 0.80, -0.245) @ rot_x(-4))
    b.add_part(n, rounded_box(0.40, 0.03, 0.018, 0.006, seg=3, grain_axis=0), oak,
               translate(0, 0.64, -0.234) @ rot_x(-4))
    for z in (0.18, -0.19):
        b.add_part(n, cylinder(0.009, 0.009, 0.36, seg=12), oak, translate(-0.18, 0.18, z) @ rot_z(-90))
    add_contact_shadow(b, root, 0.44, 0.46)
    return b


# ----------------------------------------------------------------------------
# 7. Шкаф «Классик» (распашные двери)
# ----------------------------------------------------------------------------

def wardrobe_classic():
    b = GlbBuilder("Wardrobe_Classic")
    outer = wood("Walnut_Body", "walnut")
    inner = wood("Light_Interior", "light")
    chrome = metal("Chrome", "#C8C8C8", 0.22)
    handle = metal("Handle_Brass", "#B08D57", 0.3)
    root = b.add_node("Wardrobe_Classic")
    body = b.add_node("Body", parent=root)
    W, H, D, t = 1.0, 2.0, 0.58, 0.018
    plinth = 0.08
    b.add_part(body, box(W, plinth, D - 0.04, grain_axis=0), outer, translate(0, plinth / 2, -0.01))
    for sx in (-1, 1):
        b.add_part(body, box(t, H - plinth, D, grain_axis=1), outer, translate(sx * (W / 2 - t / 2), plinth + (H - plinth) / 2, 0))
    b.add_part(body, box(W, t, D, grain_axis=0), outer, translate(0, H - t / 2, 0))
    b.add_part(body, box(W - 2 * t, t, D - t, grain_axis=0), inner, translate(0, plinth + t / 2, t / 2))
    b.add_part(body, box(W - 2 * t, H - plinth, 0.006, grain_axis=1), inner, translate(0, plinth + (H - plinth) / 2, -D / 2 + 0.003))
    # внутреннее наполнение
    b.add_part(body, box(W - 2 * t, t, D - 0.05, grain_axis=0), inner, translate(0, 1.72, 0.0))
    b.add_part(body, box(W - 2 * t, t, D - 0.05, grain_axis=0), inner, translate(0, 0.42, 0.0))
    b.add_part(body, cylinder(0.012, 0.012, W - 2 * t, seg=16), chrome, translate(-(W / 2 - t), 1.62, 0.0) @ rot_z(-90))
    # сложенные пледы на нижней полке
    blanket1 = fabric("Fabric_Blanket", "#C4B6A6")
    blanket2 = fabric("Fabric_Blanket2", "#8E8478", "B")
    b.add_part(body, rounded_box(0.40, 0.10, 0.36, 0.03, seg=3, bulge=0.01), blanket1, translate(-0.2, 0.488, 0.02))
    b.add_part(body, rounded_box(0.38, 0.09, 0.34, 0.03, seg=3, bulge=0.01), blanket2, translate(0.22, 0.483, 0.03))
    add_contact_shadow(b, root, 1.02, 0.60)

    door_w, door_h = W / 2 - 0.004, H - plinth - 0.01
    door_y = plinth + 0.005 + door_h / 2
    z_front = D / 2 + 0.01
    doors = []
    for sx, name in ((-1, "DoorLeft"), (1, "DoorRight")):
        hinge = np.array([sx * W / 2, 0.0, z_front])
        d = b.add_node(name, parent=root, translation=hinge)
        doors.append((d, sx))
        local_cx = -sx * door_w / 2
        b.add_part(d, rounded_box(door_w, door_h, 0.02, 0.004, seg=2, grain_axis=1), outer,
                   translate(local_cx, door_y, 0))
        # филёнка
        b.add_part(d, rounded_box(door_w - 0.12, door_h - 0.16, 0.006, 0.01, seg=3, grain_axis=1), outer,
                   translate(local_cx, door_y, 0.012))
        b.add_part(d, rounded_box(0.014, 0.28, 0.014, 0.006, seg=3), handle,
                   translate(-sx * (door_w - 0.05), 1.05, 0.032))
    total = 2.3

    def door_fn(sx, delay):
        return lambda t: quat_y(sx * 100.0 * ease_in_out((t - delay) / 1.8))

    chans = [(d, "rotation", door_fn(sx, 0.0 if sx < 0 else 0.2)) for d, sx in doors]
    make_clip(b, "Открыть двери", total, chans)
    make_clip(b, "Закрыть двери", total, [(n, p, reverse_clip_fn(f, 2.0)) for n, p, f in chans])
    return b


# ----------------------------------------------------------------------------
# 8. Комод «Практик» (выдвижные ящики)
# ----------------------------------------------------------------------------

def dresser_praktik():
    b = GlbBuilder("Dresser_Praktik")
    body_mat = wood("Oak_Body", "oak")
    front_mat = Material("Front_White", "#EDE8E1", roughness=0.6)
    inner = wood("Light_Inner", "light")
    handle = metal("Handle_Black", "#1E1E1E", 0.4)
    root = b.add_node("Dresser_Praktik")
    body = b.add_node("Body", parent=root)
    W, H, D, t = 1.20, 0.86, 0.46, 0.018
    leg_h = 0.10
    for sx in (-1, 1):
        for sz in (-1, 1):
            b.add_part(body, cylinder(0.016, 0.022, leg_h, seg=16), body_mat,
                       translate(sx * (W / 2 - 0.06), 0, sz * (D / 2 - 0.06)))
        b.add_part(body, box(t, H - leg_h - t, D, grain_axis=1), body_mat,
                   translate(sx * (W / 2 - t / 2), leg_h + (H - leg_h - t) / 2, 0))
    b.add_part(body, rounded_box(W + 0.02, 0.025, D + 0.015, 0.006, seg=2, grain_axis=0), body_mat,
               translate(0, H - 0.0125, 0.0))
    b.add_part(body, box(W - 2 * t, t, D, grain_axis=0), body_mat, translate(0, leg_h + t / 2, 0))
    b.add_part(body, box(W - 2 * t, H - leg_h - t, 0.006), inner, translate(0, leg_h + (H - leg_h) / 2, -D / 2 + 0.003))
    add_contact_shadow(b, root, 1.22, 0.48)

    n_dr = 4
    inner_h = H - leg_h - t - 0.025
    gap = 0.006
    front_h = (inner_h - gap * (n_dr + 1)) / n_dr
    drawers = []
    for i in range(n_dr):
        y0 = leg_h + t + gap + i * (front_h + gap)
        yc = y0 + front_h / 2
        # разделители между ящиками
        if i > 0:
            b.add_part(body, box(W - 2 * t, 0.012, D - 0.03, grain_axis=0), body_mat,
                       translate(0, y0 - gap / 2, -0.01))
        dnode = b.add_node(f"Drawer{i + 1}", parent=root)
        drawers.append(dnode)
        fw = W - 2 * t - 0.006
        b.add_part(dnode, rounded_box(fw, front_h, 0.02, 0.004, seg=2), front_mat, translate(0, yc, D / 2 - 0.01))
        box_h = front_h - 0.05
        by = y0 + 0.015 + box_h / 2
        bd = D - 0.06
        bz = D / 2 - 0.02 - bd / 2
        b.add_part(dnode, box(fw - 0.04, 0.010, bd, grain_axis=0), inner, translate(0, y0 + 0.015, bz))
        for sx in (-1, 1):
            b.add_part(dnode, box(0.012, box_h, bd, grain_axis=2), inner, translate(sx * (fw / 2 - 0.026), by, bz))
        b.add_part(dnode, box(fw - 0.04, box_h, 0.012, grain_axis=0), inner, translate(0, by, bz - bd / 2 + 0.006))
        b.add_part(dnode, rounded_box(0.32, 0.016, 0.022, 0.007, seg=3), handle, translate(0, yc, D / 2 + 0.011))
    total = 1.9
    # ящики выдвигаются по очереди — сверху вниз

    def drawer_fn(k):
        delay = (n_dr - 1 - k) * 0.22
        return lambda t: np.array([0.0, 0.0, 0.30 * ease_in_out((t - delay) / 0.9)])

    chans = [(d, "translation", drawer_fn(k)) for k, d in enumerate(drawers)]
    make_clip(b, "Открыть ящики", total, chans)
    make_clip(b, "Закрыть ящики", total, [(n, p, reverse_clip_fn(f, 1.6)) for n, p, f in chans])
    return b


MODELS = {
    "sofa_komfort": sofa_komfort,
    "sofa_skandi": sofa_skandi,
    "armchair_loft": armchair_loft,
    "armchair_relax": armchair_relax,
    "table_elegant": table_elegant,
    "chair_nord": chair_nord,
    "wardrobe_classic": wardrobe_classic,
    "dresser_praktik": dresser_praktik,
}
