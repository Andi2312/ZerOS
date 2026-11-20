# Assemble deps-q: everything the Gradle-free Android 10 Launcher3QuickStep build
# needs on top of the AOSP source tree.
#   sysui\shared-src, sysui\plugin-src  - SystemUISharedLib sources (same tag)
#   gen-aidl                            - java generated from its .aidl files
#   libs\*.jar, res\*                   - androidx 1.1.0/2.1.0 classes and resources
#   android-system-29.jar               - system SDK API 29 (hidden APIs for javac)
#   runtime\nano                        - protobuf-javanano runtime (reused from deps)
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Deps = Join-Path $Root 'deps-q'
$Q = Join-Path $env:TEMP 'opencode\q'
$Aid = 'C:\work\aidlq'
$Tag = 'android-10.0.0_r47'

New-Item -ItemType Directory -Force $Deps,"$Deps\libs","$Deps\sysui","$Deps\runtime" | Out-Null

# SystemUI shared + plugin sources from the same AOSP tag. The repository
# already carries the patched copies, so a fresh (unpatched) download is only
# taken when fetch-q-deps.ps1 actually produced it.
if (Test-Path "$Q\sysui-shared\src") {
    foreach ($p in @(@{from = "$Q\sysui-shared\src"; to = "$Deps\sysui\shared-src" },
                     @{from = "$Q\sysui-plugin\src"; to = "$Deps\sysui\plugin-src" },
                     @{from = "$Q\sysui-plugin-core\src"; to = "$Deps\sysui\plugin-core-src" })) {
        if (Test-Path $p.to) { Remove-Item $p.to -Recurse -Force }
        robocopy $p.from $p.to /E /NFL /NDL /NJH /NJS | Out-Null
    }
}

# protobuf-javanano runtime is the same as for the P build
if (-not (Test-Path "$Deps\runtime\nano")) {
    robocopy (Join-Path $Root 'deps\runtime\nano') "$Deps\runtime\nano" /E /NFL /NDL /NJH /NJS | Out-Null
}

# PluginManagerImpl calls the hidden libcore API Thread.setUncaughtExceptionPreHandler,
# which a plain JDK does not declare; route it through compat-src\UncaughtHandlerCompat.
$pmi = "$Deps\sysui\shared-src\com\android\systemui\shared\plugins\PluginManagerImpl.java"
if (Test-Path $pmi) {
    $t = [IO.File]::ReadAllText($pmi)
    $t = $t.Replace('Thread.getUncaughtExceptionPreHandler(), initializer);',
                    'UncaughtHandlerCompat.getPreHandler(), initializer);')
    $t = $t.Replace('Thread.setUncaughtExceptionPreHandler(uncaughtExceptionHandler);',
                    'UncaughtHandlerCompat.setPreHandler(uncaughtExceptionHandler);')
    [IO.File]::WriteAllText($pmi, $t, (New-Object Text.UTF8Encoding($false)))
}

# SystemGestureExclusionListenerCompat: LineageOS 17.1 calls the three argument
# form of the binder callback; the API 29 source only implements the two argument
# one, so add it (no @Override: the interface compiled against lacks it).
$sge = "$Deps\sysui\shared-src\com\android\systemui\shared\system\SystemGestureExclusionListenerCompat.java"
if (Test-Path $sge) {
    $t = [IO.File]::ReadAllText($sge) -replace "`r`n", "`n"
    if ($t -notmatch 'systemGestureExclusionUnrestricted') {
        $marker = "`n            };`n    private boolean mRegistered;"
        $add = "`n                // Android 10 as shipped by LineageOS 17.1 also calls the three" +
               "`n                // argument form; the interface compiled against only declares the two" +
               "`n                // argument one, so this method is not marked @Override." +
               "`n                public void onSystemGestureExclusionChanged(int displayId," +
               "`n                        Region systemGestureExclusion, Region systemGestureExclusionUnrestricted) {" +
               "`n                    if (displayId == mDisplayId) {" +
               "`n                        onExclusionChanged(systemGestureExclusion);" +
               "`n                    }" +
               "`n                }`n"
        $t = $t.Replace($marker, $add + "            };`n    private boolean mRegistered;")
        [IO.File]::WriteAllText($sge, $t, (New-Object Text.UTF8Encoding($false)))
    }
}

# Launcher stats proto (frameworks/base/core/proto/android/stats/launcher).
$statsProto = "$Deps\proto\launcher.proto"
if (-not (Test-Path $statsProto)) {
    New-Item -ItemType Directory -Force (Split-Path -Parent $statsProto) | Out-Null
    $u = 'https://android.googlesource.com/platform/frameworks/base/+/refs/tags/' +
         "$Tag/core/proto/android/stats/launcher/launcher.proto?format=TEXT"
    $b64 = Invoke-WebRequest -Uri $u -TimeoutSec 300 -UseBasicParsing
    $txt = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(($b64.Content -replace '\s', '')))
    [IO.File]::WriteAllText($statsProto, $txt, (New-Object Text.UTF8Encoding($false)))
}

