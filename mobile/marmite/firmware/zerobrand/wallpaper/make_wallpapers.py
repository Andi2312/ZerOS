# -*- coding: utf-8 -*-
"""Обои ZerOS: три варианта 720x1280 — ровно экран marmite. Такой файл
система рисует 1:1 (как заводские обои), без растяжения и обрезки.

Запуск: python make_wallpapers.py
Результат: out/*.jpg и out/preview.jpg (все варианты рядом для выбора).
"""
import os

import numpy as np
from PIL import Image

W, H = 720, 1280
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out')

ACCENT = np.array([0x4A, 0x0A, 0x18], dtype=np.float32)
ACCENT_DIM = np.array([0x2E, 0x06, 0x0F], dtype=np.float32)
PRESSED = np.array([0x5C, 0x0D, 0x1E], dtype=np.float32)
ON_ACCENT = np.array([0xF2, 0xA6, 0xA0], dtype=np.float32)
BG = np.array([0x06, 0x04, 0x05], dtype=np.float32)

YY, XX = np.mgrid[0:H, 0:W].astype(np.float32)


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def mix(a, b, t):
    return a + (b - a) * t[..., None]


def radial(cx, cy, radius):
    d = np.sqrt((XX - cx) ** 2 + (YY - cy) ** 2)
    return np.exp(-(d / radius) ** 2)


def vignette(img, strength=0.45):
    nx = (XX - W / 2) / (W / 2)
    ny = (YY - H / 2) / (H / 2)
    r2 = (nx ** 2 + ny ** 2) / 2.0
    return img * (1.0 - strength * (r2 ** 1.4))[..., None]


def grain(img, sigma=0.9, seed=0):
    rs = np.random.RandomState(seed)
    return (img + rs.normal(0.0, sigma, (H, W, 1))
            + rs.normal(0.0, 0.4, (H, W, 1))).astype(np.float32)


def line(img, y, sigma=1.6, alpha=0.5, half=310.0, color=ON_ACCENT):
    a = np.exp(-((YY - y) / sigma) ** 2) * alpha
    a *= smoothstep(half, half * 0.30, np.abs(XX - W / 2))
    return img + (color - img) * a[..., None]


def to_image(img):
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGB')


def horizon():
    t = smoothstep(0.22, 1.0, YY / (H - 1))
    img = mix(BG, ACCENT * 0.78, t)
    img = img + ACCENT * (radial(W / 2, H + 120, 520) * 0.45)[..., None]
    img = img + PRESSED * (radial(W / 2, H + 20, 300) * 0.40)[..., None]
    img = line(img, 1002, 1.6, 0.72, 340.0)
    img = img + ACCENT_DIM * (smoothstep(1008, H, YY) * 0.35)[..., None]
    img = vignette(img, 0.40)
    return to_image(grain(img, 0.8, 11))


def orbit():
    img = mix(BG, ACCENT * 0.30, smoothstep(0.65, 0.02, YY / (H - 1)))
    cx, cy = W / 2, H * 0.47
    d = np.sqrt((XX - cx) ** 2 + (YY - cy) ** 2)
    img = img + ACCENT * (radial(cx, cy, 380) * 0.50)[..., None]
    for r, color, alpha, wdt in ((112, ACCENT, 0.34, 1.1),
                                 (216, ON_ACCENT, 0.44, 0.9),
                                 (322, ACCENT, 0.20, 1.5)):
        ring = np.exp(-((d - r) / wdt) ** 2) * alpha
        img = img + (color - img) * ring[..., None]
    ang = np.deg2rad(212.0)
    px, py = cx + 216 * np.cos(ang), cy + 216 * np.sin(ang)
    dot = np.exp(-(((XX - px) ** 2 + (YY - py) ** 2) / (4.2 ** 2)))
    img = img + (ON_ACCENT - img) * (dot * 0.9)[..., None]
    img = img + ACCENT_DIM * (radial(cx, cy, 100) * 0.55)[..., None]
    img = vignette(img, 0.38)
    return to_image(grain(img, 0.8, 22))


def eclipse():
    img = mix(BG, ACCENT * 0.35, smoothstep(0.85, 0.0, YY / (H - 1)))
    cx, cy, r0 = W / 2, H * 0.455, 168.0
    d = np.sqrt((XX - cx) ** 2 + (YY - cy) ** 2)
    img = img + ACCENT * (radial(cx, cy, 460) * 0.34)[..., None]
    img = img + ON_ACCENT * (np.exp(-((d - r0) / 78.0) ** 2) * 0.07)[..., None]
    disc = smoothstep(r0 + 1.0, r0 - 1.0, d)
    img = img * (1.0 - disc * 0.72)[..., None]
    img = img + ACCENT_DIM * (disc * 0.06)[..., None]
    rim = np.exp(-((d - r0) / 0.9) ** 2) * 0.60
    img = img + (ON_ACCENT - img) * rim[..., None]
    img = img + PRESSED * (np.exp(-((d - r0) / 16.0) ** 2) * 0.20)[..., None]
    img = img + ACCENT * (smoothstep(r0, H * 0.95, YY) * 0.16)[..., None]
    img = vignette(img, 0.40)
    return to_image(grain(img, 0.8, 44))


def main():
    os.makedirs(OUT, exist_ok=True)
    imgs = [('1-horizon.jpg', horizon()), ('2-orbit.jpg', orbit()),
            ('3-eclipse.jpg', eclipse())]
    for name, im in imgs:
        im.save(os.path.join(OUT, name), 'JPEG', quality=96,
                subsampling=0, optimize=True)
        print(name, im.size)

    gap = 20
    tw, th = 288, 512
    sheet = Image.new('RGB', (len(imgs) * tw + gap * (len(imgs) - 1), th),
                      (10, 8, 9))
    for i, (_, im) in enumerate(imgs):
        sheet.paste(im.resize((tw, th), Image.LANCZOS), (i * (tw + gap), 0))
    sheet.save(os.path.join(OUT, 'preview.jpg'), 'JPEG', quality=92)
    print('preview.jpg', sheet.size)


if __name__ == '__main__':
    main()
