<#
.SYNOPSIS
  Build the full ZerOS I TWRP zip for Wileyfox Swift 2 (marmite).

.DESCRIPTION
  The zip writes the whole system partition from a raw image, so it installs
  the entire OS on top of an already present LineageOS 17.1 boot/vendor.
  boot, vendor, recovery and splash are not touched.

  The system image is taken raw (3.5 GB) and deflated inside the zip; TWRP's
  unzip streams it straight to the block device (see full/update-binary).

.EXAMPLE
  7z x dist\ZerOS-I-1.0-marmite-system.img.7z -oC:\work\zos
  .\make_full_zip.ps1 -SystemImg C:\work\zos\system.img
#>
param(
    [Parameter(Mandatory = $true)][string]$SystemImg,
    [string]$Version = '1.0',
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sevenZip = 'C:\Program Files (x86)\AOMEI Partition Assistant\7z.exe'

if (-not $Out) {
    $Out = Join-Path $here ("dist\ZerOS-I-{0}-marmite-full.zip" -f $Version)
}
if (-not (Test-Path -LiteralPath $SystemImg)) { throw "missing $SystemImg" }
if (-not (Test-Path -LiteralPath $sevenZip)) { throw "missing 7z: $sevenZip" }

$stage = Join-Path ([IO.Path]::GetTempPath()) ('zerofull-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $stage | Out-Null

try {
    Copy-Item -LiteralPath (Join-Path $here 'full\README.txt') (Join-Path $stage 'README.txt')
    $meta = Join-Path $stage 'META-INF\com\google\android'
    New-Item -ItemType Directory -Force $meta | Out-Null
    Copy-Item -LiteralPath (Join-Path $here 'full\META-INF\com\google\android\update-binary')  $meta
    Copy-Item -LiteralPath (Join-Path $here 'full\META-INF\com\google\android\updater-script') $meta

    # Hard link instead of a 3.5 GB copy when both paths are on one volume.
    $link = Join-Path $stage 'system.img'
    try {
        New-Item -ItemType HardLink -Path $link -Target (Resolve-Path -LiteralPath $SystemImg).Path -ErrorAction Stop | Out-Null
    }
    catch {
        Copy-Item -LiteralPath $SystemImg $link
    }

    $dist = Split-Path -Parent $Out
    if (-not (Test-Path $dist)) { New-Item -ItemType Directory $dist | Out-Null }
    if (Test-Path -LiteralPath $Out) { Remove-Item -LiteralPath $Out -Force }

    Push-Location $stage
    try {
        & $sevenZip a -tzip -mx=5 -y $Out 'META-INF' 'README.txt' 'system.img' | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "7z failed with $LASTEXITCODE" }
    }
    finally {
        Pop-Location
    }

    $size = (Get-Item -LiteralPath $Out).Length
    Write-Host ("OK: {0}  ({1:N0} bytes)" -f $Out, $size)
}
finally {
    Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue
}
