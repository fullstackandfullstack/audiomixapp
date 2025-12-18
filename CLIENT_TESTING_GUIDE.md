# Client Testing Guide - Audio Mixer App

## 📦 APK Information
- **APK File**: `app/build/outputs/apk/debug/app-debug.apk`
- **File Size**: ~11-12 MB
- **Package**: com.mixapp
- **Version**: 1.0
- **Minimum Android**: Android 9 (API 28)
- **Target Android**: Android 14 (API 34)

## 📲 Installation Steps

1. **Transfer APK to Device**
   - Copy `app-debug.apk` to your Android device (via USB, email, or cloud storage)

2. **Enable Unknown Sources**
   - Go to **Settings** → **Security** (or **Apps** → **Special app access**)
   - Enable **"Install unknown apps"** for your file manager/browser

3. **Install the APK**
   - Open the APK file on your device
   - Tap **Install**
   - Tap **Open** when installation completes

4. **Grant Permissions**
   - When prompted, allow **Storage/Audio** permissions
   - This is required to select audio files

---

## ✅ Testing Checklist - All Reported Issues

### **Issue #1: Home Screen with Menu**

**What to Test:**
1. ✅ Launch the app
2. ✅ You should see a **Home** screen (not the controls screen)
3. ✅ Top menu bar should show **"Home"** on the left
4. ✅ Menu button (☰) should be visible on the top-right
5. ✅ If you have playlists, they should be listed here
6. ✅ If no playlists exist, you should see "No playlists yet. Tap the menu to create one."

**Expected Result:**
- Home screen displays first (not the controls)
- Menu shows "Home" in the path
- All playlists are visible on the home screen

---

### **Issue #2: File Names Preserved**

**What to Test:**
1. ✅ Create a new playlist
2. ✅ Tap **"Add Track"**
3. ✅ Select an MP3 or WAV file (e.g., "MySong.mp3" or "Announcement.wav")
4. ✅ Check the track list - the original filename should appear

**Expected Result:**
- Original file names are preserved (not changed to "audio_XXXXX.mp3")
- File extensions are maintained (.mp3, .wav, etc.)

---

### **Issue #3: Sequential Track Playback**

**What to Test:**
1. ✅ Create a playlist
2. ✅ Add **3-4 different tracks** (different songs)
3. ✅ Tap **"Play"**
4. ✅ Listen carefully - tracks should play **one after another**, not all at the same time

**Expected Result:**
- Only ONE track plays at a time
- When first track finishes, second track starts
- When second track finishes, third track starts
- Tracks play in the order they were added (or reordered)

---

### **Issue #4: Play at End Only + WAV Support**

**What to Test:**
1. ✅ Create a playlist
2. ✅ Add **2-3 main tracks**
3. ✅ Add **1-2 announcement files** (try both MP3 and WAV formats)
4. ✅ Enable **"Play at End Only"** checkbox
5. ✅ Tap **"Play"**
6. ✅ Wait for all main tracks to finish playing
7. ✅ Announcements should play **only after all tracks finish**

**Expected Result:**
- WAV files work correctly (not just MP3)
- Announcements play ONLY after all main tracks complete
- Announcements play in sequence

---

### **Issue #5: Sequential Playback for All**

**What to Test:**
1. ✅ Create a playlist
2. ✅ Add **3 tracks** and **3 announcements**
3. ✅ Tap **"Play"**
4. ✅ Observe playback order:
   - Tracks play sequentially (Track 1 → Track 2 → Track 3)
   - Announcements play sequentially (Announcement 1 → Announcement 2 → Announcement 3)
   - Announcements play according to interval settings

**Expected Result:**
- Tracks play one after another
- Announcements play one after another (not overlapping)
- Both respect the order they were added

---

### **Issue #6: Drag-and-Drop Reordering**

**What to Test - Tracks:**
1. ✅ Create a playlist
2. ✅ Add **3 tracks** (e.g., Track A, Track B, Track C)
3. ✅ **Long-press** on Track B
4. ✅ **Drag** it above Track A
5. ✅ Release to reorder
6. ✅ Tap **"Play"** - tracks should play in the new order

**What to Test - Announcements:**
1. ✅ Add **3 announcements** (e.g., Ann 1, Ann 2, Ann 3)
2. ✅ **Long-press** on Ann 3
3. ✅ **Drag** it to the top
4. ✅ Release to reorder
5. ✅ Tap **"Play"** - announcements should play in the new order

