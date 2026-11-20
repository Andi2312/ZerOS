# Download the AOSP parts that are not in the Launcher3 source drop
# (android-10.0.0_r47) so that Launcher3QuickStep can be built without an AOSP
# tree:
#   - SystemUISharedLib (packages/SystemUI/shared), same release as Launcher3;
#   - plugin + plugin_core (com.android.systemui.plugins interfaces);
#   - launcher stats proto (fetched by fetch-q-deps2.ps1).
# Result: %TEMP%\opencode\q, consumed by fetch-q-deps2.ps1.
$ErrorActionPreference = 'Stop'
$Tag = 'android-10.0.0_r47'
$Out = Join-Path $env:TEMP 'opencode\q'
New-Item -ItemType Directory -Force $Out | Out-Null

function Get-Archive($path, $dest) {
    $u = "https://android.googlesource.com/platform/frameworks/base/+archive/refs/tags/$Tag/$path.tar.gz"
    $f = Join-Path $Out (($path -replace '/', '_') + '.tar.gz')
    if (-not (Test-Path $f)) {
        Invoke-WebRequest -Uri $u -OutFile $f -TimeoutSec 600 -UseBasicParsing
    }
    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
    New-Item -ItemType Directory -Force $dest | Out-Null
    tar -xzf $f -C $dest
    "$path -> $dest ($((Get-ChildItem $dest -Recurse -File).Count) files)"
}

Get-Archive 'packages/SystemUI/shared' (Join-Path $Out 'sysui-shared')
Get-Archive 'packages/SystemUI/plugin' (Join-Path $Out 'sysui-plugin')
Get-Archive 'packages/SystemUI/plugin_core' (Join-Path $Out 'sysui-plugin-core')
