# Quick Start Guide

## To Run This Project:

### ✅ Step 1: Install Android Studio
Download and install from: https://developer.android.com/studio

### ✅ Step 2: Set Up SDK Path

**Option A - Automatic (if SDK found):**
```powershell
.\setup-sdk.ps1
```

**Option B - Manual:**
1. Find your Android SDK location (usually in Android Studio: `File` → `Settings` → `Android SDK`)
2. Create file `local.properties` in project root
3. Add this line (replace with your actual path):
   ```
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```

### ✅ Step 3: Build the Project

**Using Android Studio (Easiest):**
1. Open Android Studio
2. `File` → `Open` → Select this folder
3. Wait for Gradle sync
4. `Build` → `Build APK(s)`
5. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

**Using Command Line:**
```powershell
.\gradlew.bat assembleDebug
```

### ✅ Step 4: Install and Run

**Via Android Studio:**
- Click the green "Run" button
- Select your device/emulator

**Via Command Line:**
```powershell
.\gradlew.bat installDebug
```
(Requires device connected with USB debugging enabled)

**Manual Install:**
- Transfer `app-debug.apk` to your Android device
- Enable "Install from Unknown Sources"
- Tap the APK to install

## Current Status

✅ Project structure created
✅ Gradle wrapper configured (Gradle 8.5)
✅ Java 21 detected and compatible
❌ Android SDK path needed (create `local.properties`)

## Next Steps

1. **Install Android Studio** (if not already installed)
2. **Run setup script**: `.\setup-sdk.ps1`
3. **Build**: `.\gradlew.bat assembleDebug`
4. **Install APK** on your Android device

For detailed instructions, see `RUN_INSTRUCTIONS.md`

