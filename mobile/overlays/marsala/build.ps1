<#
    Сборка RRO-оверлея ZerOS (марсала для Monet на Android 12+).

    Собирается в ASCII-каталоге: путь репозитория может содержать кириллицу,
    aapt2 на ней спотыкается, поэтому исходники копируются в -WorkDir.

    Пример:
        .\build.ps1

    Нужны: aapt2, apksig.jar, framework-res.apk целевой прошивки и platform-ключ,
    которым подписана эта прошивка (у LineageOS a5xelte 20 — AOSP platform).
#>
[CmdletBinding()]
param(
    [string] $WorkDir      = 'C:\work\zeros\overlay-marsala',
    [string] $Aapt2        = 'C:\Users\SillyTech\.gradle\caches\9.4.1\transforms\6600be886e980e0f39557fe4b1a3a561\transformed\aapt2-9.2.1-15009934-windows\aapt2.exe',
    [string] $Apksig       = 'C:\Users\SillyTech\.gradle\caches\modules-2\files-2.1\com.android.tools.build\apksig\9.2.1\2881101f6a9d0baf6a341926ef0ff08c98a5d13b\apksig-9.2.1.jar',
    [string] $FrameworkRes = "$env:TEMP\opencode\a5\framework-res.apk",
    [string] $KeyDir       = 'C:\work\settings_apk\keys',
    [string] $JavaHome     = 'C:\Program Files\Java\jdk-21.0.12.1'
)

$ErrorActionPreference = 'Stop'
$src = $PSScriptRoot

foreach ($p in @($Aapt2, $Apksig, $FrameworkRes, "$KeyDir\platform.pk8", "$KeyDir\platform.x509.pem")) {
    if (-not (Test-Path $p)) { throw "не найдено: $p" }
}

New-Item -ItemType Directory -Force "$WorkDir\res\values", "$WorkDir\tools\classes" | Out-Null
Copy-Item "$src\AndroidManifest.xml" $WorkDir -Force
Copy-Item "$src\res\values\colors.xml" "$WorkDir\res\values" -Force
Copy-Item "$src\tools\SignApk.java" "$WorkDir\tools" -Force

Push-Location $WorkDir
try {
    & $Aapt2 compile --dir res -o res.zip
    if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile' }

    # -I framework-res.apk вместо android.jar: атрибуты android:* в нём уже есть.
    & $Aapt2 link -o unsigned.apk -I $FrameworkRes --manifest AndroidManifest.xml `
        --min-sdk-version 29 --target-sdk-version 29 res.zip
    if ($LASTEXITCODE -ne 0) { throw 'aapt2 link' }

    & "$JavaHome\bin\javac.exe" -cp $Apksig -d "$WorkDir\tools\classes" "$WorkDir\tools\SignApk.java"
    if ($LASTEXITCODE -ne 0) { throw 'javac' }

    & "$JavaHome\bin\java.exe" -cp "$Apksig;$WorkDir\tools\classes" SignApk `
        "$KeyDir\platform.pk8" "$KeyDir\platform.x509.pem" `
        "$WorkDir\unsigned.apk" "$WorkDir\ZerOS-marsala.apk" 29
    if ($LASTEXITCODE -ne 0) { throw 'sign' }
}
finally {
    Pop-Location
}

"готово: $WorkDir\ZerOS-marsala.apk"
