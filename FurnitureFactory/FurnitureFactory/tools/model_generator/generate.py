"""
Генерирует демонстрационные GLB-модели и миниатюры для приложения.

Запуск (из корня проекта):
    python3 tools/model_generator/generate.py

Результат:
    app/src/main/assets/models/<id>.glb      — 3D-модели с анимациями
    app/src/main/assets/images/<id>.png      — миниатюры для карточек (прозрачный фон)
    app/src/main/assets/images/banner_interior.png — баннер профиля

Параметр --frames <папка> дополнительно сохраняет раскадровку анимаций для проверки.
"""
from __future__ import annotations

import argparse
import json
import os
import struct
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from models import MODELS  # noqa: E402
from render_preview import on_background, render  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")


def inspect_glb(path):
    with open(path, "rb") as f:
        data = f.read()
    magic, ver, total = struct.unpack_from("<III", data, 0)
    assert magic == 0x46546C67 and ver == 2 and total == len(data), "bad GLB header"
    jlen, jtype = struct.unpack_from("<II", data, 12)
    assert jtype == 0x4E4F534A
    js = json.loads(data[20:20 + jlen].decode("utf-8"))
    blen, btype = struct.unpack_from("<II", data, 20 + jlen)
    assert btype == 0x004E4942 and blen == js["buffers"][0]["byteLength"]
    for bv in js["bufferViews"]:
        assert bv["byteOffset"] + bv["byteLength"] <= blen
        assert bv["byteOffset"] % 4 == 0
    sizes = {5126: 4, 5123: 2, 5125: 4}
    comps = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}
    for acc in js["accessors"]:
        bv = js["bufferViews"][acc["bufferView"]]
        need = acc["count"] * comps[acc["type"]] * sizes[acc["componentType"]]
        assert need == bv["byteLength"], (need, bv["byteLength"])
    for m in js.get("meshes", []):
        for p in m["primitives"]:
            n = js["accessors"][p["attributes"]["POSITION"]]["count"]
            idx = js["accessors"][p["indices"]]
            off = js["bufferViews"][idx["bufferView"]]["byteOffset"]
            dt = np.uint16 if idx["componentType"] == 5123 else np.uint32
            arr = np.frombuffer(data, dtype=dt, count=idx["count"], offset=28 + jlen + off)
            assert arr.max() < n, "index out of range"
    anims = []
    for a in js.get("animations", []):
        dur = 0.0
        for s in a["samplers"]:
            ia, oa = js["accessors"][s["input"]], js["accessors"][s["output"]]
            assert ia["count"] == oa["count"]
            dur = max(dur, ia["max"][0])
        anims.append((a["name"], round(dur, 2), len(a["channels"])))
    tris = sum(js["accessors"][p["indices"]]["count"] // 3 for m in js.get("meshes", []) for p in m["primitives"])
    return {"bytes": len(data), "triangles": tris, "animations": anims, "materials": len(js.get("materials", [])),
            "images": len(js.get("images", []))}


def make_banner(path):
    """Баннер «Вдохновение для вашего дома»: интерьерная композиция справа, место под текст слева."""
    sofa = MODELS["sofa_skandi"]()
    chair = MODELS["armchair_loft"]()
    img_sofa = render(sofa, yaw=-18, pitch=8, size=(560, 320), ss=2)
    img_chair = render(chair, yaw=-50, pitch=8, size=(300, 300), ss=2)
    W, H, floor_y = 900, 330, 262
    bg = Image.new("RGB", (W, H), "#E8DFD3")
    light = Image.new("L", (W, H), 0)
    ImageDraw.Draw(light).polygon([(560, 0), (900, 0), (900, 200), (700, 330), (420, 330)], fill=110)
    light = light.filter(ImageFilter.GaussianBlur(45))
    bg = Image.composite(Image.new("RGB", (W, H), "#F8F3EC"), bg, light)
    d = ImageDraw.Draw(bg)
    d.rectangle([0, floor_y, W, H], fill="#D8CCBC")
    d.rectangle([0, floor_y, W, floor_y + 3], fill="#CDBFAE")
    # картина на стене
    d.rectangle([505, 34, 595, 106], fill="#F4EEE6", outline="#B9A894", width=4)
    d.ellipse([525, 50, 575, 90], fill="#D7B99A")
    # растение в кашпо
    d.rounded_rectangle([826, 200, 876, floor_y + 2], radius=8, fill="#CBBFAF")
    leaves = [(836, 168, 24, "#7C8A6A"), (866, 150, 22, "#6E7C5E"), (850, 120, 20, "#869373"),
              (878, 182, 17, "#738164"), (824, 140, 15, "#8A9878")]
    for cx, cy, r, c in leaves:
        d.ellipse([cx - r, cy - r * 1.4, cx + r, cy + r * 1.4], fill=c)
    d.line([(851, 200), (849, 128)], fill="#5E6B50", width=3)
    canvas = bg.convert("RGBA")

    def paste_on_floor(img, x, scale=1.0, sink=4):
        if scale != 1.0:
            img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)
        bbox = img.getchannel("A").point(lambda a: 255 if a > 200 else 0).getbbox()
        canvas.alpha_composite(img, (x, floor_y + sink - bbox[3]))

    paste_on_floor(img_sofa, 300, scale=0.88)
    paste_on_floor(img_chair, 670, scale=0.56, sink=8)
    canvas.convert("RGB").save(path, quality=92)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--frames", default=None, help="папка для раскадровки анимаций")
    ap.add_argument("--only", default=None, help="id модели")
    args = ap.parse_args()
    os.makedirs(os.path.join(ASSETS, "models"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "images"), exist_ok=True)
    report = {}
    for mid, fn in MODELS.items():
        if args.only and mid != args.only:
            continue
        b = fn()
        out = os.path.join(ASSETS, "models", f"{mid}.glb")
        b.write(out)
        info = inspect_glb(out)
        report[mid] = info
        thumb = render(b, size=(800, 600), ss=2)
        thumb.save(os.path.join(ASSETS, "images", f"{mid}.png"), optimize=True)
        print(f"{mid}: {info['bytes'] / 1024:.0f} KB, {info['triangles']} tris, anims={info['animations']}")
        if args.frames and b.clips:
            os.makedirs(args.frames, exist_ok=True)
            for ci, clip in enumerate(b.clips):
                dur = clip.duration
                times = np.linspace(0, dur, 6)
                # общие границы по всему клипу, чтобы кадр не «прыгал»
                from render_preview import collect
                bounds = []
                for tt in times:
                    bounds += [(m, mat) for m, mat in collect(b, b.pose_at(clip, tt)) if not mat.blend]
                frames = [on_background(render(b, clip, tt, size=(360, 270), ss=1, bounds_items=bounds))
                          for tt in times]
                sheet = Image.new("RGB", (360 * 3, 270 * 2), "white")
                for k, fr in enumerate(frames):
                    sheet.paste(fr, ((k % 3) * 360, (k // 3) * 270))
                sheet.save(os.path.join(args.frames, f"{mid}_clip{ci}.png"))
    if not args.only:
        make_banner(os.path.join(ASSETS, "images", "banner_interior.jpg"))
    with open(os.path.join(ROOT, "tools", "model_generator", "last_report.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
