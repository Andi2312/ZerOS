# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zerobrand: свойства, загрузочная анимация,
звуки интерфейса и логотип ZerOS в LineageParts.

Модуль подменяет свойства через resetprop и накладывает файлы оверлеем
Magisk: /system/media/bootanimation.zip, /system/product/media/audio/ui/*.ogg
и /system/product/overlay/ZerOSLogo.apk.

Перед сборкой анимации и логотипа:
    powershell -File bootanimation\\make_bootanimation.ps1
    powershell -File logo\\build_overlay.ps1

Звуки лежат готовыми в sounds\\ui (генератор — sounds\\make_sounds.py).
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, 'dist')
VERSION = '1.0'
OUT = os.path.join(DIST, 'zerobrand-%s.zip' % VERSION)

# Загрузочная анимация: system/media/bootanimation.zip
BOOT = os.path.join(ROOT, 'bootanimation.zip')
# Звуки интерфейса: system/product/media/audio/ui/*.ogg
SOUNDS = os.path.join(ROOT, 'sounds', 'ui')
# RRO-оверлей логотипа: system/product/overlay/ZerOSLogo.apk
LOGO = os.path.join(ROOT, 'logo', 'ZerOSLogo.apk')


def main():
    if not os.path.exists(BOOT):
        raise SystemExit('нет %s - сначала запусти bootanimation\\make_bootanimation.ps1' % BOOT)
    if not os.path.exists(LOGO):
        raise SystemExit('нет %s - сначала запусти logo\\build_overlay.ps1' % LOGO)
    if not os.path.isdir(SOUNDS):
        raise SystemExit('нет %s' % SOUNDS)

    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(ROOT, 'module.prop'), 'module.prop')
        z.write(os.path.join(ROOT, 'system.prop'), 'system.prop')
        z.write(BOOT, 'system/media/bootanimation.zip')
        for name in sorted(os.listdir(SOUNDS)):
            if name.lower().endswith('.ogg'):
                z.write(os.path.join(SOUNDS, name),
                        'system/product/media/audio/ui/' + name)
        z.write(LOGO, 'system/product/overlay/ZerOSLogo.apk')
    print('OK:', OUT, os.path.getsize(OUT), 'байт')


if __name__ == '__main__':
    main()
