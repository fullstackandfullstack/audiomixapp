# How to Run the Android Audio Mixer Project

## Option 1: Using Android Studio (Recommended)

### Prerequisites
1. **Install Android Studio**
   - Download from: https://developer.android.com/studio
   - Install with default settings (includes Android SDK)

### Steps
1. **Open the Project**
   - Launch Android Studio
   - Click "Open" and select the `MixApp` folder
   - Wait for Gradle sync to complete

2. **Configure SDK** (if needed)
   - Go to `File` → `Project Structure` → `SDK Location`
   - Verify Android SDK location is set
   - If not set, browse to your SDK location (usually `C:\Users\YourName\AppData\Local\Android\Sdk`)

3. **Build the APK**
   - Go to `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Wait for build to complete
   - APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

4. **Run on Device/Emulator**
   - Connect Android device via USB (enable USB debugging)
   - Or create/start an Android emulator
   - Click the green "Run" button in Android Studio
   - Select your device/emulator

## Option 2: Command Line Build

### Prerequisites
1. **Android SDK** must be installed
   - Either via Android Studio installation
   - Or download command-line tools from: https://developer.android.com/studio#command-tools

2. **Set SDK Location**
   Create a file named `local.properties` in the project root with:
   ```
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   ```
   Replace `YourUsername` with your Windows username.

### Build Steps
1. **Open PowerShell/Terminal** in the project directory

2. **Build Debug APK**
   ```powershell
   .\gradlew.bat assembleDebug
   ```

3. **Install on Device**
   ```powershell
   .\gradlew.bat installDebug
   ```
   (Requires device connected via USB with USB debugging enabled)

4. **Find the APK**
   The built APK will be at: `app\build\outputs\apk\debug\app-debug.apk`

## Finding Your Android SDK Location

### Common Locations:
- **Windows**: `C:\Users\YourUsername\AppData\Local\Android\Sdk`
- **If installed via Android Studio**: Check `File` → `Settings` → `Appearance & Behavior` → `System Settings` → `Android SDK`

### To Find SDK Location:
1. Open Android Studio
2. Go to `File` → `Settings` (or `Preferences` on Mac)
3. Navigate to `Appearance & Behavior` → `System Settings` → `Android SDK`
4. The "Android SDK Location" shows your SDK path

## Troubleshooting

### "SDK location not found" Error
- Create `local.properties` file in project root
- Add: `sdk.dir=C\:\\Path\\To\\Your\\Android\\Sdk`
- Use double backslashes (`\\`) in the path

### "Gradle sync failed"
- Ensure Android SDK is installed
- Check that `local.properties` has correct SDK path
- Try: `File` → `Invalidate Caches / Restart` in Android Studio

### "Build failed" Errors
- Ensure you have Android SDK Platform 28-34 installed
- Check Android Studio SDK Manager: `Tools` → `SDK Manager`
- Install required SDK platforms and build tools

### Java Version Issues
- Project requires Java 8 or higher
- Gradle 8.5 supports Java 21 (current setup)
- If issues occur, try Java 17

## Quick Start (Android Studio)

1. Open Android Studio
2. `File` → `Open` → Select `MixApp` folder
3. Wait for sync
4. `Build` → `Build APK(s)`
5. Install APK on device or use `Run` button

## Testing the App

1. **Add Tracks**: Tap "Add Track" and select MP3 files
2. **Add Announcements**: Tap "Add Announcement" for voice-over clips
3. **Adjust Volumes**: Use sliders for main and announcement volumes
4. **Set Interval**: Configure how often announcements play
5. **Play**: Start mixing and playback

## Notes

- The app requires storage permissions (granted automatically on first file selection)
- Test with sample MP3 files to verify functionality
- Ensure device is running Android 9 (API 28) or higher

