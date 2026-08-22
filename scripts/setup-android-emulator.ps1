#Requires -Version 5.1
<#
.SYNOPSIS
  Instala emulador Android e cria AVD local (opcional — Robolectric cobre gestos sem emulador).

  Pastas (relativas à raiz do repo):
    .android-sdk/   — SDK + emulator + system image
    .android-avd/   — AVDs (ANDROID_AVD_HOME)

.USAGE
  cd <repo-root>
  .\scripts\setup-android-emulator.ps1
  .\scripts\setup-android-emulator.ps1 -StartHeadless
#>
param(
    [switch]$StartHeadless,
    [string]$AvdName = "foss_headless_api36",
    [string]$SystemImage = "system-images;android-36;google_apis;x86_64"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

$Jdk = Join-Path $RepoRoot ".jdk"
$Sdk = Join-Path $RepoRoot ".android-sdk"
$AvdHome = Join-Path $RepoRoot ".android-avd"
$SdkManager = Join-Path $Sdk "cmdline-tools\latest\bin\sdkmanager.bat"
$AvdManager = Join-Path $Sdk "cmdline-tools\latest\bin\avdmanager.bat"
$Emulator = Join-Path $Sdk "emulator\emulator.exe"

$env:JAVA_HOME = $Jdk
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk
$env:ANDROID_AVD_HOME = $AvdHome

if (-not (Test-Path $SdkManager)) {
    Write-Error "sdkmanager não encontrado. Instale cmdline-tools em .android-sdk/ primeiro."
}

New-Item -ItemType Directory -Force -Path $AvdHome | Out-Null

Write-Host "Aceitando licenças SDK..."
cmd /c "echo y | `"$SdkManager`" --licenses" | Out-Null

Write-Host "Instalando emulator + system image em .android-sdk/ ..."
& $SdkManager "emulator" $SystemImage "platform-tools"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$AvdList = & $AvdManager list avd 2>&1 | Out-String
if ($AvdList -notmatch $AvdName) {
    Write-Host "Criando AVD $AvdName em .android-avd/ ..."
    cmd /c "echo no | `"$AvdManager`" create avd -n $AvdName -k `"$SystemImage`" -d pixel_6 --force"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host ""
Write-Host "OK Emulador: $Emulator"
Write-Host "OK AVD home: $AvdHome"
Write-Host "OK AVD name: $AvdName"
Write-Host ""
Write-Host "Headless: $Emulator -avd $AvdName -no-window -no-audio -gpu swiftshader_indirect"

if ($StartHeadless) {
    if (-not (Test-Path $Emulator)) {
        Write-Error "emulator.exe não encontrado em .android-sdk/emulator/"
    }
    & $Emulator -avd $AvdName -no-window -no-audio -gpu swiftshader_indirect
}
