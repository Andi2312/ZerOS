<#
make_bootanimation.ps1 — собирает bootanimation.zip для ZerOS.

Рисует на чёрном фоне надпись «ZerOS I» акцентным цветом палитры
(#F2A6A0) с чертой: сначала плавное появление (part0, играет один раз),
потом лёгкая пульсация (part1, крутится, пока идёт загрузка).

Разрешение задаётся под устройство (marmite — Wileyfox Swift 2, 720x1280).

Упаковка — через python: zipfile.ZIP_STORED. PowerShell 5.1 (.NET
Framework) с CompressionLevel.NoCompression всё равно жмёт кадры deflate,
а Android bootanimation читает только stored (method 0) — иначе чёрный
экран вместо анимации.

Запуск:
    powershell -NoProfile -ExecutionPolicy Bypass -File .\make_bootanimation.ps1
    powershell -NoProfile -ExecutionPolicy Bypass -File .\make_bootanimation.ps1 -Width 1080 -Height 2160
#>

param(
    [int]$Width = 720,
    [int]$Height = 1280,
    [int]$Fps = 30,
    [int]$FadeFrames = 24,
    [int]$PulseFrames = 90,
    [int]$PulseMin = 205,
    [string]$Text = 'ZerOS I',
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Out) { $Out = Join-Path (Split-Path -Parent $Root) 'bootanimation.zip' }

$Tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("zeros-boot-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
$Part0 = Join-Path $Tmp 'part0'
$Part1 = Join-Path $Tmp 'part1'
New-Item -ItemType Directory -Force -Path $Part0, $Part1 | Out-Null

$Accent = [System.Drawing.Color]::FromArgb(255, 242, 166, 160)   # zer-on-accent #F2A6A0
$Bg = [System.Drawing.Color]::Black

$bmp = New-Object System.Drawing.Bitmap($Width, $Height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$family = $null
foreach ($name in @('Segoe UI Semibold', 'Segoe UI', 'Arial')) {
    try { $family = New-Object System.Drawing.FontFamily($name); break } catch { }
}
if (-not $family) { $family = [System.Drawing.FontFamily]::GenericSansSerif }

$size = [Math]::Round($Width * 0.155)
$font = New-Object System.Drawing.Font($family, $size, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$fmt = New-Object System.Drawing.StringFormat
$fmt.Alignment = [System.Drawing.StringAlignment]::Center
$fmt.LineAlignment = [System.Drawing.StringAlignment]::Center

$textSize = $g.MeasureString($Text, $font)
$cx = $Width / 2.0
$cy = $Height / 2.0
$barW = [Math]::Round($textSize.Width * 0.9)
$barY = [Math]::Round($cy + $textSize.Height * 0.52)
$barH = [Math]::Max(2, [Math]::Round($Height * 0.004))

function Save-Frame([int]$alpha, [string]$file) {
    $g.Clear($Bg)

    $textColor = [System.Drawing.Color]::FromArgb($alpha, $Accent)
    $rect = New-Object System.Drawing.RectangleF(0, 0, $Width, $Height)
    $brush = New-Object System.Drawing.SolidBrush($textColor)
    $g.DrawString($Text, $font, $brush, $rect, $fmt)
    $brush.Dispose()

    $barAlpha = [int][Math]::Round($alpha * 0.7)
    $w = [Math]::Max(1, [int][Math]::Round($barW * ($alpha / 255.0)))
    $barColor = [System.Drawing.Color]::FromArgb($barAlpha, $Accent)
    $barBrush = New-Object System.Drawing.SolidBrush($barColor)
    $g.FillRectangle($barBrush, [single]($cx - $w / 2.0), [single]$barY, [single]$w, [single]$barH)
    $barBrush.Dispose()

    $bmp.Save($file, [System.Drawing.Imaging.ImageFormat]::Png)
}

Write-Host (("кадров: {0} появление + {1} пульсация ({2}x{3}, {4} fps)") -f $FadeFrames, $PulseFrames, $Width, $Height, $Fps)

# part0 — появление, играет один раз
for ($i = 0; $i -lt $FadeFrames; $i++) {
    $t = ($i + 1) / [double]$FadeFrames
    $alpha = [int][Math]::Round(255 * [Math]::Pow($t, 1.6))
    Save-Frame $alpha (Join-Path $Part0 ("frame_{0:d3}.png" -f $i))
}

# part1 — пульсация, крутится до конца загрузки; первый и последний кадр
# совпадают по яркости, поэтому стык незаметен
for ($i = 0; $i -lt $PulseFrames; $i++) {
    $phase = 2.0 * [Math]::PI * $i / $PulseFrames
    $alpha = [int][Math]::Round($PulseMin + (255 - $PulseMin) * (0.5 + 0.5 * [Math]::Cos($phase)))
    Save-Frame $alpha (Join-Path $Part1 ("frame_{0:d3}.png" -f $i))
}

$g.Dispose()
$bmp.Dispose()
$font.Dispose()
$family.Dispose()

# desc.txt: размер экрана, fps, части. part0 — раз, part1 — бесконечно.
$desc = Join-Path $Tmp 'desc.txt'
$descText = "$Width $Height $Fps`np 1 0 part0`np 0 0 part1`n"
[System.IO.File]::WriteAllText($desc, $descText, (New-Object System.Text.UTF8Encoding($false)))

# ZIP_STORED: Android bootanimation не читает deflate-кадры.
$pack = Join-Path $Tmp 'pack.py'
$packPy = @"
import os
import sys
import zipfile

src, out = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(out, 'w', zipfile.ZIP_STORED) as z:
    z.write(os.path.join(src, 'desc.txt'), 'desc.txt')
    for part in ('part0', 'part1'):
        d = os.path.join(src, part)
        # bootanimation ищет в zip сам каталог partN/, а не только файлы
        info = zipfile.ZipInfo(part + '/')
        info.external_attr = 0o40755 << 16
        z.writestr(info, b'')
        for name in sorted(os.listdir(d)):
            z.write(os.path.join(d, name), part + '/' + name)
"@
[System.IO.File]::WriteAllText($pack, $packPy, (New-Object System.Text.UTF8Encoding($false)))

if (Test-Path $Out) { Remove-Item $Out -Force }
& python $pack $Tmp $Out
if ($LASTEXITCODE -ne 0) { throw 'python pack failed' }

Remove-Item $Tmp -Recurse -Force

Write-Host (("готово: {0} ({1:N0} байт)") -f $Out, (Get-Item $Out).Length)
