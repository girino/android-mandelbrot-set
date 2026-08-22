#Requires -Version 5.1
<#
.SYNOPSIS
  Instala/atualiza Android SDK local (.android-sdk/) exigido pelo projeto.

  Pacotes: platform-tools, platforms;android-36, build-tools;36.0.0

.USAGE
  cd <repo-root>
  .\scripts\setup-android-sdk.ps1
#>
param(
    [int]$PlatformApi = 36,
    [string]$BuildTools = "36.0.0"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

$Jdk = Join-Path $RepoRoot ".jdk"
$Sdk = Join-Path $RepoRoot ".android-sdk"
$SdkManager = Join-Path $Sdk "cmdline-tools\latest\bin\sdkmanager.bat"

function Test-ToolPath($Label, $Path) {
    if (-not (Test-Path $Path)) {
        Write-Error "$Label ausente: $Path"
    }
    Write-Host "OK $Label -> $Path"
}

Test-ToolPath "JDK" (Join-Path $Jdk "bin\java.exe")
Test-ToolPath "sdkmanager" $SdkManager

$env:JAVA_HOME = $Jdk
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

if (-not (Test-Path "local.properties")) {
    "sdk.dir=.android-sdk" | Set-Content -Encoding utf8 "local.properties"
    Write-Host "Criado local.properties (sdk.dir=.android-sdk)"
}

Write-Host "Aceitando licenças SDK..."
cmd /c "echo y | `"$SdkManager`" --licenses" | Out-Null

$Packages = @(
    "platform-tools",
    "platforms;android-$PlatformApi",
    "build-tools;$BuildTools"
)

Write-Host "Instalando pacotes em .android-sdk/ ..."
& $SdkManager @Packages
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$Adb = Join-Path $Sdk "platform-tools\adb.exe"
Test-ToolPath "adb" $Adb

Write-Host ""
Write-Host "SDK local pronto."
Write-Host "  ANDROID_HOME=$Sdk"
Write-Host "  adb=$Adb"
