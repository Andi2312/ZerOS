# Rebuild the stock LineageOS Settings.apk with the «About phone» screen patched:
# «LineageOS API level» removed, «LineageOS version» renamed to «ZerOS version».
#
# Settings is a platform-signed app: it must be re-signed with the AOSP
# platform test key (build/target/product/security of android-10.0.0_r47),
# otherwise it loses its signature-level permissions. The certificate hash is
# checked after signing.
#
# Needs: JDK 21, apktool, apksigner, zipalign, python, adb with the phone
# connected. Everything is built under C:\work (ASCII path): apktool and aapt2
# do not cope with the Cyrillic project path.

param(
    [string]$Work = 'C:\work\settings_apk',
    [string]$Apk  = '',            # source Settings.apk; pulled from the phone when empty
    [string]$Tag  = 'android-10.0.0_r47'
)

$ErrorActionPreference = 'Stop'

$Root  = Split-Path -Parent $MyInvocation.MyCommand.Path
$Dist  = Join-Path $Root 'dist'
$Dec   = Join-Path $Work 'dec'
$Keys  = Join-Path $Root 'keys'
$Jdk   = 'C:\Program Files\Java\jdk-21.0.12.1'
if (-not (Test-Path (Join-Path $Jdk 'bin\java.exe')) -and $env:JAVA_HOME) {
    $Jdk = $env:JAVA_HOME
}
$Java     = Join-Path $Jdk 'bin\java.exe'
$Keytool  = Join-Path $Jdk 'bin\keytool.exe'
$Tools    = 'C:\Program Files (x86)\APK Editor Studio\tools'
$Apktool  = Join-Path $Tools 'apktool.jar'
$Apksigner = Join-Path $Tools 'apksigner.jar'
$Zipalign = Join-Path $Tools 'zipalign.exe'

foreach ($tool in @($Java, $Apktool, $Apksigner, $Zipalign)) {
    if (-not (Test-Path $tool)) { throw "missing: $tool" }
}

# AOSP platform test key SHA-256 — must survive the rebuild.
$WantCert = 'C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8'

New-Item -ItemType Directory -Force $Work, $Dist, $Keys | Out-Null

if (-not $Apk) {
    $Apk = Join-Path $Dist 'Settings-stock.apk'
    if (-not (Test-Path $Apk)) {
        & adb pull /system/product/priv-app/Settings/Settings.apk $Apk
        if ($LASTEXITCODE -ne 0) { throw 'adb pull failed' }
    }
}

# apktool resolves framework resource ids against its installed frameworks.
# Settings is built against Android 10 and lineageos.platform (package id 63).
$Framework = Join-Path $Work 'framework-res.apk'
if (-not (Test-Path $Framework)) {
    & adb pull /system/framework/framework-res.apk $Framework
    if ($LASTEXITCODE -ne 0) { throw 'adb pull framework-res failed' }
}
$Platform = Join-Path $Work 'org.lineageos.platform-res.apk'
if (-not (Test-Path $Platform)) {
    & adb pull /system/framework/org.lineageos.platform-res.apk $Platform
    if ($LASTEXITCODE -ne 0) { throw 'adb pull org.lineageos.platform-res failed' }
}
& $Java -jar $Apktool if $Framework | Out-Null
& $Java -jar $Apktool if $Platform | Out-Null

if (Test-Path $Dec) { Remove-Item $Dec -Recurse -Force }
& $Java -Xmx6g -jar $Apktool d -f -o $Dec $Apk
if ($LASTEXITCODE -ne 0) { throw 'apktool d failed' }

& python (Join-Path $Root 'patch_res.py') $Dec
if ($LASTEXITCODE -ne 0) { throw 'patch_res.py failed' }

$Unsigned = Join-Path $Work 'out-unsigned.apk'
$Aligned  = Join-Path $Work 'out-aligned.apk'
& $Java -Xmx6g -jar $Apktool b $Dec -o $Unsigned
if ($LASTEXITCODE -ne 0) { throw 'apktool b failed' }

& $Zipalign -p -f 4 $Unsigned $Aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

# Public AOSP test keys from the same tag the ROM was built from.
$Pk8 = Join-Path $Keys 'platform.pk8'
$Pem = Join-Path $Keys 'platform.x509.pem'
$Base = "https://android.googlesource.com/platform/build/+/refs/tags/$Tag/target/product/security/"
foreach ($f in @(@('platform.pk8', $Pk8), @('platform.x509.pem', $Pem))) {
    if (-not (Test-Path $f[1])) {
        $r = Invoke-WebRequest -Uri ($Base + $f[0] + '?format=TEXT') -UseBasicParsing -TimeoutSec 60
        [System.IO.File]::WriteAllBytes($f[1], [Convert]::FromBase64String(($r.Content -replace '\s', '')))
    }
}

$Signed = Join-Path $Dist 'Settings.apk'
& $Java -jar $Apksigner sign --key $Pk8 --cert $Pem `
    --min-sdk-version 29 --v1-signing-enabled true --v2-signing-enabled true `
    --out $Signed $Aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner sign failed' }
& $Java -jar $Apksigner verify --print-certs $Signed | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'apksigner verify failed' }

$got = (& $Keytool -printcert -file $Pem | Select-String 'SHA256:').Line -replace '.*SHA256:\s*', ''
if ($got.Trim() -ne $WantCert) { throw "platform cert mismatch: $got" }

& python (Join-Path $Root 'make_module.py')
