# Build the from-scratch ZerOS Home launcher.
# The project lives under a Cyrillic path, so everything is copied to C:\work first:
# aapt2 on Windows does not cope with non-ASCII paths.

$ErrorActionPreference = 'Stop'

$Proj    = $PSScriptRoot
$Work    = 'C:\work\zerhome'
$Dist    = Join-Path $Proj 'dist'
$OutName = 'ZerHome-0.1.0.apk'

$Jdk = 'C:\Program Files\Java\jdk-21.0.12.1'
if (-not (Test-Path (Join-Path $Jdk 'bin\javac.exe')) -and $env:JAVA_HOME) {
    $Jdk = $env:JAVA_HOME
}
$Javac   = Join-Path $Jdk 'bin\javac.exe'
$Java    = Join-Path $Jdk 'bin\java.exe'
$Jar     = Join-Path $Jdk 'bin\jar.exe'
$Keytool = Join-Path $Jdk 'bin\keytool.exe'

$AndroidJar = 'G:\Android-sdk\platforms\android-36.1\android.jar'
if (-not (Test-Path $AndroidJar)) { throw "missing $AndroidJar" }

$Aapt2 = 'C:\Users\SillyTech\.gradle\caches\9.4.1\transforms\467bdda5c6a853f23b3335dfdc88698e\transformed\aapt2-9.2.1-15009934-windows\aapt2.exe'
if (-not (Test-Path $Aapt2)) {
    $found = Get-ChildItem (Join-Path $env:USERPROFILE '.gradle\caches') -Recurse -Filter aapt2.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $Aapt2 = $found.FullName }
}
if (-not (Test-Path $Aapt2)) { throw 'aapt2.exe not found in gradle caches' }

$R8 = Get-ChildItem 'G:\Android-sdk\build-tools' -Recurse -Filter 'd8.jar' -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
if (-not $R8) { throw 'd8.jar not found' }

$Tools = 'C:\Program Files (x86)\APK Editor Studio\tools'
foreach ($tool in @($Javac, $Java, $R8.FullName, (Join-Path $Tools 'zipalign.exe'), (Join-Path $Tools 'apksigner.jar'))) {
    if (-not (Test-Path $tool)) { throw "missing $tool" }
}

$MinSdk = 26
$TargetSdk = 29
$VersionCode = 1
$VersionName = '0.1.0'

# 1. clean copy outside the Cyrillic path
if (Test-Path $Work) { Remove-Item $Work -Recurse -Force }
New-Item -ItemType Directory -Force $Work | Out-Null
robocopy (Join-Path $Proj 'src') (Join-Path $Work 'src') /E /NFL /NDL /NJH /NJS | Out-Null
robocopy (Join-Path $Proj 'res') (Join-Path $Work 'res') /E /NFL /NDL /NJH /NJS | Out-Null
Copy-Item (Join-Path $Proj 'AndroidManifest.xml') (Join-Path $Work 'AndroidManifest.xml') -Force

$Build = Join-Path $Work 'build'
New-Item -ItemType Directory -Force $Build | Out-Null
$Gen = Join-Path $Build 'gen'
New-Item -ItemType Directory -Force $Gen | Out-Null

# 2. resources
& $Aapt2 compile --dir (Join-Path $Work 'res') -o (Join-Path $Build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

& $Aapt2 link -o (Join-Path $Build 'base.apk') `
    -I $AndroidJar `
    --manifest (Join-Path $Work 'AndroidManifest.xml') `
    --java $Gen `
    --min-sdk-version $MinSdk --target-sdk-version $TargetSdk `
    --version-code $VersionCode --version-name $VersionName `
    (Join-Path $Build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

# 3. java -> bytecode
$Classes = Join-Path $Build 'classes'
New-Item -ItemType Directory -Force $Classes | Out-Null
$Sources = @(Get-ChildItem (Join-Path $Work 'src') -Recurse -Filter *.java | ForEach-Object FullName)
$Sources += @(Get-ChildItem $Gen -Recurse -Filter *.java | ForEach-Object FullName)

$ArgFile = Join-Path $Build 'sources.txt'
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($ArgFile, ($Sources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }), $utf8)

& $Javac --release 8 -Xlint:-options -nowarn -classpath $AndroidJar -d $Classes "@$ArgFile"
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

# 4. bytecode -> dex
$ClassesJar = Join-Path $Build 'classes.jar'
& $Jar cf $ClassesJar -C $Classes .
$Dex = Join-Path $Build 'dex'
New-Item -ItemType Directory -Force $Dex | Out-Null
& $Java -cp $R8.FullName com.android.tools.r8.D8 --min-api $MinSdk --lib $AndroidJar --output $Dex $ClassesJar
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

# 5. dex into the apk
$Unsigned = Join-Path $Build 'unsigned.apk'
Copy-Item (Join-Path $Proj '..\deps\add_dex.py') (Join-Path $Build 'add_dex.py') -Force
& python (Join-Path $Build 'add_dex.py') (Join-Path $Build 'base.apk') (Join-Path $Dex 'classes.dex') $Unsigned
if ($LASTEXITCODE -ne 0) { throw 'add_dex failed' }

# 6. align + sign
$Aligned = Join-Path $Build 'aligned.apk'
& (Join-Path $Tools 'zipalign.exe') -f 4 $Unsigned $Aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

$Ks = Join-Path $env:TEMP 'opencode\android\zeroshome.jks'
if (-not (Test-Path $Ks)) {
    & $Keytool -genkeypair -keystore $Ks -alias zeroshome -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass zeroshome -keypass zeroshome `
        -dname 'CN=ZeroC, O=ZeroC, C=RU'
}

$Out = Join-Path $Build $OutName
& $Java -jar (Join-Path $Tools 'apksigner.jar') sign `
    --ks $Ks --ks-pass pass:zeroshome --key-pass pass:zeroshome `
    --min-sdk-version $MinSdk --out $Out $Aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }

& $Java -jar (Join-Path $Tools 'apksigner.jar') verify --print-certs $Out

New-Item -ItemType Directory -Force $Dist | Out-Null
Copy-Item $Out $Dist -Force
Write-Host "APK: $(Join-Path $Dist $OutName)"
