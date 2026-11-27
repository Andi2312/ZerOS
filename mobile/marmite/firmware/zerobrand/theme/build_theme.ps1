# Build the ZerOS default theme as three static RRO overlays.
#
# The theme "Silly" is the LineageOS theme: accent pink, font Rubik, icon
# shape roundedrect. Instead of applying those runtime overlays once (old
# service.sh approach), these APKs replace the framework defaults: they are
# marked android:isStatic="true" and preinstalled in /system/product/overlay,
# so OverlayManager keeps them enabled at every boot and the ThemePicker
# cannot switch them off. Picking another accent in Styles still works:
# runtime overlays have priority 1, ours 0, so a user choice wins.
#
# Build in an ASCII-only directory: apksigner cannot open Cyrillic paths.
#
#   powershell -File build_theme.ps1
#   powershell -File build_theme.ps1 -KeysDir D:\keys -ToolsDir D:\tools

param(
    [string]$KeysDir = 'C:\work\settings_apk\keys',
    [string]$ToolsDir = 'C:\Program Files (x86)\APK Editor Studio\tools',
    [string]$WorkDir = (Join-Path $env:TEMP 'zerobrand_theme'),
    [switch]$KeepWork
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$apktool = Join-Path $ToolsDir 'apktool.jar'
$zipalign = Join-Path $ToolsDir 'zipalign.exe'
$apksigner = Join-Path $ToolsDir 'apksigner.jar'

$need = @(
    $apktool, $zipalign, $apksigner,
    (Join-Path $KeysDir 'platform.pk8'),
    (Join-Path $KeysDir 'platform.x509.pem')
)
foreach ($f in $need) { if (-not (Test-Path $f)) { throw "missing: $f" } }

# source subdir in this folder -> output apk name
$overlays = @(
    @{ Dir = 'accent'; Apk = 'ZerOSAccent.apk' },
    @{ Dir = 'font';   Apk = 'ZerOSFont.apk' },
    @{ Dir = 'shape';  Apk = 'ZerOSShape.apk' }
)

$apktoolYml = @'
!!brut.androlib.meta.MetaInfo
apkFileName: {0}
compressionType: false
doNotCompress:
- resources.arsc
isFrameworkApk: false
packageInfo: null
sdkInfo:
  minSdkVersion: '29'
  targetSdkVersion: '29'
sharedLibrary: false
sparseResources: false
usesFramework:
  ids:
  - 1
version: 2.7.0
versionInfo:
  versionCode: '1'
  versionName: '1.0'
'@

if (Test-Path $WorkDir) { Remove-Item $WorkDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null

try {
    foreach ($o in $overlays) {
        $src = Join-Path $here $o.Dir
        if (-not (Test-Path $src)) { throw "missing sources: $src" }

        $w = Join-Path $WorkDir $o.Dir
        New-Item -ItemType Directory -Force -Path $w | Out-Null
        Copy-Item (Join-Path $src '*') $w -Recurse -Force
        ($apktoolYml -f $o.Apk) | Set-Content (Join-Path $w 'apktool.yml') -Encoding ASCII

        Push-Location $w
        try {
            & java -jar $apktool b . -o unsigned.apk
            if ($LASTEXITCODE -ne 0) { throw "apktool build failed: $($o.Dir)" }

            & $zipalign -f -p 4 unsigned.apk aligned.apk
            if ($LASTEXITCODE -ne 0) { throw "zipalign failed: $($o.Dir)" }

            & java -jar $apksigner sign `
                --key (Join-Path $KeysDir 'platform.pk8') `
                --cert (Join-Path $KeysDir 'platform.x509.pem') `
                --min-sdk-version 29 --v1-signing-enabled true --v2-signing-enabled true `
                --out signed.apk aligned.apk
            if ($LASTEXITCODE -ne 0) { throw "apksigner sign failed: $($o.Dir)" }

            $cert = & java -jar $apksigner verify --print-certs signed.apk
            if ($LASTEXITCODE -ne 0) { throw "apksigner verify failed: $($o.Dir)" }
            $cert | Select-Object -First 2

            Copy-Item signed.apk (Join-Path $here $o.Apk) -Force
        } finally { Pop-Location }
    }

    Get-ChildItem (Join-Path $here '*.apk') | Select-Object Name, Length, LastWriteTime
} finally {
    if (-not $KeepWork) { Remove-Item $WorkDir -Recurse -Force -ErrorAction SilentlyContinue }
}