# androidx: classes.jar + res/ from the extracted AARs
$aar = "$Q\aar"
if (-not (Test-Path $aar)) {
    throw "androidx AARs not found in $aar - run fetch-q-androidx.ps1 first"
}
foreach ($d in (Get-ChildItem $aar -Directory)) {
    $jar = Join-Path $d.FullName 'classes.jar'
    if (Test-Path $jar) { Copy-Item $jar (Join-Path "$Deps\libs" ($d.Name + '.jar')) -Force }
    if (Test-Path (Join-Path $d.FullName 'res')) {
        $to = Join-Path "$Deps\res" $d.Name
        if (Test-Path $to) { Remove-Item $to -Recurse -Force }
        robocopy (Join-Path $d.FullName 'res') $to /E /NFL /NDL /NJH /NJS | Out-Null
    }
}

# AIDL -> java. Generated in C:\work on purpose: the SDK aidl.exe mangles the
# %TEMP% path (it fails to find its own include directory there). The generated
# files are committed, so this only runs when they are missing.
if (-not (Test-Path "$Deps\gen-aidl")) {
    if (-not (Test-Path "$Q\sysui-shared\src")) {
        throw "deps-q\gen-aidl is missing and $Q\sysui-shared is absent - run fetch-q-deps.ps1 first"
    }
    if (-not (Test-Path "$Aid\out")) {
        $aidlExe = Get-ChildItem 'G:\Android-sdk\build-tools' -Directory |
            Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName 'aidl.exe' } |
            Where-Object { Test-Path $_ } | Select-Object -First 1
        if (-not $aidlExe) { throw 'aidl.exe not found in G:\Android-sdk\build-tools' }

        New-Item -ItemType Directory -Force "$Aid\src\com\android\systemui\shared\recents",
            "$Aid\out", "$Aid\stub\android\graphics", "$Aid\stub\android\os", "$Aid\stub\android\view" | Out-Null
        Copy-Item "$Q\sysui-shared\src\com\android\systemui\shared\recents\*.aidl" `
            "$Aid\src\com\android\systemui\shared\recents\"

        # parcelable stubs for the framework classes the interfaces import
        foreach ($c in @(@{pkg = 'android.graphics'; cls = 'Rect' },
                         @{pkg = 'android.graphics'; cls = 'Region' },
                         @{pkg = 'android.os'; cls = 'Bundle' },
                         @{pkg = 'android.view'; cls = 'MotionEvent' })) {
            $p = Join-Path "$Aid\stub" ($c.pkg -replace '\.', '\')
            New-Item -ItemType Directory -Force $p | Out-Null
            [IO.File]::WriteAllText((Join-Path $p ($c.cls + '.aidl')),
                "package $($c.pkg);`nparcelable $($c.cls);`n",
                (New-Object Text.UTF8Encoding($false)))
        }

        foreach ($f in (Get-ChildItem "$Aid\src" -Recurse -Filter *.aidl)) {
            & $aidlExe "-o$Aid\out" "-I$Aid\src" "-I$Aid\stub" $f.FullName
            if ($LASTEXITCODE -ne 0) { throw "aidl failed: $($f.Name)" }
        }
    }

    robocopy "$Aid\out" "$Deps\gen-aidl" /E /NFL /NDL /NJH /NJS | Out-Null
}

# system SDK API 29 for javac (hidden APIs used by quickstep and SystemUISharedLib)
$sysJar = Join-Path $Deps 'android-system-29.jar'
if (-not (Test-Path $sysJar)) {
    $u = 'https://android.googlesource.com/platform/prebuilts/sdk/+/refs/heads/android10-release/29/system/android.jar?format=TEXT'
    $b64 = Join-Path $Q 'android-system-29.b64'
    if (-not (Test-Path $b64)) { Invoke-WebRequest -Uri $u -OutFile $b64 -TimeoutSec 600 -UseBasicParsing }
    [IO.File]::WriteAllBytes($sysJar, [Convert]::FromBase64String((Get-Content $b64 -Raw)))
}

"libs:  $((Get-ChildItem "$Deps\libs" -Filter *.jar).Count) jars"
"res:   $((Get-ChildItem "$Deps\res" -Directory).Count) dirs"
"aidl:  $((Get-ChildItem "$Deps\gen-aidl" -Recurse -Filter *.java -ErrorAction SilentlyContinue).Count) files"
"sysui: $((Get-ChildItem "$Deps\sysui" -Recurse -Filter *.java).Count) files"
"sysjar: $((Get-Item $sysJar -ErrorAction SilentlyContinue).Length) bytes"
