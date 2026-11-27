# Build the ZerOS charging-sound helper app (org.zeroc.charge).
#
# The app has one manifest receiver for ACTION_POWER_DISCONNECTED and plays
# /system/product/media/audio/ui/ChargingStopped.ogg, which the system does
# not play by itself. It is preinstalled by the zerobrand Magisk module into
# /system/product/app/ZerOSCharge.
#
# Build in an ASCII-only directory: apktool/apksigner cannot open Cyrillic
# paths on Windows.
#
#   powershell -File build_charge.ps1
#   powershell -File build_charge.ps1 -KeysDir D:\keys -ToolsDir D:\tools

param(
    [string]$KeysDir = 'C:\work\settings_apk\keys',
    [string]$ToolsDir = 'C:\Program Files (x86)\APK Editor Studio\tools',
    [string]$WorkDir = (Join-Path $env:TEMP 'zerobrand_charge')
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$apktool = Join-Path $ToolsDir 'apktool.jar'
$zipalign = Join-Path $ToolsDir 'zipalign.exe'
$apksigner = Join-Path $ToolsDir 'apksigner.jar'

foreach ($f in @($apktool, $zipalign, $apksigner,
                 (Join-Path $here 'AndroidManifest.xml'),
                 (Join-Path $here 'smali\org\zeroc\charge\PowerReceiver.smali'))) {
    if (-not (Test-Path $f)) { throw "missing: $f" }
}

# Sources go to an ASCII work dir; stale files only get overwritten.
$dstSmali = Join-Path $WorkDir 'smali\org\zeroc\charge'
New-Item -ItemType Directory -Force -Path $dstSmali | Out-Null
Copy-Item (Join-Path $here 'AndroidManifest.xml') (Join-Path $WorkDir 'AndroidManifest.xml') -Force
Copy-Item (Join-Path $here 'smali\org\zeroc\charge\*.smali') $dstSmali -Force

@'
!!brut.androlib.meta.MetaInfo
apkFileName: ZerOSCharge.apk
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
'@ | Set-Content (Join-Path $WorkDir 'apktool.yml') -Encoding UTF8

Push-Location $WorkDir
try {
    & java -jar $apktool b . -o unsigned.apk
    if ($LASTEXITCODE -ne 0) { throw 'apktool build failed' }
    & $zipalign -f -p 4 unsigned.apk aligned.apk
    if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }
    & java -jar $apksigner sign `
        --key (Join-Path $KeysDir 'platform.pk8') `
        --cert (Join-Path $KeysDir 'platform.x509.pem') `
        --min-sdk-version 29 --v1-signing-enabled true --v2-signing-enabled true `
        --out signed.apk aligned.apk
    if ($LASTEXITCODE -ne 0) { throw 'apksigner sign failed' }
    & java -jar $apksigner verify --print-certs signed.apk
    if ($LASTEXITCODE -ne 0) { throw 'apksigner verify failed' }
    Copy-Item signed.apk (Join-Path $here 'ZerOSCharge.apk') -Force
}
finally {
    Pop-Location
}

Get-Item (Join-Path $here 'ZerOSCharge.apk') | Select-Object FullName, Length, LastWriteTime
