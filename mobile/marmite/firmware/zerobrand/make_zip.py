# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zerobrand: брендинг ZerOS в build.prop.

Модуль только переопределяет свойства при загрузке (resetprop), файлы
/system и /vendor не меняются. Выключение модуля возвращает LineageOS-строки.
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, 'dist')
VERSION = '0.0.1'
OUT = os.path.join(DIST, 'zerobrand-%s.zip' % VERSION)


def main():
    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(ROOT, 'module.prop'), 'module.prop')
        z.write(os.path.join(ROOT, 'system.prop'), 'system.prop')
    print('OK:', OUT, os.path.getsize(OUT), 'байт')


if __name__ == '__main__':
    main()
