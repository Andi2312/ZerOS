# Build the ZerOS logo RRO overlay APK for LineageParts.
#
# The overlay replaces drawable/logo_lineage in org.lineageos.lineageparts.
# It is static and must be installed as a preinstalled system overlay
# (the zerobrand Magisk module puts it in /system/product/overlay).
#
# Build in an ASCII-only directory: apksigner cannot open Cyrillic paths.
#
#   powershell -File build_overlay.ps1
#   powershell -File build_overlay.ps1 -KeysDir D:\keys -ToolsDir D:\tools

param(
    [string]$KeysDir = 'C:\work\settings_apk\keys',
    [string]$ToolsDir = 'C:\Program Files (x86)\APK Editor Studio\tools',
    [string]$WorkDir = (Join-Path $env:TEMP 'zerobrand_overlay')
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$apktool = Join-Path $ToolsDir 'apktool.jar'
$zipalign = Join-Path $ToolsDir 'zipalign.exe'
$apksigner = Join-Path $ToolsDir 'apksigner.jar'

foreach ($f in @($apktool, $zipalign, $apksigner, (Join-Path $here 'logo_lineage.png'))) {
    if (-not (Test-Path $f)) { throw "missing: $f" }
}

$res = Join-Path $WorkDir 'res\drawable-nodpi'
New-Item -ItemType Directory -Force -Path $res | Out-Null
Copy-Item (Join-Path $here 'logo_lineage.png') (Join-Path $res 'logo_lineage.png') -Force

@'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="org.zeroc.logo"
    android:versionCode="1"
    android:versionName="1.0">
    <application
        android:hasCode="false"
        android:label="ZerOS logo" />
    <overlay
        android:targetPackage="org.lineageos.lineageparts"
        android:isStatic="true"
        android:priority="1" />
</manifest>
'@ | Set-Content (Join-Path $WorkDir 'AndroidManifest.xml') -Encoding UTF8

@'
!!brut.androlib.meta.MetaInfo
apkFileName: ZerOSLogo.apk
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
    Copy-Item signed.apk (Join-Path $here 'ZerOSLogo.apk') -Force
}
finally {
    Pop-Location
}

Get-Item (Join-Path $here 'ZerOSLogo.apk') | Select-Object FullName, Length, LastWriteTime
