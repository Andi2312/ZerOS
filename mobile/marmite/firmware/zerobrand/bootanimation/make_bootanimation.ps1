<#
make_bootanimation.ps1 — собирает bootanimation.zip для ZerOS.

Рисует на чёрном фоне надпись «ZerOS I» акцентным цветом палитры
(#F2A6A0): плавное появление, потом держится до конца загрузки.

Разрешение задаётся под устройство (marmite — Wileyfox Swift 2, 720x1280).

Запуск:
    powershell -NoProfile -ExecutionPolicy Bypass -File .\make_bootanimation.ps1
    powershell -NoProfile -ExecutionPolicy Bypass -File .\make_bootanimation.ps1 -Width 1080 -Height 2160
#>

param(
    [int]$Width = 720,
    [int]$Height = 1280,
    [int]$Fps = 30,
    [int]$FadeFrames = 24,
    [int]$HoldFrames = 36,
    [string]$Text = 'ZerOS I',
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Out) { $Out = Join-Path (Split-Path -Parent $Root) 'bootanimation.zip' }

$Frames = $FadeFrames + $HoldFrames
$Tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("zeros-boot-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
$Part = Join-Path $Tmp 'part0'
New-Item -ItemType Directory -Force -Path $Part | Out-Null

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

Write-Host ("кадров: {0} ({1}x{2}, {3} fps)" -f $Frames, $Width, $Height, $Fps)

for ($i = 0; $i -lt $Frames; $i++) {
    $alpha = 255
    if ($i -lt $FadeFrames) {
        $t = ($i + 1) / [double]$FadeFrames
        $alpha = [int][Math]::Round(255 * [Math]::Pow($t, 1.6))
    }

    $g.Clear($Bg)

    $textColor = [System.Drawing.Color]::FromArgb($alpha, $Accent)
    $rect = New-Object System.Drawing.RectangleF(0, 0, $Width, $Height)
    $brush = New-Object System.Drawing.SolidBrush($textColor)
    $g.DrawString($Text, $font, $brush, $rect, $fmt)
    $brush.Dispose()

    # акцентная черта под надписью: растёт вместе с появлением
    $barAlpha = [int][Math]::Round($alpha * 0.7)
    $w = [Math]::Max(1, [int][Math]::Round($barW * ($alpha / 255.0)))
    $barColor = [System.Drawing.Color]::FromArgb($barAlpha, $Accent)
    $barBrush = New-Object System.Drawing.SolidBrush($barColor)
    $g.FillRectangle($barBrush, [single]($cx - $w / 2.0), [single]$barY, [single]$w, [single]$barH)
    $barBrush.Dispose()

    $file = Join-Path $Part ("frame_{0:d3}.png" -f $i)
    $bmp.Save($file, [System.Drawing.Imaging.ImageFormat]::Png)
}

$g.Dispose()
$bmp.Dispose()
$font.Dispose()
$family.Dispose()

# desc.txt: размер экрана, fps, одна часть (проиграть один раз и держать кадр)
$desc = Join-Path $Tmp 'desc.txt'
$descText = "$Width $Height $Fps`np 1 0 part0`n"
[System.IO.File]::WriteAllText($desc, $descText, (New-Object System.Text.UTF8Encoding($false)))

# zip без сжатия: так требует загрузчик Android
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path $Out) { Remove-Item $Out -Force }
$zip = [System.IO.Compression.ZipFile]::Open($Out, [System.IO.Compression.ZipArchiveMode]::Create)
function Add-Stored($zip, $path, $entryName) {
    $e = $zip.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::NoCompression)
    $s = $e.Open()
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $s.Write($bytes, 0, $bytes.Length)
    $s.Dispose()
}
Add-Stored $zip $desc 'desc.txt'
foreach ($f in (Get-ChildItem $Part -Filter *.png | Sort-Object Name)) {
    Add-Stored $zip $f.FullName ('part0/' + $f.Name)
}
$zip.Dispose()

Remove-Item $Tmp -Recurse -Force

Write-Host ("готово: {0} ({1:N0} байт)" -f $Out, (Get-Item $Out).Length)
