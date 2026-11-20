# -*- coding: utf-8 -*-
"""Правка строк декодированного Updater.apk: «LineageOS» → «ZerOS».

Меняются только подписи в интерфейсе: заголовок экрана («LineageOS\\n1.0»),
строки списка сборок. Адреса сервера, пути к папкам загрузок и ссылки на wiki
остаются прежними — иначе обновлятор перестанет работать.

Использование: python patch_res.py C:\\work\\updater_apk\\dec
"""
import glob
import io
import os
import re
import sys

# Строки, в которых «LineageOS» — это название системы в интерфейсе.
LABELS = (
    'header_title_text',          # «LineageOS\n1.0» в шапке
    'list_build_version',         # «LineageOS 1.0» в списке сборок
    'list_build_version_date',    # «LineageOS 1.0 - <дата>»
)


def patch_file(path):
    with io.open(path, encoding='utf-8') as f:
        text = f.read()
    changed = False
    for name in LABELS:
        def repl(m):
            value = m.group(2)
            if 'LineageOS' not in value:
                return m.group(0)
            return m.group(1) + value.replace('LineageOS', 'ZerOS') + m.group(3)

        text, n = re.subn(
            r'(<string name="%s">)(.*?)(</string>)' % name, repl, text, flags=re.S)
        changed = changed or n > 0
    if changed:
        with io.open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)
    return changed


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else '.'
    files = sorted(glob.glob(os.path.join(root, 'res', 'values*', 'strings.xml')))
    touched = [p for p in files if patch_file(p)]
    if not touched:
        raise SystemExit('не найдено ни одной строки с «LineageOS»')
    print('patched %d files' % len(touched))


if __name__ == '__main__':
    main()
