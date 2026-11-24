# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль HomeWatch: подменяет системный лаунчер на нашу сборку AOSP Launcher3.

Модуль обратим: выключить/удалить модуль в Magisk — вернётся штатный лаунчер.
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, 'dist')

# Сборки в порядке предпочтения. Первая — Launcher3QuickStep из Android 10:
# только она совместима с рекастами на Android 10 (LOS 17.1).
CANDIDATES = [
    'Launcher3QuickStep-0.1.0.apk',
    'Launcher3QuickStep-r47.apk',
    'Launcher3QuickStep.apk',
    'Launcher3.apk',
]
APK = next((os.path.join(DIST, n) for n in CANDIDATES if os.path.exists(os.path.join(DIST, n))), None)
OUT = os.path.join(DIST, 'HomeWatch-magisk.zip')

# Версия модуля берётся из имени APK, чтобы Magisk видел обновление.
VERSIONS = {
    'Launcher3QuickStep-0.1.0.apk': '0.1.0-r47',
    'Launcher3QuickStep-r47.apk': '10.0.0-r47',
}
VERSION = VERSIONS.get(os.path.basename(APK or ''), '9.0.0-r61')
MODULE_VERSION_CODE = '2'

# Куда Magisk положит наш APK (тот же путь, что у штатного лаунчера LineageOS).
TARGET = 'system/product/priv-app/TrebuchetQuickStep/TrebuchetQuickStep.apk'

MODULE_PROP = """id=homewatch
name=HomeWatch (AOSP Launcher3 QuickStep)
version=%s
versionCode=%s
author=ZeroC
description=Штатный AOSP Launcher3, флейвор Launcher3QuickStep, как домашний экран HomeWatch: рабочий стол, рекасты и жесты. Подменяет системный com.android.launcher3 (TrebuchetQuickStep). Выключите модуль, чтобы вернуть штатный лаунчер.
""" % (VERSION, MODULE_VERSION_CODE)


def main():
    if not APK:
        raise SystemExit('нет APK в %s — сначала соберите build-l3q.ps1' % DIST)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('module.prop', MODULE_PROP)
        z.write(APK, TARGET)
    print('OK:', OUT, os.path.getsize(OUT), 'байт, apk:', os.path.basename(APK), 'version:', VERSION)


if __name__ == '__main__':
    main()
