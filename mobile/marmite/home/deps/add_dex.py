# -*- coding: utf-8 -*-
"""Add classes.dex to an APK, rebuilding the zip with clean headers.

aapt2 link produces an APK without a dex; appending with ZipFile('a')
leaves local/central header mismatches that strict package scanners
(and apksigner's own zip parser) complain about.
"""
import sys
import zipfile

src, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]

with zipfile.ZipFile(src) as zi, zipfile.ZipFile(out, 'w') as zo:
    for info in zi.infolist():
        data = zi.read(info.filename)
        ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
        ni.compress_type = info.compress_type
        ni.external_attr = info.external_attr
        ni.internal_attr = info.internal_attr
        ni.create_system = info.create_system
        zo.writestr(ni, data)

    with open(dex, 'rb') as f:
        dex_data = f.read()
    ni = zipfile.ZipInfo('classes.dex', date_time=(1980, 1, 1, 0, 0, 0))
    ni.compress_type = zipfile.ZIP_DEFLATED
    ni.external_attr = 0o644 << 16
    zo.writestr(ni, dex_data)

print('added classes.dex ->', out)
