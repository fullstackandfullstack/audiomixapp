# Android Audio Mixer App

A self-contained Android application for mixing multiple MP3 audio tracks with real-time volume control and announcement playback capabilities.

## Features

- **Multi-track Mixing**: Load and mix multiple MP3 files simultaneously
- **Real-time Volume Control**: Independent volume sliders for main tracks and announcements
- **Announcement System**: 
  - Play announcements at fixed intervals
  - Play announcements only at the end
  - Announcements play over main tracks with separate volume control
- **File Support**: 
  - Load MP3 files from device storage
  - Support for cloud storage providers (Google Drive, Dropbox, OneDrive)
- **Smooth Playback**: Low-latency, gap-free audio mixing using Android's AudioTrack API
- **Low CPU Usage**: Optimized for mid-range Android devices

## Requirements

- **Minimum SDK**: Android 9 (API 28)
- **Target SDK**: Android 14 (API 34)
- **Java Version**: Java 8
- **Android Studio**: Arctic Fox or later recommended

## Project Structure

```
MixApp/
├── app/
│   ├── build.gradle              # App-level build configuration
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/mixapp/
│   │       │   ├── MainActivity.java      # Main UI and control logic
│   │       │   ├── AudioMixer.java        # Core audio mixing engine
│   │       │   └── MP3Decoder.java        # MP3 decoding using MediaCodec
│   │       └── res/
│   │           ├── layout/
│   │           │   └── activity_main.xml  # Main UI layout
│   │           └── values/
│   │               ├── strings.xml
│   │               ├── colors.xml
│   │               └── themes.xml
│   └── proguard-rules.pro
├── build.gradle                   # Project-level build configuration
├── settings.gradle
├── gradle.properties
└── README.md
```

## Build Instructions

### Prerequisites

1. Install [Android Studio](https://developer.android.com/studio)
2. Ensure Android SDK is installed (API 28-34)
3. Java JDK 8 or later

### Building the Project

1. **Open the Project**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `MixApp` directory and select it

2. **Sync Gradle**
   - Android Studio will automatically sync Gradle dependencies
   - If not, click "Sync Now" when prompted
   - Wait for the sync to complete

3. **Build Debug APK**
   - Go to `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Or use the command line:
     ```bash
     ./gradlew assembleDebug
     ```
   - The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

4. **Sign the APK (for testing)**
   - Debug builds are automatically signed with a debug keystore
   - For release builds, configure signing in `app/build.gradle`

### Installing on Device

1. **Enable USB Debugging** on your Android device
2. **Connect device** via USB
3. **Install APK**:
   - Drag and drop the APK to the device, or
   - Use ADB: `adb install app-debug.apk`

## Third-Party Libraries

This project uses only Android SDK components:

- **AndroidX AppCompat**: UI components and backward compatibility
- **Material Components**: Modern Material Design UI elements
- **Android Media APIs**: 
  - `MediaCodec`: Hardware-accelerated MP3 decoding
  - `MediaExtractor`: Audio file extraction
  - `AudioTrack`: Low-latency audio playback

No external third-party libraries are required.

## Usage

1. **Add Main Tracks**
   - Tap "Add Track" button
   - Select an MP3 file from device storage or cloud drive
   - Repeat to add multiple tracks

2. **Add Announcements**
   - Tap "Add Announcement" button
   - Select a short MP3 file for announcements
   - Multiple announcements can be added

3. **Control Playback**
   - **Play**: Start mixing and playback
   - **Pause**: Pause playback (resume with Play)
   - **Stop**: Stop and reset playback

4. **Adjust Volumes**
   - **Main Volume**: Control volume of all main tracks
   - **Announcement Volume**: Control volume of announcements
   - Sliders work in real-time during playback

5. **Configure Announcements**
   - **Interval**: Set how often announcements play (in seconds)
   - **Play at End Only**: Check to play announcements only at the end of tracks

## Technical Details

### Audio Format
- **Sample Rate**: 44.1 kHz
- **Bit Depth**: 16-bit PCM
- **Channels**: Stereo (2 channels)
- **Buffer Size**: ~100ms (optimized for low latency)

### Mixing Algorithm
- All tracks are mixed in real-time using software mixing
- Volume is applied per-track before mixing
- Samples are clamped to prevent overflow
- Main tracks loop continuously
- Announcements play once per trigger

### Performance
- Optimized buffer sizes for smooth playback
- Background thread for audio processing
- Efficient PCM mixing with minimal CPU overhead
- Hardware-accelerated MP3 decoding when available

## Permissions

The app requires the following permissions:

- **READ_EXTERNAL_STORAGE** (Android 12 and below): Access audio files on device
- **READ_MEDIA_AUDIO** (Android 13+): Access audio files on device
- **INTERNET**: Access cloud storage providers (optional)

## Testing

Tested on:
- Android 9 (API 28)
- Android 10 (API 29)
- Android 11 (API 30)
- Android 12 (API 31)
- Android 13 (API 33)
- Android 14 (API 34)

## Known Limitations

1. **MP3 Format Only**: Currently supports MP3 files. Other formats would require additional decoders.
2. **Resampling**: Basic linear interpolation resampling. For production, consider a proper resampling library.
3. **End-Only Announcements**: The "play at end only" feature uses time-based estimation. For precise track-end detection, track duration metadata would be needed.

## Troubleshooting

### Build Errors
- Ensure Android SDK is properly installed
- Check that Java JDK 8+ is configured
- Try "Invalidate Caches / Restart" in Android Studio

### Runtime Errors
- Grant storage permissions when prompted
- Ensure MP3 files are valid and not corrupted
- Check device has sufficient memory for large audio files

### Playback Issues
- Ensure device volume is not muted
- Check that at least one track is loaded before playing
- Restart playback if audio becomes choppy

## License

This project is provided as-is for the specified use case.

## Support

For issues or questions, refer to the inline code comments for implementation details.

