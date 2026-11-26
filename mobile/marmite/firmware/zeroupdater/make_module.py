# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zeroupdater: подменяет системный Updater.apk.

В обновляторе шапка показывает «ZerOS 1.0» вместо «LineageOS 1.0». Модуль
обратим: выключить/удалить в Magisk — вернётся штатный обновлятор.
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, 'dist')
APK = os.path.join(DIST, 'Updater.apk')
OUT = os.path.join(DIST, 'zeroupdater-1.0.zip')

VERSION = '1.0'

# Тот же путь, что у штатного обновлятора LineageOS.
TARGET = 'system/priv-app/Updater/Updater.apk'

MODULE_PROP = """id=zeroupdater
name=ZerOS Updater
version=%s
versionCode=1
author=ZeroC
description=Обновлятор с названием ZerOS: в шапке «ZerOS 1.0» вместо «LineageOS 1.0». Адреса сервера и пути загрузок не тронуты, подпись — AOSP testkey, как у штатного. Выключите модуль, чтобы вернуть штатный обновлятор.
""" % VERSION


def main():
    if not os.path.exists(APK):
        raise SystemExit('нет APK: %s — сначала соберите build.ps1' % APK)
    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('module.prop', MODULE_PROP)
        z.write(APK, TARGET)
    print('OK:', OUT, os.path.getsize(OUT), 'байт, apk:', os.path.getsize(APK))


if __name__ == '__main__':
    main()
