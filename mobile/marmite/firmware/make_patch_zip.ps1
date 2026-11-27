<#
.SYNOPSIS
  Build ZerOS I TWRP patch zip for Wileyfox Swift 2 (marmite).

.DESCRIPTION
  The zip writes branding files directly into the /system partition.
  It needs the ready build.prop (stock build.prop + appended ZerOS block)
  and the built artifacts: bootanimation.zip, theme overlays, logo overlay,
  Settings.apk, Updater.apk.

.EXAMPLE
  .\make_patch_zip.ps1 -BuildProp C:\work\sys\build.prop.new
#>
param(
    [Parameter(Mandatory = $true)][string]$BuildProp,
    [string]$Version = '1.0',
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

if (-not $Out) {
    $Out = Join-Path $here ("dist\ZerOS-I-{0}-marmite-patch.zip" -f $Version)
}

$items = [ordered]@{
    'system/build.prop'                              = $BuildProp
    'system/media/bootanimation.zip'                 = Join-Path $here 'zerobrand\bootanimation.zip'
    'system/product/overlay/ZerOSAccent.apk'         = Join-Path $here 'zerobrand\theme\ZerOSAccent.apk'
    'system/product/overlay/ZerOSFont.apk'           = Join-Path $here 'zerobrand\theme\ZerOSFont.apk'
    'system/product/overlay/ZerOSShape.apk'          = Join-Path $here 'zerobrand\theme\ZerOSShape.apk'
    'system/product/overlay/ZerOSLogo.apk'           = Join-Path $here 'zerobrand\logo\ZerOSLogo.apk'
    'system/product/priv-app/Settings/Settings.apk'  = Join-Path $here 'zerosettings\dist\Settings.apk'
    'system/priv-app/Updater/Updater.apk'            = Join-Path $here 'zeroupdater\dist\Updater.apk'
    'system/priv-app/ZerOSAbout/ZerOSAbout.apk'      = Join-Path $here 'zerocabout\dist\ZerOSAbout.apk'
}

foreach ($key in $items.Keys) {
    if (-not (Test-Path -LiteralPath $items[$key])) {
        throw "missing $($items[$key]) - build it first"
    }
}

$stage = Join-Path ([IO.Path]::GetTempPath()) ('zeropatch-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $stage | Out-Null

try {
    $sums = foreach ($key in $items.Keys) {
        $h = (Get-FileHash -LiteralPath $items[$key] -Algorithm MD5).Hash.ToLower()
        "$h  $key"
    }
    $md5 = Join-Path $stage 'md5sums.txt'
    [IO.File]::WriteAllText($md5, (($sums -join "`n") + "`n"), (New-Object Text.UTF8Encoding($false)))

    $dist = Split-Path -Parent $Out
    if (-not (Test-Path $dist)) { New-Item -ItemType Directory $dist | Out-Null }
    if (Test-Path -LiteralPath $Out) { Remove-Item -LiteralPath $Out -Force }

    $zip = [IO.Compression.ZipFile]::Open($Out, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $add = {
            param($zip, $name, $src, $mode)
            $entry = $zip.CreateEntry($name, [IO.Compression.CompressionLevel]::Optimal)
            $entry.ExternalAttributes = $mode -shl 16
            $o = $entry.Open()
            $i = [IO.File]::OpenRead($src)
            try { $i.CopyTo($o) } finally { $i.Dispose(); $o.Dispose() }
        }

        # 0644 = 0x81A4, 0755 = 0x81ED
        & $add $zip 'META-INF/com/google/android/updater-script' (Join-Path $here 'patch\META-INF\com\google\android\updater-script') 33188
        & $add $zip 'META-INF/com/google/android/update-binary'  (Join-Path $here 'patch\META-INF\com\google\android\update-binary')  33261
        & $add $zip 'README.txt'                                 (Join-Path $here 'patch\README.txt')                                33188
        & $add $zip 'md5sums.txt'                                $md5                                                                33188
        foreach ($key in $items.Keys) {
            & $add $zip $key $items[$key] 33188
        }
    }
    finally {
        $zip.Dispose()
    }

    $size = (Get-Item -LiteralPath $Out).Length
    Write-Host ("OK: {0}  ({1:N0} bytes)" -f $Out, $size)
}
finally {
    Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue
}
