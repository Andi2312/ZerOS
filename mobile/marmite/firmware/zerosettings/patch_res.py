# -*- coding: utf-8 -*-
"""Правка ресурсов декодированного Settings.apk: экран «Версия прошивки».

Убирает пункт «Версия API» (LineageApiVersionPreferenceController) и
переименовывает «Версия LineageOS» в «Версия ZerOS» (ключ и контроллер
остаются штатными, поэтому значение по-прежнему берётся из
ro.lineage.version).

Использование: python patch_res.py C:\\work\\settings_apk\\dec
"""
import io
import os
import re
import sys

XML = 'res/xml/firmware_version.xml'
STRINGS = [
    ('res/values/strings.xml', 'zer_version', 'ZerOS version'),
    ('res/values-ru/strings.xml', 'zer_version', 'Версия ZerOS'),
]


def patch_xml(root):
    path = os.path.join(root, XML)
    with io.open(path, encoding='utf-8') as f:
        text = f.read()

    if 'android:key="lineage_api_version"' not in text and '@string/zer_version' in text:
        print('skip', XML, '(already patched)')
        return

    # Выкидываем весь <Preference ... /> с ключом lineage_api_version.
    text, n = re.subn(
        r'\n[ \t]*<Preference[^>]*android:key="lineage_api_version"[^>]*/>', '', text)
    if n != 1:
        raise SystemExit('%s: ожидалась одна строка lineage_api_version, найдено %d' % (XML, n))

    text, n = re.subn(
        r'<Preference android:title="@lineageos\.platform:string/lineage_version"',
        '<Preference android:title="@string/zer_version"', text)
    if n != 1:
        raise SystemExit('%s: не найден заголовок lineage_version' % XML)

    with io.open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    print('patched', XML)


def patch_strings(root, rel, name, value):
    path = os.path.join(root, rel)
    with io.open(path, encoding='utf-8') as f:
        text = f.read()
    if 'name="%s"' % name in text:
        print('skip', rel, '(already has', name + ')')
        return
    line = '    <string name="%s">%s</string>\n' % (name, value)
    text = text.replace('</resources>', line + '</resources>')
    with io.open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    print('patched', rel)


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else '.'
    patch_xml(root)
    for rel, name, value in STRINGS:
        patch_strings(root, rel, name, value)


if __name__ == '__main__':
    main()
