# PowerShell script to help set up Android SDK path
Write-Host "Android SDK Setup Helper" -ForegroundColor Green
Write-Host "========================" -ForegroundColor Green
Write-Host ""

# Check common SDK locations
$sdkPaths = @(
    "$env:LOCALAPPDATA\Android\Sdk",
    "$env:USERPROFILE\AppData\Local\Android\Sdk",
    "C:\Android\Sdk",
    "$env:ANDROID_HOME"
)

$foundSdk = $null
foreach ($path in $sdkPaths) {
    if ($path -and (Test-Path $path)) {
        $foundSdk = $path
        Write-Host "Found Android SDK at: $path" -ForegroundColor Green
        break
    }
}

if (-not $foundSdk) {
    Write-Host "Android SDK not found in common locations." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please provide your Android SDK path:" -ForegroundColor Cyan
    Write-Host "(Usually: C:\Users\YourName\AppData\Local\Android\Sdk)" -ForegroundColor Gray
    $sdkPath = Read-Host "Enter SDK path"
    
    if ($sdkPath -and (Test-Path $sdkPath)) {
        $foundSdk = $sdkPath
    } else {
        Write-Host "Path not found. Please install Android Studio or Android SDK." -ForegroundColor Red
        exit 1
    }
}

# Create local.properties file
$sdkDir = $foundSdk -replace '\\', '\\'
$content = "sdk.dir=$sdkDir`n"

Set-Content -Path "local.properties" -Value $content
Write-Host ""
Write-Host "Created local.properties file with SDK path!" -ForegroundColor Green
Write-Host "You can now build the project with: .\gradlew.bat assembleDebug" -ForegroundColor Cyan

