# Build the stock AOSP Launcher3, flavor Launcher3QuickStep, from the Android 10
# source drop (android-10.0.0_r47) without Gradle or an AOSP tree.
#
# This is the variant that matches a phone running Android 10: the recents
# (QuickStep) code has to be compiled against the same framework release as the
# system, otherwise it dies on APIs that changed between releases.
#
# Modules reproduced (see launcher3-q\Android.mk):
#   Launcher3CommonDepsLib  -> protos, src_build_config, iconloaderlib, res
#   Launcher3QuickStepLib   -> src, quickstep/src, quickstep/recents_ui_overrides/src,
#                              src_flags, src_shortcuts_overrides, quickstep res
#   Launcher3QuickStep      -> package with quickstep/AndroidManifest.xml
# plus SystemUISharedLib (com.android.systemui.shared.*) from the same tag, the
# plugin interfaces it needs, and androidx 1.1.0 libraries.
#
# Needs: JDK 21, aapt2, android.jar (resources), deps-q\android-system-29.jar
# (hidden APIs for javac), r8.jar, zipalign, apksigner, python, deps-q\ filled
# by fetch-q-deps.ps1 / fetch-q-androidx.ps1 / fetch-q-deps2.ps1.
#
# Everything is built under C:\work (ASCII path): the SDK aidl.exe and aapt2
# do not cope with the Cyrillic project path.

param(
    [string]$Work = 'C:\work\aospl3q'
)

$ErrorActionPreference = 'Stop'

$Root  = Split-Path -Parent $MyInvocation.MyCommand.Path
$Proj  = Join-Path $Root 'launcher3-q'
$Deps  = Join-Path $Root 'deps-q'
$Dist  = Join-Path $Root 'dist'
$Jdk   = 'C:\Program Files\Java\jdk-21.0.12.1'
if (-not (Test-Path (Join-Path $Jdk 'bin\javac.exe')) -and $env:JAVA_HOME) {
    $Jdk = $env:JAVA_HOME
}
$Javac = Join-Path $Jdk 'bin\javac.exe'
$Java  = Join-Path $Jdk 'bin\java.exe'
$Jar   = Join-Path $Jdk 'bin\jar.exe'
$Keytool = Join-Path $Jdk 'bin\keytool.exe'

$AndroidJar = @(
    'G:\Android-sdk\platforms\android-36.1\android.jar',
    (Join-Path $env:TEMP 'opencode\android\platform-33\android-13\android.jar')
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $AndroidJar) { throw 'android.jar not found' }

# javac needs the whole framework (SystemUISharedLib uses com.android.internal.*
# and android.view.IRotationWatcher, which the system SDK does not export), so
# the full API 29 framework classes from Robolectric's android-all (the AOSP
# framework compiled for the JVM) are used for compilation only.
$CompileJar = Join-Path $Deps 'android-all-10.jar'
if (-not (Test-Path $CompileJar)) { $CompileJar = Join-Path $Deps 'android-system-29.jar' }
if (-not (Test-Path $CompileJar)) { throw "missing $CompileJar (run fetch-q-deps2.ps1)" }

$R8    = Join-Path $env:TEMP 'opencode\android\r8.jar'
$Tools = 'C:\Program Files (x86)\APK Editor Studio\tools'
foreach ($tool in @($Javac, $R8, (Join-Path $Tools 'zipalign.exe'), (Join-Path $Tools 'apksigner.jar'),
                    (Join-Path $Root 'deps\tools\protoc.exe'))) {
    if (-not (Test-Path $tool)) { throw "missing: $tool" }
}

$Aapt2 = 'C:\Users\SillyTech\.gradle\caches\9.4.1\transforms\467bdda5c6a853f23b3335dfdc88698e\transformed\aapt2-9.2.1-15009934-windows\aapt2.exe'
if (-not (Test-Path $Aapt2)) {
    $found = Get-ChildItem (Join-Path $env:USERPROFILE '.gradle\caches') -Recurse -Filter aapt2.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $Aapt2 = $found.FullName }
}
if (-not $Aapt2 -or -not (Test-Path $Aapt2)) { throw 'aapt2.exe not found in gradle caches' }

