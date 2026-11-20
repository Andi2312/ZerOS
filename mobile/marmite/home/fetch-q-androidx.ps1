# Download androidx AARs needed by AOSP Launcher3QuickStep (Android 10).
# Versions match the Android 10 platform tree (androidx 1.1.0 / 2.1.0).
# Each AAR is extracted to %TEMP%\opencode\q\aar\<name> (classes.jar + res).
$ErrorActionPreference = 'Stop'
$Out = Join-Path $env:TEMP 'opencode\q\aar'
New-Item -ItemType Directory -Force $Out | Out-Null

$libs = @(
    'annotation/annotation/1.1.0',
    'collection/collection/1.1.0',
    'core/core/1.1.0',
    'customview/customview/1.0.0',
    'dynamicanimation/dynamicanimation/1.0.0',
    'recyclerview/recyclerview/1.1.0',
    'preference/preference/1.1.0',
    'appcompat/appcompat/1.1.0',
    'appcompat/appcompat-resources/1.1.0',
    'fragment/fragment/1.1.0',
    'activity/activity/1.0.0',
    'slidingpanelayout/slidingpanelayout/1.0.0',
    'drawerlayout/drawerlayout/1.0.0',
    'viewpager/viewpager/1.0.0',
    'loader/loader/1.0.0',
    'cursoradapter/cursoradapter/1.0.0',
    'interpolator/interpolator/1.0.0',
    'documentfile/documentfile/1.0.0',
    'print/print/1.0.0',
    'localbroadcastmanager/localbroadcastmanager/1.0.0',
    'versionedparcelable/versionedparcelable/1.1.0',
    'vectordrawable/vectordrawable/1.1.0',
    'vectordrawable/vectordrawable-animated/1.1.0',
    'savedstate/savedstate/1.0.0',
    'lifecycle/lifecycle-common/2.1.0',
    'lifecycle/lifecycle-runtime/2.1.0',
    'lifecycle/lifecycle-viewmodel/2.1.0',
    'lifecycle/lifecycle-livedata/2.1.0',
    'lifecycle/lifecycle-livedata-core/2.1.0',
    'arch/core/core-common/2.1.0',
    'arch/core/core-runtime/2.1.0'
)

foreach ($l in $libs) {
    $p = $l.Split('/')
    $group = $p[0..($p.Count - 3)] -join '.'
    $artifact = $p[$p.Count - 2]
    $version = $p[$p.Count - 1]
    $name = "$artifact-$version"
    $dir = Join-Path $Out $name
    if (Test-Path (Join-Path $dir 'classes.jar')) { Write-Output "${name}: have"; continue }
    $url = "https://dl.google.com/dl/android/maven2/androidx/$($group -replace '\.', '/')/$artifact/$version/$name.aar"
    $aar = Join-Path $Out "$name.aar"
    try {
        if (-not (Test-Path $aar)) { Invoke-WebRequest -Uri $url -OutFile $aar -TimeoutSec 300 -UseBasicParsing }
        New-Item -ItemType Directory -Force $dir | Out-Null
        tar -xzf $aar -C $dir
        Write-Output "${name}: ok"
    } catch {
        Write-Output "${name}: FAIL $url"
    }
}
