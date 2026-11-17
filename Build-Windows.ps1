# Compile an installable Android debug APK. No unit or device tests are run.
# Requires a locally installed Android SDK and Java JDK 17+. Downloads Gradle if needed.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$gradleVersion = '8.11.1'
$gradleCmd = Get-Command gradle -ErrorAction SilentlyContinue
if ($null -ne $gradleCmd) {
    $gradleBin = $gradleCmd.Source
} else {
    $toolHome = Join-Path $PSScriptRoot '.tools'
    $zip = Join-Path $toolHome "gradle-$gradleVersion-bin.zip"
    $gradleBin = Join-Path $toolHome "gradle-$gradleVersion\bin\gradle.bat"
    if (-not (Test-Path $gradleBin)) {
        New-Item -ItemType Directory -Force -Path $toolHome | Out-Null
        Write-Host 'Downloading official Gradle distribution...'
        Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zip
        Expand-Archive -Path $zip -DestinationPath $toolHome -Force
        Remove-Item $zip -Force
    }
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java JDK 17+ is required. Install Android Studio JDK or Temurin JDK 17.'
}
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT -and -not (Test-Path 'local.properties')) {
    $defaultSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $defaultSdk) {
        $env:ANDROID_HOME = $defaultSdk
    } else {
        throw 'Android SDK not found. Install SDK API 35 + build-tools 35.0.0 or set ANDROID_HOME.'
    }
}
Write-Host 'Assembling debug APK, without tests...'
& $gradleBin --no-daemon --stacktrace assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
$apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { throw "Expected APK missing: $apk" }
$release = Join-Path $PSScriptRoot 'release'
New-Item -ItemType Directory -Force -Path $release | Out-Null
$out = Join-Path $release 'SignalScope-v0.1.0-debug.apk'
Copy-Item $apk $out -Force
$hash = (Get-FileHash $out -Algorithm SHA256).Hash.ToLowerInvariant()
"$hash  SignalScope-v0.1.0-debug.apk" | Set-Content (Join-Path $release 'SHA256SUMS.txt')
Write-Host "APK created: $out"
Write-Host 'IMPORTANT: This is an untested, debug-signed APK for development use only.'
