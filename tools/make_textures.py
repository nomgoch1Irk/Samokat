#!/usr/bin/env python3
"""Генератор текстур мода (нужен Pillow: pip install pillow).

Запуск из корня проекта:  python3 tools/make_textures.py
Раскладка кубов ОБЯЗАНА совпадать с ScooterModel.java (texOffs + размеры w, h, d).
Можно просто нарисовать свои PNG и положить поверх — скрипт для этого не нужен.
"""
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                    "src", "main", "resources", "assets", "escooter", "textures")

# Цвета самокатов: имя -> основной цвет корпуса
COLORS = {
    "yellow": (250, 204, 21),
    "purple": (147, 51, 234),
}

def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)

def fill(img, mask, x, y, w, h, color, border=None):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if mask[yy][xx]:
                raise SystemExit(f"Пересечение UV-областей в пикселе ({xx},{yy})")
            mask[yy][xx] = True
            edge = xx in (x, x + w - 1) or yy in (y, y + h - 1)
            img.putpixel((xx, yy), border if (border and edge and w > 2 and h > 2) else color)

def paint_box(img, mask, u, v, w, h, d, top, bottom, side, front, back=None, border_k=0.78):
    back = back or front
    def f(x, y, ww, hh, c):
        fill(img, mask, x, y, ww, hh, c, shade(c, border_k))
    f(u + d,         v,     w, d, top)      # верх
    f(u + d + w,     v,     w, d, bottom)   # низ
    f(u,             v + d, d, h, side)     # бок
    f(u + d,         v + d, w, h, front)    # перёд (-z)
    f(u + d + w,     v + d, d, h, side)     # бок
    f(u + 2 * d + w, v + d, w, h, back)     # зад (+z)

def entity_texture(name, color):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    mask = [[False] * 64 for _ in range(64)]

    # Дека окрашена в цвет самоката
    deck_top = shade(color, 0.95)
    deck_side = shade(color, 0.72)
    paint_box(img, mask, 0, 0, 7, 2, 20, deck_top, shade(color, 0.5), deck_side, deck_side)
    # антискользящие полосы на верхней грани деки (верх: x = d..d+w, y = 0..d)
    for yy in range(2, 18, 3):
        for xx in range(21, 26):
            img.putpixel((xx, yy), shade(color, 0.7))
    # тёмная полоса по центру деки
    for yy in range(1, 19):
        img.putpixel((23, yy), (35, 36, 40))

    tire = (28, 28, 32)
    rim = shade(color, 1.0)
    paint_box(img, mask, 0, 24, 2, 5, 5, tire, tire, tire, tire)
    # диск колеса в цвет самоката (боковые грани: x=0..4 и x=7..11, y=29..33)
    for (ox, oy) in ((0, 29), (7, 29)):
        for yy in range(oy + 1, oy + 4):
            for xx in range(ox + 1, ox + 4):
                img.putpixel((xx, yy), rim)

    silver = (190, 196, 206)
    paint_box(img, mask, 16, 24, 2, 17, 2, silver, silver, shade(silver, 0.85), silver)
    # руль: чёрный, ручки цветные
    paint_box(img, mask, 26, 24, 12, 2, 2, (35, 36, 40), (35, 36, 40), (30, 31, 35), (45, 46, 52))
    for xx in list(range(28, 29)) + list(range(36, 37)):
        pass  # ручки получаются из боковых граней ниже
    paint_box(img, mask, 0, 36, 3, 3, 2, (255, 244, 190), (200, 190, 130), (225, 212, 150), (255, 250, 215))
    green = (38, 166, 91)
    paint_box(img, mask, 26, 30, 4, 8, 2, shade(green, 1.1), shade(green, 0.6), shade(green, 0.85), green)
    # молния на батарее (перёд: x = 28..31, y = 32..39)
    for p in ((30, 33), (29, 34), (29, 35), (30, 36), (29, 37)):
        img.putpixel(p, (255, 240, 120))

    os.makedirs(os.path.join(ROOT, "entity"), exist_ok=True)
    img.save(os.path.join(ROOT, "entity", f"electric_scooter_{name}.png"))

def item_scooter(name, color):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    silver, dark, tire = (190, 196, 206), (60, 63, 72), (28, 28, 32)
    light, mid = shade(color, 1.0), shade(color, 0.75)
    # колёса
    for cx in (3, 12):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (-1, 1), (0, 2), (1, 2)):
            img.putpixel((cx + dx, 12 + dy), tire)
        img.putpixel((cx, 13), light)
    # дека в цвет самоката
    for x in range(3, 13):
        img.putpixel((x, 11), mid)
        img.putpixel((x, 10), light)
    for x in range(4, 12):
        if x % 2 == 0:
            img.putpixel((x, 10), dark)
    # стойка и руль (спереди = справа)
    for y in range(2, 11):
        img.putpixel((12, y), silver)
    for x in range(10, 15):
        img.putpixel((x, 2), (35, 36, 40))
    img.putpixel((10, 2), light)
    img.putpixel((14, 2), light)
    img.putpixel((11, 3), (255, 244, 190))
    img.save(os.path.join(ROOT, "item", f"{name}_electric_scooter.png"))

def item_battery():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body, dark, light, cap = (38, 166, 91), (22, 100, 55), (90, 220, 140), (180, 186, 196)
    for y in range(4, 15):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (4, 14)
            img.putpixel((x, y), dark if edge else body)
    for y in range(5, 14):
        img.putpixel((5, y), light)
    for x in range(6, 10):
        for y in (2, 3):
            img.putpixel((x, y), cap)
    for p in ((8, 6), (7, 7), (7, 8), (8, 9), (7, 10), (8, 8)):
        img.putpixel(p, (255, 240, 120))
    img.save(os.path.join(ROOT, "item", "battery.png"))

if __name__ == "__main__":
    os.makedirs(os.path.join(ROOT, "item"), exist_ok=True)
    for cname, ccolor in COLORS.items():
        entity_texture(cname, ccolor)
        item_scooter(cname, ccolor)
    item_battery()
    print("Текстуры созданы в", os.path.normpath(ROOT))
