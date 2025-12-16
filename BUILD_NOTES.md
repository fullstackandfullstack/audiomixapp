# Build Notes

## Launcher Icons

The AndroidManifest.xml references launcher icons (`@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`). 
Android Studio will automatically generate default launcher icons when you first build the project.

To customize icons:
1. Right-click on `app` → `New` → `Image Asset`
2. Select "Launcher Icons (Adaptive and Legacy)"
3. Configure your icon and click "Next" → "Finish"

## First Build

When building for the first time:
1. Android Studio may prompt to download missing SDK components - accept and wait for download
2. Gradle sync may take a few minutes on first run
3. If you see warnings about missing launcher icons, they will be auto-generated

## Testing the APK

After building the debug APK:
1. Transfer to your Android device
2. Enable "Install from Unknown Sources" if needed
3. Install and test with sample MP3 files

## Known Build Considerations

- The project uses AndroidX libraries (automatically migrated)
- Minimum SDK is Android 9 (API 28) for MediaCodec compatibility
- No external dependencies required - all using Android SDK

