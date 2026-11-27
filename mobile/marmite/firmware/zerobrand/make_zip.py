# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zerobrand: свойства, загрузочная анимация,
обои по умолчанию и логотип ZerOS в LineageParts.

Модуль подменяет свойства через resetprop и накладывает файлы оверлеем
Magisk: /system/media/bootanimation.zip и
/system/product/overlay/ZerOSLogo.apk. Обои (wallpaper/wallpaper.jpg)
post-fs-data.sh один раз кладёт в /data/system/users/0/wallpaper.
Звуки не трогаются - они заводские.

Перед сборкой анимации и логотипа:
    powershell -File bootanimation\\make_bootanimation.ps1
    powershell -File logo\\build_overlay.ps1
Обои: wallpaper\\make_wallpapers.py (готовый wallpaper.jpg лежит в git).
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
# Обои по умолчанию (остаются в каталоге модуля, их ставит post-fs-data.sh)
WALL = os.path.join(ROOT, 'wallpaper', 'wallpaper.jpg')
WALL_INFO = os.path.join(ROOT, 'wallpaper', 'wallpaper_info.xml')


def main():
    for path, hint in ((BOOT, 'bootanimation\\make_bootanimation.ps1'),
                       (LOGO, 'logo\\build_overlay.ps1'),
                       (WALL, 'wallpaper\\make_wallpapers.py')):
        if not os.path.exists(path):
            raise SystemExit('нет %s - сначала запусти %s' % (path, hint))

    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(ROOT, 'module.prop'), 'module.prop')
        z.write(os.path.join(ROOT, 'system.prop'), 'system.prop')
        z.write(os.path.join(ROOT, 'post-fs-data.sh'), 'post-fs-data.sh')
        z.write(os.path.join(ROOT, 'uninstall.sh'), 'uninstall.sh')
        z.write(BOOT, 'system/media/bootanimation.zip')
        z.write(LOGO, 'system/product/overlay/ZerOSLogo.apk')
        z.write(WALL, 'wallpaper/wallpaper.jpg')
        z.write(WALL_INFO, 'wallpaper/wallpaper_info.xml')
    print('OK:', OUT, os.path.getsize(OUT), 'байт')


if __name__ == '__main__':
    main()
