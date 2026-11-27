<#
.SYNOPSIS
  Build ZerOSAbout.apk (ZerOS "About phone" app) without Gradle.

.DESCRIPTION
  Uses a minimal Android SDK: platform android.jar + build-tools
  (aapt2, d8, zipalign, apksigner). Builds in an ASCII temp path because
  aapt2/apksigner cannot open Cyrillic paths.

  Downloads (once):
    https://dl.google.com/android/repository/platform-29_r05.zip
    https://dl.google.com/android/repository/build-tools_r29.0.3-windows.zip
  Extract both into -Sdk (default C:\work\sdk); the script finds the tools.

.EXAMPLE
  .\build.ps1
  .\build.ps1 -Sdk D:\android -KeysDir D:\keys
#>
param(
    [string]$Sdk = 'C:\work\sdk',
    [string]$KeysDir = 'C:\work\settings_apk\keys',
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Out) {
    $Out = Join-Path $here 'dist\ZerOSAbout.apk'
}
$Out = [IO.Path]::GetFullPath($Out)
$sevenZip = 'C:\Program Files (x86)\AOMEI Partition Assistant\7z.exe'

function Find-Tool([string]$root, [string]$name) {
    $f = Get-ChildItem $root -Recurse -Filter $name -ErrorAction SilentlyContinue |
         Select-Object -First 1 -ExpandProperty FullName
    if (-not $f) { throw "not found: $name under $root" }
    return $f
}

$androidJar = Find-Tool $Sdk 'android.jar'
$aapt2 = Find-Tool $Sdk 'aapt2.exe'
$dexJar = Find-Tool $Sdk 'dx.jar'
$zipalign = Find-Tool $Sdk 'zipalign.exe'
$apksignerJar = Find-Tool $Sdk 'apksigner.jar'
$java = 'java'

$key = Join-Path $KeysDir 'platform.pk8'
$cert = Join-Path $KeysDir 'platform.x509.pem'
if (-not (Test-Path $key) -or -not (Test-Path $cert)) {
    throw "platform key not found in $KeysDir"
}

$work = Join-Path ([IO.Path]::GetTempPath()) ('zerocabout-' + [Guid]::NewGuid().ToString('N'))
$app = Join-Path $work 'app'
$gen = Join-Path $work 'gen'
$classes = Join-Path $work 'classes'
$dex = Join-Path $work 'dex'
$res = Join-Path $work 'res.zip'
$base = Join-Path $work 'base.apk'
$unsigned = Join-Path $work 'unsigned.apk'
$aligned = Join-Path $work 'aligned.apk'
New-Item -ItemType Directory $app, $gen, $classes, $dex | Out-Null

# aapt2/apksigner cannot open Cyrillic paths, so build from an ASCII copy.
Copy-Item (Join-Path $here 'res') $app -Recurse
Copy-Item (Join-Path $here 'src') $app -Recurse
Copy-Item (Join-Path $here 'AndroidManifest.xml') $app

try {
    Write-Host 'aapt2 compile...'
    & $aapt2 compile --dir (Join-Path $app 'res') -o $res
    if ($LASTEXITCODE) { throw 'aapt2 compile failed' }

    Write-Host 'aapt2 link...'
    & $aapt2 link -o $base -I $androidJar `
        --manifest (Join-Path $app 'AndroidManifest.xml') `
        --java $gen `
        --min-sdk-version 29 --target-sdk-version 29 `
        --version-code 1 --version-name 1.0 `
        $res
    if ($LASTEXITCODE) { throw 'aapt2 link failed' }

    $sources = Get-ChildItem (Join-Path $app 'src') -Recurse -Filter *.java |
               Select-Object -ExpandProperty FullName
    $rjava = Get-ChildItem $gen -Recurse -Filter R.java |
             Select-Object -ExpandProperty FullName

    Write-Host 'javac...'
    & javac -encoding UTF-8 -source 8 -target 8 -bootclasspath $androidJar `
        -d $classes $sources $rjava
    if ($LASTEXITCODE) { throw 'javac failed' }

    Write-Host 'dx (dex)...'
    $classFiles = Get-ChildItem $classes -Recurse -Filter *.class |
                  Select-Object -ExpandProperty FullName
    $dexFile = Join-Path $dex 'classes.dex'
    Push-Location $classes
    try {
        $relative = $classFiles | ForEach-Object {
            $_.Substring($classes.Length + 1).Replace('\', '/')
        }
        & $java -cp $dexJar com.android.dx.command.Main --dex `
            --min-sdk-version=29 ("--output=$dexFile") $relative
        if ($LASTEXITCODE) { throw 'dx failed' }
    }
    finally { Pop-Location }

    Write-Host 'pack classes.dex...'
    Copy-Item $base $unsigned
    Push-Location $dex
    try {
        & $sevenZip a -tzip $unsigned 'classes.dex' | Out-Null
        if ($LASTEXITCODE) { throw 'cannot add classes.dex' }
    }
    finally { Pop-Location }

    Write-Host 'zipalign...'
    & $zipalign -p -f 4 $unsigned $aligned
    if ($LASTEXITCODE) { throw 'zipalign failed' }

    Write-Host 'apksigner...'
    $dist = Split-Path -Parent $Out
    if (-not (Test-Path $dist)) { New-Item -ItemType Directory $dist | Out-Null }
    & $java -jar $apksignerJar sign --key $key --cert $cert `
        --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled false `
        --out $Out $aligned
    if ($LASTEXITCODE) { throw 'apksigner failed' }

    $verify = & $java -jar $apksignerJar verify --print-certs $Out
    if ($LASTEXITCODE) { throw 'signature verification failed' }
    $verify | Select-Object -First 3

    $badging = & $aapt2 dump badging $Out
    $badging | Select-String -Pattern "^package:|^application-label:|^launchable-activity:" |
        ForEach-Object { $_.Line }
    Write-Host ("OK: {0} ({1:N0} bytes)" -f $Out, (Get-Item $Out).Length)
}
finally {
    Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}

