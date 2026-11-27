# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zerobrand: свойства, загрузочная анимация,
тема по умолчанию и логотип ZerOS в LineageParts.

Модуль подменяет свойства через resetprop и накладывает файлы оверлеем
Magisk: /system/media/bootanimation.zip и
/system/product/overlay/ZerOS*.apk. Тема ZerOS (акцент pink, шрифт Rubik,
форма иконок roundedrect) собрана статическими RRO-оверлеями: они лежат в
/product/overlay, включаются сами при каждой загрузке и из "Стилей" не
выключаются. Обои не трогаются - остаются родные Lineage. Звуки заводские.

Перед сборкой анимации, темы и логотипа:
    powershell -File bootanimation\\make_bootanimation_from_video.ps1
    powershell -File theme\\build_theme.ps1
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
# Статические RRO темы: system/product/overlay/ZerOS{Accent,Font,Shape}.apk
THEME = [os.path.join(ROOT, 'theme', name + '.apk')
         for name in ('ZerOSAccent', 'ZerOSFont', 'ZerOSShape')]


def main():
    checks = [(BOOT, 'bootanimation\\make_bootanimation_from_video.ps1'),
              (LOGO, 'logo\\build_overlay.ps1')]
    checks += [(p, 'theme\\build_theme.ps1') for p in THEME]
    for path, hint in checks:
        if not os.path.exists(path):
            raise SystemExit('нет %s - сначала запусти %s' % (path, hint))

    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(ROOT, 'module.prop'), 'module.prop')
        z.write(os.path.join(ROOT, 'system.prop'), 'system.prop')
        z.write(os.path.join(ROOT, 'uninstall.sh'), 'uninstall.sh')
        z.write(BOOT, 'system/media/bootanimation.zip')
        z.write(LOGO, 'system/product/overlay/ZerOSLogo.apk')
        for path in THEME:
            z.write(path, 'system/product/overlay/' + os.path.basename(path))
    print('OK:', OUT, os.path.getsize(OUT), 'байт')


if __name__ == '__main__':
    main()