$MinSdk      = 26
$TargetSdk   = 29
$VersionCode = 1
$VersionName = '10.0.0-r47'
$OutName     = 'Launcher3QuickStep-r47.apk'
$Manifest    = Join-Path $Deps 'AndroidManifest-quickstep.xml'

# 1. clean copy outside the Cyrillic path
if (Test-Path $Work) { Remove-Item $Work -Recurse -Force }
New-Item -ItemType Directory -Force $Work | Out-Null
robocopy $Proj $Work /E /XD tests robolectric_tests go SecondaryDisplayLauncher /NFL /NDL /NJH /NJS | Out-Null
$Build = Join-Path $Work 'build'
New-Item -ItemType Directory -Force $Build | Out-Null
Copy-Item $Manifest (Join-Path $Work 'AndroidManifest-merged.xml') -Force
foreach ($x in @('libs', 'res', 'gen-aidl')) {
    robocopy (Join-Path $Deps $x) (Join-Path $Work "deps-$x") /E /NFL /NDL /NJH /NJS | Out-Null
}
robocopy (Join-Path $Deps 'sysui\shared-src') (Join-Path $Work 'sysui-shared') /E /NFL /NDL /NJH /NJS | Out-Null
robocopy (Join-Path $Deps 'sysui\plugin-src') (Join-Path $Work 'sysui-plugin') /E /NFL /NDL /NJH /NJS | Out-Null
robocopy (Join-Path $Deps 'sysui\plugin-core-src') (Join-Path $Work 'sysui-plugin-core') /E /NFL /NDL /NJH /NJS | Out-Null
$Nano = Join-Path $Deps 'runtime\nano'
if (-not (Test-Path $Nano)) { $Nano = Join-Path $Root 'deps\runtime\nano' }
robocopy $Nano (Join-Path $Work 'runtime-nano') /E /NFL /NDL /NJH /NJS | Out-Null
robocopy (Join-Path $Deps 'stats-src') (Join-Path $Work 'stats-src') /E /NFL /NDL /NJH /NJS | Out-Null
robocopy (Join-Path $Deps 'compat-src') (Join-Path $Work 'sysui-compat') /E /NFL /NDL /NJH /NJS | Out-Null
robocopy (Join-Path $Deps 'proto') (Join-Path $Work 'stats-proto') /E /NFL /NDL /NJH /NJS | Out-Null
Copy-Item (Join-Path $Root 'deps\add_dex.py') (Join-Path $Work 'add_dex.py') -Force
$utf8 = New-Object System.Text.UTF8Encoding($false)

# 2. protobuf: nano sources for the dump / user-event protocols
$GenProto = Join-Path $Build 'gen-proto'
New-Item -ItemType Directory -Force $GenProto | Out-Null
& (Join-Path $Root 'deps\tools\protoc.exe') `
    "--proto_path=$(Join-Path $Work 'protos')" `
    "--proto_path=$(Join-Path $Work 'proto_overrides')" `
    "--proto_path=$(Join-Path $Work 'stats-proto')" `
    "--javanano_out=enum_style=java:$GenProto" `
    (Join-Path $Work 'protos\launcher_dump.proto') `
    (Join-Path $Work 'protos\launcher_log.proto') `
    (Join-Path $Work 'stats-proto\launcher.proto') `
    (Join-Path $Work 'proto_overrides\launcher_log_extension.proto')
if ($LASTEXITCODE -ne 0) { throw 'protoc failed' }

# 3. resources. Precedence low -> high: androidx libs, iconloaderlib, launcher res,
# quickstep res, recents_ui_overrides res. aapt2 accepts conflicts only between
# -R overlays ("the last conflicting resource takes precedence"), and the
# androidx libraries define some of the same resources, so every resource dir is
# passed as an overlay in priority order.
$aapt2Order = @()
foreach ($d in (Get-ChildItem (Join-Path $Work 'deps-res') -Directory | Sort-Object Name)) {
    if ($d.Name -like 'appcompat-*') { continue }
    $aapt2Order += $d.FullName
}
$aapt2Order += (Join-Path $Work 'deps-res\appcompat-resources-1.1.0')
$aapt2Order += (Join-Path $Work 'deps-res\appcompat-1.1.0')
$aapt2Order += (Join-Path $Work 'iconloaderlib\res')
$aapt2Order += (Join-Path $Work 'res')
$aapt2Order += (Join-Path $Work 'quickstep\res')
$aapt2Order += (Join-Path $Work 'quickstep\recents_ui_overrides\res')

