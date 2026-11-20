# Rebuild the stock LineageOS Updater.apk with «ZerOS» instead of «LineageOS»
# in the UI (header «LineageOS\n1.0» and the build list entries).
#
# The Updater is signed with the AOSP test key (build/target/product/security
# of android-10.0.0_r47), so the rebuilt APK is re-signed with the same key and
# keeps its privileged/signature permissions. The certificate hash is checked
# after signing.
#
# Needs: JDK 21, apktool, apksigner, zipalign, python, adb with the phone
# connected. Everything is built under C:\work (ASCII path): apktool and aapt2
# do not cope with the Cyrillic project path.

param(
    [string]$Work = 'C:\work\updater_apk',
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
$Java      = Join-Path $Jdk 'bin\java.exe'
$Keytool   = Join-Path $Jdk 'bin\keytool.exe'
$Tools     = 'C:\Program Files (x86)\APK Editor Studio\tools'
$Apktool   = Join-Path $Tools 'apktool.jar'
$Apksigner = Join-Path $Tools 'apksigner.jar'
$Zipalign  = Join-Path $Tools 'zipalign.exe'

foreach ($tool in @($Java, $Apktool, $Apksigner, $Zipalign)) {
    if (-not (Test-Path $tool)) { throw "missing: $tool" }
}

# AOSP test key SHA-256 — must survive the rebuild.
$WantCert = 'A4:0D:A8:0A:59:D1:70:CA:A9:50:CF:15:C1:8C:45:4D:47:A3:9B:26:98:9D:8B:64:0E:CD:74:5B:A7:1B:F5:DC'

New-Item -ItemType Directory -Force $Work, $Dist, $Keys | Out-Null

$Apk = Join-Path $Dist 'Updater-stock.apk'
if (-not (Test-Path $Apk)) {
    & adb pull /system/priv-app/Updater/Updater.apk $Apk
    if ($LASTEXITCODE -ne 0) { throw 'adb pull failed' }
}

# apktool resolves framework resource ids against its installed frameworks.
$Framework = Join-Path $Work 'framework-res.apk'
if (-not (Test-Path $Framework)) {
    & adb pull /system/framework/framework-res.apk $Framework
    if ($LASTEXITCODE -ne 0) { throw 'adb pull framework-res failed' }
}
& $Java -jar $Apktool if $Framework | Out-Null

if (Test-Path $Dec) { Remove-Item $Dec -Recurse -Force }
& $Java -Xmx4g -jar $Apktool d -f -o $Dec $Apk
if ($LASTEXITCODE -ne 0) { throw 'apktool d failed' }

& python (Join-Path $Root 'patch_res.py') $Dec
if ($LASTEXITCODE -ne 0) { throw 'patch_res.py failed' }

$Unsigned = Join-Path $Work 'out-unsigned.apk'
$Aligned  = Join-Path $Work 'out-aligned.apk'
& $Java -Xmx4g -jar $Apktool b $Dec -o $Unsigned
if ($LASTEXITCODE -ne 0) { throw 'apktool b failed' }

& $Zipalign -p -f 4 $Unsigned $Aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

# Public AOSP test keys from the same tag the ROM was built from.
$Pk8 = Join-Path $Keys 'testkey.pk8'
$Pem = Join-Path $Keys 'testkey.x509.pem'
$Base = "https://android.googlesource.com/platform/build/+/refs/tags/$Tag/target/product/security/"
foreach ($f in @(@('testkey.pk8', $Pk8), @('testkey.x509.pem', $Pem))) {
    if (-not (Test-Path $f[1])) {
        $r = Invoke-WebRequest -Uri ($Base + $f[0] + '?format=TEXT') -UseBasicParsing -TimeoutSec 60
        [System.IO.File]::WriteAllBytes($f[1], [Convert]::FromBase64String(($r.Content -replace '\s', '')))
    }
}

$Signed = Join-Path $Dist 'Updater.apk'
& $Java -jar $Apksigner sign --key $Pk8 --cert $Pem `
    --min-sdk-version 29 --v1-signing-enabled true --v2-signing-enabled true `
    --out $Signed $Aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner sign failed' }
& $Java -jar $Apksigner verify --print-certs $Signed | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'apksigner verify failed' }

$got = (& $Keytool -printcert -file $Pem | Select-String 'SHA256:').Line -replace '.*SHA256:\s*', ''
if ($got.Trim() -ne $WantCert) { throw "testkey cert mismatch: $got" }

& python (Join-Path $Root 'make_module.py')
