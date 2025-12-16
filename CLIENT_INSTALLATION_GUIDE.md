# Audio Mixer App - Installation Guide for Client

## APK Information
- **App Name**: Audio Mixer
- **Package**: com.mixapp
- **Version**: 1.0 (Debug Build)
- **Minimum Android**: Android 9 (API 28)
- **Target Android**: Android 14 (API 34)

## Installation Instructions

### Step 1: Enable Unknown Sources
1. On your Android device, go to **Settings**
2. Navigate to **Security** (or **Apps** → **Special app access**)
3. Find **Install unknown apps** (or **Install apps from unknown sources**)
4. Select your file manager/browser and enable **Allow from this source**

**Note**: On Android 8.0+, you need to enable this for the specific app you'll use to install the APK.

### Step 2: Transfer APK to Device
**Option A - Direct Transfer:**
- Connect device to computer via USB
- Copy `app-debug.apk` to device storage
- Open file manager on device and tap the APK

**Option B - Email/Cloud:**
- Email the APK to yourself
- Open email on device and download attachment
- Tap the downloaded APK to install

**Option C - Cloud Storage:**
- Upload APK to Google Drive, Dropbox, etc.
- Download on device
- Tap to install

### Step 3: Install the APK
1. Open the APK file using your file manager
2. Tap **Install** when prompted
3. If you see a security warning, tap **Install anyway** or **OK**
4. Wait for installation to complete
5. Tap **Open** to launch the app

## First Launch

1. **Grant Permissions**: The app will request storage permissions
   - Tap **Allow** when prompted
   - This is needed to select MP3 files

2. **Add Audio Files**:
   - Tap **"Add Track"** to load main music tracks
   - Tap **"Add Announcement"** to load voice-over clips
   - Select MP3 files from your device storage

3. **Test Features**:
   - Use **Play/Pause/Stop** buttons
   - Adjust **Main Volume** and **Announcement Volume** sliders
   - Set **Announcement Interval** (in seconds)
   - Enable **"Play at End Only"** if needed

## Features to Test

✅ **Multi-track Mixing**: Load multiple MP3 files and mix them
✅ **Real-time Volume Control**: Adjust volumes while playing
✅ **Announcement System**: 
   - Play at fixed intervals
   - Play only at the end
   - Separate volume control for announcements
✅ **Smooth Playback**: Gap-free audio mixing

## Troubleshooting

### "App not installed" Error
- Ensure "Unknown Sources" is enabled
- Check if device has enough storage space
- Verify Android version is 9 or higher

### "Parse Error"
- APK file may be corrupted - re-download
- Ensure complete download before installing

### Can't Find MP3 Files
- Grant storage permissions in Settings → Apps → Audio Mixer → Permissions
- On Android 13+, grant "Media files" permission

### Audio Not Playing
- Check device volume is not muted
- Ensure at least one track is loaded
- Try restarting the app

## System Requirements

- **Android Version**: 9.0 (API 28) or higher
- **Storage**: At least 50MB free space
- **RAM**: 2GB minimum recommended
- **Audio Files**: MP3 format supported

## Support

For issues or questions, please contact the development team with:
- Device model and Android version
- Steps to reproduce the issue
- Screenshots if applicable

## Notes

- This is a **debug build** for MVP testing
- The app uses standard Android permissions for file access
- All audio processing happens on-device (no internet required for playback)
- Tested on Android 9-14 devices

---

**Version**: 1.0  
**Build Date**: See APK file properties  
**Build Type**: Debug (Signed)

