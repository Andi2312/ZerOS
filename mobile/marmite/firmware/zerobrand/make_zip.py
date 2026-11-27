# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zerobrand: свойства, загрузочная анимация и
логотип ZerOS в LineageParts.

Модуль подменяет свойства через resetprop и накладывает файлы оверлеем
Magisk: /system/media/bootanimation.zip и
/system/product/overlay/ZerOSLogo.apk. Звуки не трогаются - они заводские.

Перед сборкой анимации и логотипа:
    powershell -File bootanimation\\make_bootanimation.ps1
    powershell -File logo\\build_overlay.ps1
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, 'dist')
VERSION = '1.0'
OUT = os.path.join(DIST, 'zerobrand-%s.zip' % VERSION)

# Загрузочная анимация: system/media/bootanimation.zip
BOOT = os.path.join(ROOT, 'bootanimation.zip')
# RRO-оверлей логотипа: system/product/overlay/ZerOSLogo.apk
LOGO = os.path.join(ROOT, 'logo', 'ZerOSLogo.apk')


def main():
    for path, hint in ((BOOT, 'bootanimation\\make_bootanimation.ps1'),
                       (LOGO, 'logo\\build_overlay.ps1')):
        if not os.path.exists(path):
            raise SystemExit('нет %s - сначала запусти %s' % (path, hint))

    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(ROOT, 'module.prop'), 'module.prop')
        z.write(os.path.join(ROOT, 'system.prop'), 'system.prop')
        z.write(BOOT, 'system/media/bootanimation.zip')
        z.write(LOGO, 'system/product/overlay/ZerOSLogo.apk')
    print('OK:', OUT, os.path.getsize(OUT), 'байт')


if __name__ == '__main__':
    main()
