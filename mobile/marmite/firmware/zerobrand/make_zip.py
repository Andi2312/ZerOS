# -*- coding: utf-8 -*-
"""Собрать Magisk-модуль zerobrand: свойства, загрузочная анимация,
звуки интерфейса, звуки уведомления/рингтона/будильника, логотип ZerOS в
LineageParts и помощник для звука отключения зарядки.

Модуль подменяет свойства через resetprop и накладывает файлы оверлеем
Magisk: /system/media/bootanimation.zip, /system/product/media/audio/ui/*.ogg,
/system/product/media/audio/{notifications,ringtones,alarms}/*.ogg,
/system/product/overlay/ZerOSLogo.apk и
/system/product/app/ZerOSCharge/ZerOSCharge.apk.

Перед сборкой анимации, логотипа и приложения:
    powershell -File bootanimation\\make_bootanimation.ps1
    powershell -File logo\\build_overlay.ps1
    powershell -File charge\\build_charge.ps1

Звуки лежат готовыми в sounds\\ui, sounds\\notifications, sounds\\ringtones
и sounds\\alarms (генератор — sounds\\make_sounds.py).
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
# Звуки уведомлений: system/product/media/audio/notifications/*.ogg
NOTIFS = os.path.join(ROOT, 'sounds', 'notifications')
# Рингтоны и будильники
RINGTONES = os.path.join(ROOT, 'sounds', 'ringtones')
ALARMS = os.path.join(ROOT, 'sounds', 'alarms')
# RRO-оверлей логотипа: system/product/overlay/ZerOSLogo.apk
LOGO = os.path.join(ROOT, 'logo', 'ZerOSLogo.apk')
# Приложение звука отключения зарядки: system/product/app/ZerOSCharge
CHARGE = os.path.join(ROOT, 'charge', 'ZerOSCharge.apk')


def main():
    for path, hint in ((BOOT, 'bootanimation\\make_bootanimation.ps1'),
                       (LOGO, 'logo\\build_overlay.ps1'),
                       (CHARGE, 'charge\\build_charge.ps1')):
        if not os.path.exists(path):
            raise SystemExit('нет %s - сначала запусти %s' % (path, hint))
    if not os.path.isdir(SOUNDS):
        raise SystemExit('нет %s' % SOUNDS)
    for folder in (NOTIFS, RINGTONES, ALARMS):
        if not os.path.isdir(folder):
            raise SystemExit('нет %s - сначала запусти sounds\\make_sounds.py' % folder)

    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(ROOT, 'module.prop'), 'module.prop')
        z.write(os.path.join(ROOT, 'system.prop'), 'system.prop')
        z.write(os.path.join(ROOT, 'service.sh'), 'service.sh')
        z.write(os.path.join(ROOT, 'uninstall.sh'), 'uninstall.sh')
        z.write(BOOT, 'system/media/bootanimation.zip')
        for name in sorted(os.listdir(SOUNDS)):
            if name.lower().endswith('.ogg'):
                z.write(os.path.join(SOUNDS, name),
                        'system/product/media/audio/ui/' + name)
        for name in sorted(os.listdir(NOTIFS)):
            if name.lower().endswith('.ogg'):
                z.write(os.path.join(NOTIFS, name),
                        'system/product/media/audio/notifications/' + name)
        for folder, target in ((RINGTONES, 'ringtones'), (ALARMS, 'alarms')):
            for name in sorted(os.listdir(folder)):
                if name.lower().endswith('.ogg'):
                    z.write(os.path.join(folder, name),
                            'system/product/media/audio/%s/' % target + name)
        z.write(LOGO, 'system/product/overlay/ZerOSLogo.apk')
        z.write(CHARGE, 'system/product/app/ZerOSCharge/ZerOSCharge.apk')
    print('OK:', OUT, os.path.getsize(OUT), 'байт')


if __name__ == '__main__':
    main()

