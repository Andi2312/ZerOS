<#
.SYNOPSIS
  Build ZerOSAbout.apk (ZerOS "About phone" app).

.DESCRIPTION
  Real Material Design 2 (com.google.android.material 1.x) built with Gradle.
  Needs a minimal Android SDK: platforms/android-34 and build-tools/34.0.0
  (see README). The release APK is built unsigned, then zipalign'ed and signed
  with the AOSP platform key.

  Gradle and aapt2 cannot work with Cyrillic paths, so the project is copied
  into an ASCII temp directory and built there.

.EXAMPLE
  .\build.ps1
  .\build.ps1 -Sdk D:\android -KeysDir D:\keys
#>
param(
    [string]$Sdk = 'C:\work\sdk',
    [string]$KeysDir = 'C:\work\settings_apk\keys',
    [string]$Out = ''
)

# Native tools (java, gradle, aapt2) write to stderr; with 'Stop' PowerShell
# turns that into a terminating error, so rely on exit codes instead.
$ErrorActionPreference = 'Continue'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Out) {
    $Out = Join-Path $here 'dist\ZerOSAbout.apk'
}
$Out = [IO.Path]::GetFullPath($Out)

if (-not $env:JAVA_HOME) {
    $settings = (& java -XshowSettings:properties -version 2>&1 | Out-String)
    $m = [regex]::Match($settings, 'java\.home = (.+)')
    if (-not $m.Success) { throw 'cannot locate java.home' }
    $env:JAVA_HOME = $m.Groups[1].Value.Trim()
}
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

$work = Join-Path ([IO.Path]::GetTempPath()) 'zerocabout-build'
if (Test-Path $work) { Remove-Item $work -Recurse -Force }
New-Item -ItemType Directory $work | Out-Null
foreach ($item in @('src', 'gradle', 'build.gradle.kts', 'settings.gradle.kts',
                    'gradle.properties', 'gradlew', 'gradlew.bat', 'proguard-rules.pro')) {
    Copy-Item (Join-Path $here $item) $work -Recurse -Force
}
"sdk.dir=$($Sdk.Replace('\', '\\'))" | Set-Content (Join-Path $work 'local.properties') -Encoding ascii

Push-Location $work
try {
    Write-Host 'gradle assembleRelease...'
    & (Join-Path $work 'gradlew.bat') --no-daemon --console=plain assembleRelease
    if ($LASTEXITCODE) { throw 'gradle build failed' }
}
finally {
    Pop-Location
}

$unsigned = Join-Path $work 'build\outputs\apk\release\ZerOSAbout-release-unsigned.apk'
if (-not (Test-Path $unsigned)) {
    $found = Get-ChildItem (Join-Path $work 'build\outputs\apk\release') -Filter '*-unsigned.apk' |
             Select-Object -First 1
    if (-not $found) { throw "release APK not found under $work" }
    $unsigned = $found.FullName
}

$buildTools = Get-ChildItem (Join-Path $Sdk 'build-tools') -Directory |
              Sort-Object Name -Descending | Select-Object -First 1
if (-not $buildTools) { throw "no build-tools in $Sdk" }
$zipalign = Join-Path $buildTools.FullName 'zipalign.exe'
$apksignerJar = Join-Path $buildTools.FullName 'lib\apksigner.jar'
$aapt2 = Join-Path $buildTools.FullName 'aapt2.exe'

$key = Join-Path $KeysDir 'platform.pk8'
$cert = Join-Path $KeysDir 'platform.x509.pem'
if (-not (Test-Path $key) -or -not (Test-Path $cert)) {
    throw "platform key not found in $KeysDir"
}

$aligned = Join-Path $work 'ZerOSAbout-aligned.apk'
Write-Host 'zipalign...'
& $zipalign -p -f 4 $unsigned $aligned
if ($LASTEXITCODE) { throw 'zipalign failed' }

Write-Host 'apksigner...'
$dist = Split-Path -Parent $Out
if (-not (Test-Path $dist)) { New-Item -ItemType Directory $dist | Out-Null }
& java -jar $apksignerJar sign --key $key --cert $cert `
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled false `
    --out $Out $aligned
if ($LASTEXITCODE) { throw 'apksigner failed' }

$verify = & java -jar $apksignerJar verify --print-certs $Out
if ($LASTEXITCODE) { throw 'signature verification failed' }
$verify | Select-Object -First 3

$badging = & $aapt2 dump badging $Out
$badging | Select-String -Pattern '^package:|^application-label:|^launchable-activity:' |
    ForEach-Object { $_.Line }
Write-Host ("OK: {0} ({1:N0} bytes)" -f $Out, (Get-Item $Out).Length)
exit 0
