# Builds a Windows desktop package for AttendIQ using jpackage.
# Produces either a Setup.exe installer (needs WiX) or a portable app-image folder.
param(
    [string]$Version = "1.0.0",
    [string]$ApiUrl = "https://ucc-attendance-system.onrender.com",
    [ValidateSet("exe", "app-image")]
    [string]$PackageType = "exe"
)

$ErrorActionPreference = "Stop"

$inputDir = Join-Path $PSScriptRoot "..\desktop\jpackage-input"
$installerDir = Join-Path $PSScriptRoot "..\desktop\installer"
$targetDir = Join-Path $PSScriptRoot "..\desktop\target"

Remove-Item -Recurse -Force $inputDir, $installerDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null

$jar = Get-ChildItem $targetDir -Filter "attendance-desktop-*.jar" |
    Where-Object { $_.Name -notmatch "^original-" } |
    Select-Object -First 1
if (-not $jar) {
    throw "Fat JAR not found under desktop/target. Run: mvn clean package -pl desktop -DskipTests"
}

Copy-Item $jar.FullName $inputDir
Write-Host "Using JAR: $($jar.Name)"

Push-Location (Join-Path $PSScriptRoot "..\desktop")
mvn -q dependency:copy-dependencies `
    "-DoutputDirectory=jpackage-input" `
    "-DincludeGroupIds=org.openjfx" `
    "-Dclassifier=win"
Pop-Location

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage not found. Install JDK 17+ (Temurin) and ensure bin is on PATH."
}

$commonArgs = @(
    "--input", $inputDir,
    "--dest", $installerDir,
    "--name", "AttendIQ",
    "--app-version", $Version,
    "--vendor", "UCC AttendIQ",
    "--description", "Lecturer attendance client for AttendIQ",
    "--main-jar", $jar.Name,
    "--main-class", "com.ucc.attendance.desktop.Launcher",
    "--java-options", "-Dapi.url=$ApiUrl"
)

if ($PackageType -eq "exe") {
    $commonArgs += @("--type", "exe", "--win-shortcut", "--win-menu", "--win-dir-chooser")
} else {
    $commonArgs += @("--type", "app-image")
}

Write-Host "Running: jpackage $($commonArgs -join ' ')"
& jpackage @commonArgs

Write-Host "`nPackage output:"
Get-ChildItem $installerDir -Recurse | Select-Object FullName
