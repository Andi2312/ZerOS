<#
.SYNOPSIS
  Build the full ZerOS I TWRP zip for Wileyfox Swift 2 (marmite).

.DESCRIPTION
  The zip installs the whole OS: boot, vendor and system images are written
  to their partitions. Images are raw (as dumped from the device or taken
  from the LineageOS 17.1 zip), deflated inside the zip; TWRP's unzip
  streams them straight to the block devices (see full/update-binary).

  md5sums.txt is generated here and checked by the installer after writing
  each partition.

.EXAMPLE
  .\make_full_zip.ps1 -SystemImg C:\work\zos\system.img `
                      -BootImg   C:\work\zos\boot.img `
                      -VendorImg C:\work\zos\vendor.img
#>
param(
    [Parameter(Mandatory = $true)][string]$SystemImg,
    [Parameter(Mandatory = $true)][string]$BootImg,
    [Parameter(Mandatory = $true)][string]$VendorImg,
    [string]$Version = '1.0',
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sevenZip = 'C:\Program Files (x86)\AOMEI Partition Assistant\7z.exe'

if (-not $Out) {
    $Out = Join-Path $here ("dist\ZerOS-I-{0}-marmite-full.zip" -f $Version)
}
foreach ($f in @($SystemImg, $BootImg, $VendorImg)) {
    if (-not (Test-Path -LiteralPath $f)) { throw "missing $f" }
}
if (-not (Test-Path -LiteralPath $sevenZip)) { throw "missing 7z: $sevenZip" }

$stage = Join-Path ([IO.Path]::GetTempPath()) ('zerofull-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $stage | Out-Null

try {
    Copy-Item -LiteralPath (Join-Path $here 'full\README.txt') (Join-Path $stage 'README.txt')
    $meta = Join-Path $stage 'META-INF\com\google\android'
    New-Item -ItemType Directory -Force $meta | Out-Null
    Copy-Item -LiteralPath (Join-Path $here 'full\META-INF\com\google\android\update-binary')  $meta
    Copy-Item -LiteralPath (Join-Path $here 'full\META-INF\com\google\android\updater-script') $meta

    # Hard links instead of 3.5 GB copies when the paths are on one volume.
    $pairs = [ordered]@{ 'boot.img' = $BootImg; 'vendor.img' = $VendorImg; 'system.img' = $SystemImg }
    $sums = foreach ($name in $pairs.Keys) {
        $link = Join-Path $stage $name
        try {
            New-Item -ItemType HardLink -Path $link -Target (Resolve-Path -LiteralPath $pairs[$name]).Path -ErrorAction Stop | Out-Null
        }
        catch {
            Copy-Item -LiteralPath $pairs[$name] $link
        }
        $h = (Get-FileHash -LiteralPath $pairs[$name] -Algorithm MD5).Hash.ToLower()
        "$h  $name"
    }
    $md5 = Join-Path $stage 'md5sums.txt'
    [IO.File]::WriteAllText($md5, (($sums -join "`n") + "`n"), (New-Object Text.UTF8Encoding($false)))

    $dist = Split-Path -Parent $Out
    if (-not (Test-Path $dist)) { New-Item -ItemType Directory $dist | Out-Null }
    if (Test-Path -LiteralPath $Out) { Remove-Item -LiteralPath $Out -Force }

    Push-Location $stage
    try {
        & $sevenZip a -tzip -mx=5 -y $Out 'META-INF' 'README.txt' 'md5sums.txt' 'boot.img' 'vendor.img' 'system.img' | Out-Null
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