$OverlayArgs = @()
$i = 0
foreach ($dir in $aapt2Order) {
    if (-not (Test-Path $dir)) { continue }
    $i++
    $out = Join-Path $Build ("res-$i.zip")
    & $Aapt2 compile --dir $dir -o $out
    if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed: $dir" }
    $OverlayArgs += @('-R', $out)
}

& $Aapt2 link -o (Join-Path $Build 'base.apk') `
    -I $AndroidJar `
    --manifest (Join-Path $Work 'AndroidManifest-merged.xml') `
    --java (Join-Path $Build 'gen') `
    --min-sdk-version $MinSdk --target-sdk-version $TargetSdk `
    --version-code $VersionCode --version-name $VersionName `
    --auto-add-overlay `
    --extra-packages androidx.appcompat:androidx.appcompat.resources:androidx.core:androidx.preference:androidx.recyclerview:com.android.launcher3.icons `
    $OverlayArgs
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

# 4. java -> bytecode
$Classes = Join-Path $Build 'classes'
New-Item -ItemType Directory -Force $Classes | Out-Null
$SourceDirs = @('src', 'src_flags', 'src_shortcuts_overrides', 'src_build_config',
                'iconloaderlib\src', 'iconloaderlib\src_full_lib', 'src_plugins',
                'quickstep\src', 'quickstep\recents_ui_overrides\src',
                'sysui-shared', 'sysui-plugin', 'sysui-plugin-core', 'deps-gen-aidl', 'runtime-nano',
                'stats-src', 'sysui-compat')
$Sources = @()
foreach ($dir in $SourceDirs) {
    $full = Join-Path $Work $dir
    if (Test-Path $full) { $Sources += @(Get-ChildItem $full -Recurse -Filter *.java | ForEach-Object FullName) }
}
$Sources += @(Get-ChildItem $GenProto -Recurse -Filter *.java | ForEach-Object FullName)
$Sources += @(Get-ChildItem (Join-Path $Build 'gen') -Recurse -Filter *.java | ForEach-Object FullName)

$LibJars = @(Get-ChildItem (Join-Path $Work 'deps-libs') -Filter *.jar | ForEach-Object FullName)
$ClassPath = ($LibJars -join ';')

# javac on Windows: hundreds of source files blow past the 32k command-line limit
$ArgFile = Join-Path $Build 'sources.txt'
$argLines = $Sources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }
[System.IO.File]::WriteAllLines($ArgFile, $argLines, $utf8)

& $Javac --release 8 -Xlint:-options -nowarn -classpath "$CompileJar;$ClassPath" -d $Classes "@$ArgFile"
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

# 5. bytecode -> dex
$ClassesJar = Join-Path $Build 'classes.jar'
& $Jar cf $ClassesJar -C $Classes .
$Dex = Join-Path $Build 'dex'
New-Item -ItemType Directory -Force $Dex | Out-Null
$DexInput = @($ClassesJar) + @($LibJars | Where-Object { $_ -notlike '*annotation*' })
$LibArgs = @('--lib', $AndroidJar, '--lib', $CompileJar)
& $Java -cp $R8 com.android.tools.r8.D8 --min-api $MinSdk @LibArgs --output $Dex $DexInput
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

# 6. classes.dex into the apk
$Unsigned = Join-Path $Build 'unsigned.apk'
& python (Join-Path $Work 'add_dex.py') (Join-Path $Build 'base.apk') (Join-Path $Dex 'classes.dex') $Unsigned
if ($LASTEXITCODE -ne 0) { throw 'add_dex failed' }

# 7. align + sign
$Aligned = Join-Path $Build 'aligned.apk'
& (Join-Path $Tools 'zipalign.exe') -f 4 $Unsigned $Aligned

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

& $Java -jar (Join-Path $Tools 'apksigner.jar') verify --print-certs $Out

New-Item -ItemType Directory -Force $Dist | Out-Null
Copy-Item $Out $Dist -Force

Write-Host ''
Write-Host "APK: $(Join-Path $Dist $OutName)"
exit 0
