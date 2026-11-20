# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zerosettings: подменяет системный Settings.apk.

В APK убрана строка «Версия API» и «Версия LineageOS» переименована в
«Версия ZerOS». Модуль обратим: выключить/удалить в Magisk — вернутся
штатные настройки LineageOS.
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, 'dist')
APK = os.path.join(DIST, 'Settings.apk')
OUT = os.path.join(DIST, 'zerosettings-0.0.1.zip')

VERSION = '0.0.1'

# Тот же путь, что у штатных настроек LineageOS.
TARGET = 'system/product/priv-app/Settings/Settings.apk'

MODULE_PROP = """id=zerosettings
name=ZerOS Settings
version=%s
versionCode=1
author=ZeroC
description=Настройки с экраном «Версия ZerOS»: убрана строка «Версия API», «Версия LineageOS» переименована. Подписан платформенным ключом AOSP, поэтому system-разрешения сохранены. Выключите модуль, чтобы вернуть штатные настройки.
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
