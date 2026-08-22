#Requires -Version 5.1
<#
.SYNOPSIS
  Verifica JDK/SDK locais e roda testes headless (Robolectric, sem emulador).

.USAGE
  cd <repo-root>
  .\scripts\setup-headless-tests.ps1
  .\scripts\setup-headless-tests.ps1 -RunTests
#>
param(
    [switch]$RunTests
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

$Jdk = Join-Path $RepoRoot ".jdk"
$Sdk = Join-Path $RepoRoot ".android-sdk"

function Test-ToolPath($Label, $Path) {
    if (-not (Test-Path $Path)) {
        Write-Error "$Label ausente: $Path"
    }
    Write-Host "OK $Label -> $Path"
}

Test-ToolPath "JDK" (Join-Path $Jdk "bin\java.exe")
Test-ToolPath "sdkmanager" (Join-Path $Sdk "cmdline-tools\latest\bin\sdkmanager.bat")

$AdbPath = Join-Path $Sdk "platform-tools\adb.exe"
if (-not (Test-Path $AdbPath)) {
    Write-Host "platform-tools ausente - instalando SDK local..."
    & (Join-Path $RepoRoot "scripts\setup-android-sdk.ps1")
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
Test-ToolPath "adb" $AdbPath

$env:JAVA_HOME = $Jdk
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

if (-not (Test-Path "local.properties")) {
    "sdk.dir=.android-sdk" | Set-Content -Encoding utf8 "local.properties"
    Write-Host "Criado local.properties (sdk.dir=.android-sdk)"
}

Write-Host ""
Write-Host "Testes headless de gesto (pinch+drag): Robolectric em src/test - sem emulador."
Write-Host "Comando: .\gradlew.bat testDebugUnitTest --tests org.girino.frac.android.foss.MandelbrotViewGestureTest"

if ($RunTests) {
    Write-Host ""
    .\gradlew.bat testDebugUnitTest --no-daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "Testes concluidos."
}
