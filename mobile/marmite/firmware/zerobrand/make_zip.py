# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zerobrand: брендинг ZerOS + загрузочная анимация.

Модуль только подменяет свойства через resetprop, файлы /system и /vendor
не трогает. bootanimation.zip кладётся в system/media и подменяется
оверлеем Magisk.

Перед сборкой анимации:
    powershell -File bootanimation\\make_bootanimation.ps1
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, 'dist')
VERSION = '1.0'
OUT = os.path.join(DIST, 'zerobrand-%s.zip' % VERSION)

# Загрузочная анимация: system/media/bootanimation.zip
BOOT = os.path.join(ROOT, 'bootanimation.zip')


def main():
    if not os.path.exists(BOOT):
        raise SystemExit('нет %s - сначала запусти bootanimation\\make_bootanimation.ps1' % BOOT)
    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(ROOT, 'module.prop'), 'module.prop')
        z.write(os.path.join(ROOT, 'system.prop'), 'system.prop')
        z.write(BOOT, 'system/media/bootanimation.zip')
    print('OK:', OUT, os.path.getsize(OUT), 'байт')


if __name__ == '__main__':
    main()
