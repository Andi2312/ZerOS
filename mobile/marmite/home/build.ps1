# Build ZerOS Home (launcher) without Gradle.
#
# Needs: JDK 21, aapt2, android.jar (API 33), r8.jar (d8), zipalign, apksigner.
# Builds in %TEMP% because aapt2 chokes on Cyrillic paths.

$ErrorActionPreference = 'Stop'

$Proj  = Split-Path -Parent $MyInvocation.MyCommand.Path
$Work  = Join-Path $env:TEMP 'zshome'
$Jdk   = 'C:\Program Files\Java\jdk-21.0.12.1'
$Javac = Join-Path $Jdk 'bin\javac.exe'
$Java  = Join-Path $Jdk 'bin\java.exe'
$Jar   = Join-Path $Jdk 'bin\jar.exe'
$Keytool = Join-Path $Jdk 'bin\keytool.exe'

$AndroidJar = @(
    'G:\Android-sdk\platforms\android-36.1\android.jar',
    (Join-Path $env:TEMP 'opencode\android\platform-33\android-13\android.jar'),
    (Join-Path $env:TEMP 'opencode\android\platform-27\android-8.1.0\android.jar')
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $AndroidJar) { throw 'android.jar not found (android-36.1 / platform-33 / platform-27)' }

$R8    = Join-Path $env:TEMP 'opencode\android\r8.jar'
$Tools = 'C:\Program Files (x86)\APK Editor Studio\tools'
foreach ($tool in @($Javac, $R8, (Join-Path $Tools 'zipalign.exe'), (Join-Path $Tools 'apksigner.jar'))) {
    if (-not (Test-Path $tool)) { throw "missing: $tool" }
}

$Aapt2 = 'C:\Users\SillyTech\.gradle\caches\9.4.1\transforms\467bdda5c6a853f23b3335dfdc88698e\transformed\aapt2-9.2.1-15009934-windows\aapt2.exe'
if (-not (Test-Path $Aapt2)) {
    $found = Get-ChildItem (Join-Path $env:USERPROFILE '.gradle\caches') -Recurse -Filter aapt2.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $Aapt2 = $found.FullName }
}
if (-not $Aapt2 -or -not (Test-Path $Aapt2)) { throw 'aapt2.exe not found in gradle caches' }

$MinSdk      = 22
$TargetSdk   = 33
$VersionCode = 3
$VersionName = '0.0.1'

# 1. clean copy outside the Cyrillic path
Remove-Item $Work -Recurse -Force -ErrorAction SilentlyContinue
robocopy $Proj $Work /E /XD build dist /XF build.ps1 /NFL /NDL /NJH /NJS | Out-Null
$Build = Join-Path $Work 'build'
New-Item -ItemType Directory -Force $Build | Out-Null

# 2. resources
& $Aapt2 compile --dir (Join-Path $Work 'res') -o (Join-Path $Build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

& $Aapt2 link -o (Join-Path $Build 'base.apk') `
    -I $AndroidJar `
    --manifest (Join-Path $Work 'AndroidManifest.xml') `
    -A (Join-Path $Work 'assets') `
    --java (Join-Path $Build 'gen') `
    --min-sdk-version $MinSdk --target-sdk-version $TargetSdk `
    --version-code $VersionCode --version-name $VersionName `
    --auto-add-overlay `
    (Join-Path $Build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

# 3. java -> bytecode
$Classes = Join-Path $Build 'classes'
New-Item -ItemType Directory -Force $Classes | Out-Null
$Sources = @(Get-ChildItem $Classes -Recurse -Filter *.java -ErrorAction SilentlyContinue | ForEach-Object FullName)
$Sources += @(Get-ChildItem (Join-Path $Build 'gen') -Recurse -Filter *.java | ForEach-Object FullName)
$Sources += @(Get-ChildItem (Join-Path $Work 'src') -Recurse -Filter *.java | ForEach-Object FullName)
& $Javac -source 8 -target 8 -Xlint:-options -bootclasspath $AndroidJar -d $Classes $Sources
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

# 4. bytecode -> dex
$ClassesJar = Join-Path $Build 'classes.jar'
& $Jar cf $ClassesJar -C $Classes .
$Dex = Join-Path $Build 'dex'
New-Item -ItemType Directory -Force $Dex | Out-Null
& $Java -cp $R8 com.android.tools.r8.D8 --release --min-api $MinSdk `
    --lib $AndroidJar --output $Dex $ClassesJar
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

# 5. classes.dex into the apk
$Unsigned = Join-Path $Build 'unsigned.apk'
& python (Join-Path $Work 'add_dex.py') (Join-Path $Build 'base.apk') (Join-Path $Dex 'classes.dex') $Unsigned

# 6. align + sign
$Aligned = Join-Path $Build 'aligned.apk'
& (Join-Path $Tools 'zipalign.exe') -f 4 $Unsigned $Aligned

$Ks = Join-Path $env:TEMP 'opencode\android\zeroshome.jks'
if (-not (Test-Path $Ks)) {
    & $Keytool -genkeypair -keystore $Ks -alias zeroshome -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass zeroshome -keypass zeroshome `
        -dname 'CN=ZeroC, O=ZeroC, C=RU'
}

$Out = Join-Path $Build 'ZerOS_Home.apk'
& $Java -jar (Join-Path $Tools 'apksigner.jar') sign `
    --ks $Ks --ks-pass pass:zeroshome --key-pass pass:zeroshome `
    --min-sdk-version $MinSdk --out $Out $Aligned

& $Java -jar (Join-Path $Tools 'apksigner.jar') verify --print-certs $Out

$Dist = Join-Path $Proj 'dist'
New-Item -ItemType Directory -Force $Dist | Out-Null
Copy-Item $Out $Dist -Force

Write-Host ""
Write-Host "APK: $(Join-Path $Dist 'ZerOS_Home.apk')"
exit 0