**Expected Result:**
- Both tracks and announcements can be reordered by drag-and-drop
- Playback order updates immediately after reordering
- Order persists after closing and reopening the app

---

### **Issue #7: Data Persistence**

**What to Test:**
1. ✅ Create **2-3 playlists** with different names
2. ✅ Add **tracks and announcements** to each playlist
3. ✅ **Close the app completely** (swipe away from recent apps)
4. ✅ **Reopen the app**
5. ✅ Check if all playlists are still there
6. ✅ Open each playlist and verify:
   - All tracks are present
   - All announcements are present
   - Original file names are preserved
   - Order is maintained

**Expected Result:**
- All playlists are saved and restored
- All tracks and announcements are saved
- Everything persists after app restart
- No data loss

---

### **Issue #8: Home Screen Shows All Lists**

**What to Test:**
1. ✅ Create **multiple playlists** (e.g., "Playlist 1", "Playlist 2", "Playlist 3")
2. ✅ Return to **Home screen** (tap menu → "Home" or use back button)
3. ✅ All playlists should be visible on the home screen
4. ✅ Each playlist should show:
   - Playlist name
   - Number of tracks
   - Number of announcements

**Expected Result:**
- Home screen displays all created playlists
- Playlist information is visible
- Can tap any playlist to open it

---

### **Issue #9: Delete Playlist with Confirmation**

**What to Test:**
1. ✅ Create a test playlist
2. ✅ Add some tracks/announcements to it
3. ✅ Go to menu → **"Manage Playlists"**
4. ✅ Select the playlist
5. ✅ Tap **"Delete"**
6. ✅ A confirmation dialog should appear asking:
   - "Are you sure you want to delete [Playlist Name]?"
   - "This will permanently delete the playlist and all its tracks and announcements."
7. ✅ Tap **"Cancel"** - playlist should NOT be deleted
8. ✅ Try again and tap **"Delete"** - playlist should be deleted

**Expected Result:**
- Confirmation dialog appears before deletion
- Clear warning message about permanent deletion
- Can cancel deletion
- Playlist is deleted only after confirmation

---

## 🎯 Additional Features to Test

### **Menu Navigation**
- ✅ Menu path shows: **"Home > Lists > [Playlist Name]"**
- ✅ Can navigate between Home and Lists screens
- ✅ Menu button works from all screens

### **Volume Controls**
- ✅ Main volume slider adjusts main track volume
- ✅ Announcement volume slider adjusts announcement volume
- ✅ Sliders work in real-time during playback

### **Interval Settings**
- ✅ Set interval to **0** - announcements play immediately one after another
- ✅ Set interval to **30 seconds** - announcements wait 30 seconds between plays
- ✅ Interval works for both single and multiple announcements

### **Fade In**
- ✅ When tapping **"Play"**, main track should fade in over 3 seconds
- ✅ Announcement should be heard first (if playing)
- ✅ Main track volume gradually increases from 0% to 100%

---

## 🐛 If You Find Issues

Please report:
1. **What you were doing** (step-by-step)
2. **What you expected** to happen
3. **What actually happened**
4. **Device information** (Android version, device model)
5. **Screenshots** (if possible)

---

## 📝 Quick Test Summary

**Minimum Test (5 minutes):**
1. Create a playlist
2. Add 2 tracks
3. Add 1 announcement
4. Play and verify sequential playback
5. Close and reopen app - verify data persists

**Full Test (15 minutes):**
1. Test all 9 issues above
2. Test drag-and-drop reordering
3. Test "Play at End Only"
4. Test different intervals
5. Test with both MP3 and WAV files
6. Create multiple playlists
7. Test delete with confirmation

---

## ✅ Success Criteria

All issues should be **FIXED**:
- ✅ Home screen shows first
- ✅ File names preserved
- ✅ Tracks play sequentially (not simultaneously)
- ✅ WAV files work + "Play at End Only" works
- ✅ All items play in sequence
- ✅ Drag-and-drop works for tracks and announcements
- ✅ Data persists after app restart
- ✅ Home screen shows all lists
- ✅ Delete shows confirmation dialog

---

**Build Date**: December 2025  
**Status**: Ready for Testing  
**Version**: 1.0 (All Issues Fixed)

