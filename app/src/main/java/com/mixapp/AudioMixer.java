package com.mixapp;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AudioMixer handles mixing multiple audio tracks and announcements into a single playback stream.
 * Uses AudioTrack for low-latency, gap-free playback with real-time volume control.
 */
public class AudioMixer {
    private static final String TAG = "AudioMixer";
    
    // Audio format constants - using 16-bit PCM at 44.1kHz for compatibility
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BYTES_PER_SAMPLE = 2; // 16-bit = 2 bytes
    private static final int CHANNELS = 2; // Stereo
    
    // Buffer size for smooth playback (about 100ms of audio)
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 4;
    
    private AudioTrack audioTrack;
    private Thread playbackThread;
    private AtomicBoolean isPlaying = new AtomicBoolean(false);
    private AtomicBoolean shouldStop = new AtomicBoolean(false);
    
    // Track data storage
    private List<TrackData> mainTracks = new ArrayList<>();
    private List<AnnouncementData> announcements = new ArrayList<>();
    
    // Volume controls (0.0 to 1.0)
    private float mainVolume = 0.8f;
    private float announcementVolume = 0.8f;
    
    // Announcement timing
    private int announcementIntervalSeconds = 30;
    private boolean playAtEndOnly = false;
    private int nextAnnouncementIndex = 0; // Track which announcement should play next in sequence
    
    // Sequential track playback
    private int currentTrackIndex = 0; // Track which main track is currently playing
    private boolean waitingForAnnouncementsAfterTrack = false; // For "Play at End Only" - waiting for announcements to finish after a track
    
    // Playback position tracking
    private long playbackStartTime = 0; // When playback started (for calculating elapsed time)
    private long totalSamplesPlayed = 0; // Total samples played across all tracks
    private long seekOffsetSamples = 0; // Offset for seeking
    
    // Fade in support
    private float fadeDurationSeconds = 3.0f; // Fade duration in seconds (3 seconds fade in)
    private float currentMainVolumeMultiplier = 0.0f; // Start at 0 so announcement is heard first
    private long fadeStartTime = 0;
    private boolean isFadingIn = false;
    
    // Ducking support (fade main track to 15% when announcement plays)
    private float duckVolume = 0.15f; // 15% volume when ducked
    private float duckFadeDurationSeconds = 0.5f; // 0.5 seconds fade for ducking
    private boolean isDucking = false;
    private boolean isDuckingOut = false; // Fading out (to 15%)
    private boolean isDuckingIn = false; // Fading back in (to 100%)
    private long duckStartTime = 0;
    private float volumeBeforeDuck = 1.0f; // Store volume before ducking
    
    /**
     * Represents a loaded audio track with file-based PCM data streaming
     */
    public static class TrackData {
        String name;
        File pcmFile; // File path to PCM data on disk
        long sampleCount;
        long currentPosition = 0; // Current position in samples
        boolean isLooping = true; // Main tracks loop continuously
        private PCMFileStream stream; // Lazy-loaded stream
        
        TrackData(String name, File pcmFile, long sampleCount) {
            this.name = name;
            this.pcmFile = pcmFile;
            this.sampleCount = sampleCount;
        }
        
        /**
         * Get or create the PCM file stream for this track
         */
        synchronized PCMFileStream getStream() throws IOException {
            if (stream == null || stream.isClosed()) {
                stream = new PCMFileStream(pcmFile, SAMPLE_RATE, CHANNELS);
            }
            return stream;
        }
        
        /**
         * Close the stream (call when done playing)
         */
        synchronized void closeStream() {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream for " + name, e);
                }
                stream = null;
            }
        }
    }
    
    /**
     * Represents an announcement clip with file-based PCM data streaming
     */
    public static class AnnouncementData {
        String name;
        File pcmFile; // File path to PCM data on disk
        long sampleCount;
        long currentPosition = 0; // Current position in samples
        long lastPlayTime = 0;
        boolean hasPlayed = false;
        private PCMFileStream stream; // Lazy-loaded stream
        
        AnnouncementData(String name, File pcmFile, long sampleCount) {
            this.name = name;
            this.pcmFile = pcmFile;
            this.sampleCount = sampleCount;
        }
        
        /**
         * Get or create the PCM file stream for this announcement
         */
        synchronized PCMFileStream getStream() throws IOException {
            if (stream == null || stream.isClosed()) {
                stream = new PCMFileStream(pcmFile, SAMPLE_RATE, CHANNELS);
            }
            return stream;
        }
        
        /**
         * Close the stream (call when done playing)
         */
        synchronized void closeStream() {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream for " + name, e);
                }
                stream = null;
            }
        }
    }
    
    /**
     * Initialize the AudioTrack for playback
     */
    public void initialize() {
        if (audioTrack != null) {
            release();
        }
        
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        
        AudioFormat audioFormat = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AUDIO_FORMAT)
                .setChannelMask(CHANNEL_CONFIG)
                .build();
        
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(BUFFER_SIZE)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        
        Log.d(TAG, "AudioTrack initialized with buffer size: " + BUFFER_SIZE);
    }
    
    /**
     * Load an audio file (MP3, WAV, etc.) and convert to PCM data, saving to disk
     * @param file The audio file to decode
     * @param pcmOutputFile Where to save the decoded PCM data
     * @param displayName Display name for the track
     * @return TrackData with file reference (not loaded into memory)
     */
    public TrackData loadAudioFile(File file, File pcmOutputFile, String displayName) throws IOException {
        Log.d(TAG, "Loading audio file: " + displayName + " (decoding directly to disk)");
        
        // Decode audio directly to disk (streaming, low memory)
        MP3Decoder.DecodeResult result = MP3Decoder.decodeAudioToFile(file, pcmOutputFile);
        
        // Return TrackData with file reference (not loaded into memory)
        return new TrackData(displayName, pcmOutputFile, result.sampleCount);
    }
    
    /**
     * Save PCM data to a file in raw binary format (little-endian 16-bit samples)
     */
    private void savePCMDataToFile(short[] pcmData, File outputFile) throws IOException {
        // Ensure parent directory exists
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // Write PCM data as raw bytes (little-endian)
        java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        
        for (short sample : pcmData) {
            buffer.putShort(0, sample);
            fos.write(buffer.array());
        }
        
        fos.close();
        Log.d(TAG, "Saved PCM data to: " + outputFile.getName() + " (" + (outputFile.length() / (1024 * 1024)) + " MB)");
    }
    
    /**
     * Add a main track to the mixer
     */
    public void addMainTrack(TrackData track) {
        synchronized (mainTracks) {
            mainTracks.add(track);
        }
        Log.d(TAG, "Added main track: " + track.name);
    }
    
    /**
     * Add an announcement clip
     */
    public void addAnnouncement(AnnouncementData announcement) {
        synchronized (announcements) {
            announcements.add(announcement);
        }
        Log.d(TAG, "Added announcement: " + announcement.name);
    }
    
    /**
     * Set main volume (0.0 to 1.0)
     */
    public void setMainVolume(float volume) {
        this.mainVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    /**
     * Set announcement volume (0.0 to 1.0)
     */
    public void setAnnouncementVolume(float volume) {
        this.announcementVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    /**
     * Set announcement interval in seconds
     */
    public void setAnnouncementInterval(int seconds) {
        this.announcementIntervalSeconds = seconds;
    }
    
    /**
     * Set whether announcements play only at the end
     */
    public void setPlayAtEndOnly(boolean playAtEndOnly) {
        this.playAtEndOnly = playAtEndOnly;
    }
    
    /**
     * Load a playlist into the mixer
     */
    public void loadPlaylist(Playlist playlist) {
        boolean wasPlaying = isPlaying.get();
        
        if (wasPlaying) {
            // Stop playback completely to ensure clean state
            stop();
        }
        
        // Clear and reload main tracks
        synchronized (mainTracks) {
            mainTracks.clear();
            // Create new list to avoid reference issues
            List<TrackData> newTracks = new ArrayList<>(playlist.getTracks());
            mainTracks.addAll(newTracks);
        }
        
        // Clear and reload announcements with new order
        // Create a completely new list from playlist to ensure we get the exact order
        List<AnnouncementData> newAnnouncements = new ArrayList<>();
        for (AudioMixer.AnnouncementData ann : playlist.getAnnouncements()) {
            // Use the same announcement objects but reset their state
            AnnouncementData newAnn = new AnnouncementData(ann.name, ann.pcmFile, ann.sampleCount);
            newAnn.currentPosition = 0;
            newAnn.lastPlayTime = 0;
            newAnn.hasPlayed = false;
            newAnnouncements.add(newAnn);
        }
        
        // Atomically replace the entire list
        synchronized (announcements) {
            announcements.clear();
            announcements.addAll(newAnnouncements);
        }
        
        // Reset sequence tracking to start from beginning
        nextAnnouncementIndex = 0;
        currentTrackIndex = 0;
        
        Log.d(TAG, "Loaded playlist: " + playlist.getName() + 
              " (Tracks: " + playlist.getTracks().size() + 
              ", Announcements: " + playlist.getAnnouncements().size() + ")");
        
        // Log announcement order for debugging
        synchronized (announcements) {
            for (int i = 0; i < announcements.size(); i++) {
                Log.d(TAG, "Announcement " + i + ": " + announcements.get(i).name);
            }
        }
        
        if (wasPlaying) {
            // Restart playback with new order
            play();
        }
    }
    
    /**
     * Set fade duration in seconds
     */
    public void setFadeDuration(float seconds) {
        this.fadeDurationSeconds = seconds;
    }
    
    /**
     * Start playback
     */
    public void play() {
        if (isPlaying.get()) {
            return;
        }
        
        if (mainTracks.isEmpty()) {
            Log.w(TAG, "No tracks to play");
            return;
        }
        
        if (audioTrack == null) {
            initialize();
        }
        
        shouldStop.set(false);
        isPlaying.set(true);
        
        // Reset all track positions
        synchronized (mainTracks) {
            for (TrackData track : mainTracks) {
                track.currentPosition = 0;
            }
        }
        
        synchronized (announcements) {
            for (AnnouncementData ann : announcements) {
                ann.currentPosition = 0;
                ann.lastPlayTime = 0;
                ann.hasPlayed = false;
            }
        }
        
        // Reset sequence tracking
        nextAnnouncementIndex = 0;
        currentTrackIndex = 0; // Start with first track
        waitingForAnnouncementsAfterTrack = false;
        
        // Reset fade state - start at 0, then fade in over 3 seconds
        currentMainVolumeMultiplier = 0.0f;
        isFadingIn = true; // Always fade in when play starts
        fadeStartTime = System.currentTimeMillis();
        
        // Reset position tracking
        playbackStartTime = System.currentTimeMillis();
        totalSamplesPlayed = 0;
        seekOffsetSamples = 0;
        
        audioTrack.play();
        
        // Start playback thread
        playbackThread = new Thread(this::playbackLoop);
        playbackThread.start();
        
        Log.d(TAG, "Playback started");
    }
    
    /**
     * Check if currently playing
     */
    public boolean isPlaying() {
        return isPlaying.get();
    }
    
    /**
     * Pause playback
     */
    public void pause() {
        if (audioTrack != null && isPlaying.get()) {
            audioTrack.pause();
            isPlaying.set(false);
            Log.d(TAG, "Playback paused");
        }
    }
    
    /**
     * Stop playback immediately
     */
    public void stop() {
        shouldStop.set(true);
        isPlaying.set(false);
        
        if (audioTrack != null) {
            try {
                audioTrack.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioTrack", e);
            }
        }
        
        if (playbackThread != null) {
            try {
                // Wait for thread to finish, but don't wait too long
                playbackThread.join(2000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping playback thread", e);
                Thread.currentThread().interrupt();
            }
            playbackThread = null;
        }
        
        resetPlaybackState();
        Log.d(TAG, "Playback stopped");
    }
    
    /**
     * Reset playback state
     */
    private void resetPlaybackState() {
        // Reset positions and close streams
        synchronized (mainTracks) {
            for (TrackData track : mainTracks) {
                track.currentPosition = 0;
                track.closeStream(); // Close any open streams
            }
        }
        currentTrackIndex = 0;
        
        synchronized (announcements) {
            for (AnnouncementData ann : announcements) {
                ann.currentPosition = 0;
                ann.lastPlayTime = 0;
                ann.hasPlayed = false;
                ann.closeStream(); // Close any open streams
            }
        }
        
        // Reset fade state
        currentMainVolumeMultiplier = 0.0f;
        isFadingIn = false;
        fadeStartTime = 0;
        nextAnnouncementIndex = 0;
        currentTrackIndex = 0;
        waitingForAnnouncementsAfterTrack = false;
        
        // Reset position tracking
        playbackStartTime = 0;
        totalSamplesPlayed = 0;
        seekOffsetSamples = 0;
        
        // Reset ducking state
        isDucking = false;
        isDuckingOut = false;
        isDuckingIn = false;
        duckStartTime = 0;
        volumeBeforeDuck = 1.0f;
        
        shouldStop.set(false); // Reset for next play
    }
    
    /**
     * Main playback loop - mixes all tracks and writes to AudioTrack
     */
    private void playbackLoop() {
        int samplesPerBuffer = BUFFER_SIZE / (BYTES_PER_SAMPLE * CHANNELS);
        short[] mixBuffer = new short[samplesPerBuffer * CHANNELS];
        long startTime = System.currentTimeMillis();
        
        // Track total duration of all tracks for "play at end" detection
        long totalTrackDurationSamples = 0;
        synchronized (mainTracks) {
            for (TrackData track : mainTracks) {
                totalTrackDurationSamples += track.sampleCount;
            }
        }
        long totalTrackDurationMs = (totalTrackDurationSamples * 1000) / SAMPLE_RATE;
        
        while (!shouldStop.get() && isPlaying.get()) {
            // Clear mix buffer
            for (int i = 0; i < mixBuffer.length; i++) {
                mixBuffer[i] = 0;
            }
            
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - startTime) / 1000;
            
            // Check if any announcement is currently playing (for ducking)
            AnnouncementData currentlyPlayingAnnouncement = null;
            synchronized (announcements) {
                for (AnnouncementData ann : announcements) {
                    if (ann.currentPosition > 0 && ann.currentPosition < ann.sampleCount) {
                        currentlyPlayingAnnouncement = ann;
                        break;
                    }
                }
            }
            
            // Handle ducking: fade main track to 15% when announcement plays
            boolean shouldDuck = (currentlyPlayingAnnouncement != null && announcementIntervalSeconds > 0);
            
            // Calculate fade multiplier for main tracks
            float mainVolumeMultiplier = currentMainVolumeMultiplier;
            
            if (isFadingIn) {
                // Initial fade in from 0 to 100% over 3 seconds
                float fadeElapsed = (currentTime - fadeStartTime) / 1000.0f;
                if (fadeElapsed < fadeDurationSeconds) {
                    mainVolumeMultiplier = fadeElapsed / fadeDurationSeconds;
                    mainVolumeMultiplier = Math.min(1.0f, mainVolumeMultiplier);
                } else {
                    mainVolumeMultiplier = 1.0f;
                    isFadingIn = false;
                }
                currentMainVolumeMultiplier = mainVolumeMultiplier;
            } else if (shouldDuck) {
                // Announcement is playing - duck main track to 15%
                if (!isDucking) {
                    // Start ducking - fade out to 15%
                    isDucking = true;
                    isDuckingOut = true;
                    isDuckingIn = false;
                    duckStartTime = currentTime;
                    volumeBeforeDuck = currentMainVolumeMultiplier;
                }
                
                if (isDuckingOut) {
                    // Fading out to 15% (ducking)
                    float duckElapsed = (currentTime - duckStartTime) / 1000.0f;
                    if (duckElapsed < duckFadeDurationSeconds) {
                        // Fade from current volume to 15%
                        float progress = duckElapsed / duckFadeDurationSeconds;
                        mainVolumeMultiplier = volumeBeforeDuck - (volumeBeforeDuck - duckVolume) * progress;
                    } else {
                        mainVolumeMultiplier = duckVolume; // Stay at 15%
                        isDuckingOut = false;
                    }
                } else {
                    // Already ducked - stay at 15%
                    mainVolumeMultiplier = duckVolume;
                }
                currentMainVolumeMultiplier = mainVolumeMultiplier;
            } else if (isDucking) {
                // Announcement finished - fade back in to 100%
                if (isDuckingOut) {
                    // We were ducking out, now switch to ducking in
                    isDuckingOut = false;
                    isDuckingIn = true;
                    duckStartTime = currentTime;
                }
                
                if (isDuckingIn) {
                    // Fading back in from 15% to 100%
                    float duckElapsed = (currentTime - duckStartTime) / 1000.0f;
                    if (duckElapsed < duckFadeDurationSeconds) {
                        // Fade from 15% to 100%
                        float progress = duckElapsed / duckFadeDurationSeconds;
                        mainVolumeMultiplier = duckVolume + (1.0f - duckVolume) * progress;
                    } else {
                        mainVolumeMultiplier = 1.0f; // Back to 100%
                        isDuckingIn = false;
                        isDucking = false;
                    }
                } else {
                    mainVolumeMultiplier = 1.0f;
                    isDucking = false;
                }
                currentMainVolumeMultiplier = mainVolumeMultiplier;
            } else {
                // Normal playback - stay at full volume
                mainVolumeMultiplier = 1.0f;
                currentMainVolumeMultiplier = mainVolumeMultiplier;
            }
            
            // Play tracks sequentially - only play the current track
            TrackData currentTrack = null;
            synchronized (mainTracks) {
                if (!mainTracks.isEmpty() && currentTrackIndex < mainTracks.size()) {
                    currentTrack = mainTracks.get(currentTrackIndex);
                }
            }
            
            if (currentTrack != null && !waitingForAnnouncementsAfterTrack) {
                // Mix only the current track
                mixTrack(currentTrack, mixBuffer, samplesPerBuffer, mainVolume * mainVolumeMultiplier);
                
                // Check if current track finished
                if (currentTrack.currentPosition >= currentTrack.sampleCount) {
                    // Track finished
                    if (playAtEndOnly && !announcements.isEmpty()) {
                        // "Play at End Only" - play announcements after this track finishes
                        // Reset announcement sequence for this track
                        synchronized (announcements) {
                            nextAnnouncementIndex = 0;
                            for (AnnouncementData ann : announcements) {
                                ann.currentPosition = 0;
                                ann.lastPlayTime = 0;
                                ann.hasPlayed = false;
                            }
                        }
                        waitingForAnnouncementsAfterTrack = true;
                        // Don't move to next track yet - wait for announcements to finish
                    } else {
                        // Move to next track immediately
                        synchronized (mainTracks) {
                            currentTrackIndex++;
                            if (currentTrackIndex >= mainTracks.size()) {
                                // All tracks finished
                                if (!playAtEndOnly) {
                                    // Loop back to first track for continuous playback
                                    currentTrackIndex = 0;
                                    // Reset all track positions for next cycle
                                    for (TrackData track : mainTracks) {
                                        track.currentPosition = 0;
                                    }
                                } else {
                                    // For "Play at End Only", stop at the end
                                    // Don't reset currentTrackIndex, keep it at mainTracks.size()
                                    // This signals that all tracks have finished
                                }
                            } else {
                                // Reset next track position
                                if (currentTrackIndex < mainTracks.size()) {
                                    mainTracks.get(currentTrackIndex).currentPosition = 0;
                                }
                            }
                        }
                    }
                }
            }
            
            // Mix the currently playing announcement (if any)
            if (currentlyPlayingAnnouncement != null) {
                // Continue playing the current announcement
                mixAnnouncement(currentlyPlayingAnnouncement, mixBuffer, samplesPerBuffer, announcementVolume);
                
                if (currentlyPlayingAnnouncement.currentPosition >= currentlyPlayingAnnouncement.sampleCount) {
                    // Announcement finished - reset for next play
                    currentlyPlayingAnnouncement.currentPosition = 0;
                    currentlyPlayingAnnouncement.lastPlayTime = currentTime; // Update time when finished
                    currentlyPlayingAnnouncement.hasPlayed = true;
                    currentlyPlayingAnnouncement.closeStream(); // Close stream when done
                    
                    // Move to next announcement in sequence
                    synchronized (announcements) {
                        nextAnnouncementIndex++;
                        if (nextAnnouncementIndex >= announcements.size()) {
                            // All announcements played
                            if (waitingForAnnouncementsAfterTrack) {
                                // We were waiting for announcements after a track - now move to next track
                                waitingForAnnouncementsAfterTrack = false;
                                synchronized (mainTracks) {
                                    currentTrackIndex++;
                                    if (currentTrackIndex >= mainTracks.size()) {
                                        // All tracks finished
                                        if (!playAtEndOnly) {
                                            // Loop back to first track for continuous playback
                                            currentTrackIndex = 0;
                                            // Reset all track positions for next cycle
                                            for (TrackData track : mainTracks) {
                                                track.currentPosition = 0;
                                            }
                                        }
                                    } else {
                                        // Reset next track position
                                        if (currentTrackIndex < mainTracks.size()) {
                                            mainTracks.get(currentTrackIndex).currentPosition = 0;
                                        }
                                    }
                                }
                            }
                            // Reset to start for next cycle
                            nextAnnouncementIndex = 0;
                            // Reset all hasPlayed flags for next cycle
                            for (AnnouncementData ann : announcements) {
                                ann.hasPlayed = false;
                            }
                        }
                    }
                }
            } else {
                // No announcement is currently playing - check if we should start a new one
                boolean shouldStartAnnouncement = false;
                int announcementIndexToPlay = -1;
                
                if (playAtEndOnly) {
                    // Play at end only - play announcements after each track finishes
                    // This is triggered when waitingForAnnouncementsAfterTrack is true
                    if (waitingForAnnouncementsAfterTrack && !announcements.isEmpty()) {
                        // Find next announcement in sequence that hasn't played yet
                        synchronized (announcements) {
                            // Play announcements in sequence starting from nextAnnouncementIndex
                            for (int i = 0; i < announcements.size(); i++) {
                                int idx = (nextAnnouncementIndex + i) % announcements.size();
                                AnnouncementData ann = announcements.get(idx);
                                if (ann.currentPosition == 0 && !ann.hasPlayed) {
                                    shouldStartAnnouncement = true;
                                    announcementIndexToPlay = idx;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    // Interval-based playback
                    synchronized (announcements) {
                        if (announcements.isEmpty()) {
                            shouldStartAnnouncement = false;
                        } else if (announcementIntervalSeconds == 0) {
                            // Interval is 0 - play ASAP (immediately, one after another in sequence)
                            if (nextAnnouncementIndex < announcements.size()) {
                                AnnouncementData ann = announcements.get(nextAnnouncementIndex);
                                // Play next in sequence immediately (no delay)
                                if (ann.currentPosition == 0) {
                                    shouldStartAnnouncement = true;
                                    announcementIndexToPlay = nextAnnouncementIndex;
                                }
                            }
                        } else {
                            // Interval > 0 - first announcement plays immediately, then respect interval
                            if (nextAnnouncementIndex < announcements.size()) {
                                AnnouncementData ann = announcements.get(nextAnnouncementIndex);
                                
                                // Check if it's time to play this announcement
                                boolean timeToPlay = false;
                                
                                if (nextAnnouncementIndex == 0) {
                                    // First announcement (index 0)
                                    if (ann.lastPlayTime > 0) {
                                        // Already played before (cycling) - check interval since last finished
                                        long timeSinceLastFinished = currentTime - ann.lastPlayTime;
                                        timeToPlay = (timeSinceLastFinished >= (announcementIntervalSeconds * 1000));
                                    } else if (!ann.hasPlayed && ann.currentPosition == 0) {
                                        // Never played before - play immediately when playback starts
                                        timeToPlay = true;
                                    }
                                } else {
                                    // Subsequent announcements - check interval since previous finished
                                    AnnouncementData prevAnn = announcements.get(nextAnnouncementIndex - 1);
                                    if (prevAnn.lastPlayTime > 0) {
                                        // Previous announcement has finished - check interval
                                        long timeSincePrevFinished = currentTime - prevAnn.lastPlayTime;
                                        timeToPlay = (timeSincePrevFinished >= (announcementIntervalSeconds * 1000));
                                    }
                                }
                                
                                if (timeToPlay && ann.currentPosition == 0) {
                                    shouldStartAnnouncement = true;
                                    announcementIndexToPlay = nextAnnouncementIndex;
                                }
                            }
                        }
                    }
                }
                
                // Start playing the selected announcement
                if (shouldStartAnnouncement && announcementIndexToPlay >= 0) {
                    synchronized (announcements) {
                        if (announcementIndexToPlay < announcements.size()) {
                            AnnouncementData ann = announcements.get(announcementIndexToPlay);
                            // Start playing this announcement
                            ann.currentPosition = 0; // Reset position
                            mixAnnouncement(ann, mixBuffer, samplesPerBuffer, announcementVolume);
                            // Don't set lastPlayTime here - it will be set when announcement finishes
                            ann.hasPlayed = true;
                            Log.d(TAG, "Starting announcement " + announcementIndexToPlay + ": " + ann.name);
                        }
                    }
                }
            }
            
            // Write mixed buffer to AudioTrack
            int written = audioTrack.write(mixBuffer, 0, mixBuffer.length);
            if (written < 0) {
                Log.e(TAG, "Error writing to AudioTrack: " + written);
                break;
            }
            
            // Track position is updated via currentPosition in TrackData
        }
        
        if (audioTrack != null) {
            audioTrack.stop();
        }
        
        Log.d(TAG, "Playback loop ended");
    }
    
    /**
     * Mix a track into the mix buffer by streaming from file
     */
    private void mixTrack(TrackData track, short[] mixBuffer, int samplesPerBuffer, float volume) {
        try {
            PCMFileStream stream = track.getStream();
            long samplesToMix = Math.min(samplesPerBuffer, track.sampleCount - track.currentPosition);
            
            if (samplesToMix <= 0) {
                return;
            }
            
            // Read samples from file
            short[] readBuffer = new short[(int)samplesToMix * CHANNELS];
            int samplesRead = stream.readSamples(track.currentPosition, (int)samplesToMix, readBuffer);
            
            if (samplesRead > 0) {
                // Mix into buffer with volume applied
                for (int i = 0; i < samplesRead * CHANNELS; i++) {
                    int mixed = mixBuffer[i] + (int)(readBuffer[i] * volume);
                    mixBuffer[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixed));
                }
                
                // Update position
                track.currentPosition += samplesRead;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading track data for " + track.name, e);
        }
    }
    
    /**
     * Mix an announcement into the mix buffer by streaming from file
     */
    private void mixAnnouncement(AnnouncementData ann, short[] mixBuffer, int samplesPerBuffer, float volume) {
        try {
            PCMFileStream stream = ann.getStream();
            long samplesToMix = Math.min(samplesPerBuffer, ann.sampleCount - ann.currentPosition);
            
            if (samplesToMix <= 0) {
                return;
            }
            
            // Read samples from file
            short[] readBuffer = new short[(int)samplesToMix * CHANNELS];
            int samplesRead = stream.readSamples(ann.currentPosition, (int)samplesToMix, readBuffer);
            
            if (samplesRead > 0) {
                // Mix into buffer with volume applied
                for (int i = 0; i < samplesRead * CHANNELS; i++) {
                    int mixed = mixBuffer[i] + (int)(readBuffer[i] * volume);
                    mixBuffer[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixed));
                }
                
                // Update position
                ann.currentPosition += samplesRead;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading announcement data for " + ann.name, e);
        }
    }
    
    /**
     * Get list of main track names
     */
    public List<String> getMainTrackNames() {
        List<String> names = new ArrayList<>();
        synchronized (mainTracks) {
            for (TrackData track : mainTracks) {
                names.add(track.name);
            }
        }
        return names;
    }
    
    /**
     * Get list of announcement names
     */
    public List<String> getAnnouncementNames() {
        List<String> names = new ArrayList<>();
        synchronized (announcements) {
            for (AnnouncementData ann : announcements) {
                names.add(ann.name);
            }
        }
        return names;
    }
    
    /**
     * Clear all tracks
     */
    public void clearAll() {
        stop();
        synchronized (mainTracks) {
            mainTracks.clear();
        }
        synchronized (announcements) {
            announcements.clear();
        }
    }
    
    /**
     * Release resources
     */
    public void release() {
        stop();
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
    }
    
    /**
     * Get current track position in milliseconds
     */
    public long getCurrentTrackPosition() {
        synchronized (mainTracks) {
            if (mainTracks.isEmpty() || currentTrackIndex >= mainTracks.size()) {
                return 0;
            }
            TrackData currentTrack = mainTracks.get(currentTrackIndex);
            long positionSamples = currentTrack.currentPosition;
            return (positionSamples * 1000) / SAMPLE_RATE;
        }
    }
    
    /**
     * Get current track duration in milliseconds
     */
    public long getCurrentTrackDuration() {
        synchronized (mainTracks) {
            if (mainTracks.isEmpty() || currentTrackIndex >= mainTracks.size()) {
                return 0;
            }
            TrackData currentTrack = mainTracks.get(currentTrackIndex);
            long durationSamples = currentTrack.sampleCount;
            return (durationSamples * 1000) / SAMPLE_RATE;
        }
    }
    
    /**
     * Get current playlist position in milliseconds (total elapsed time)
     */
    public long getCurrentPlaylistPosition() {
        synchronized (mainTracks) {
            if (mainTracks.isEmpty()) {
                return 0;
            }
            long totalSamples = 0;
            for (int i = 0; i < currentTrackIndex && i < mainTracks.size(); i++) {
                totalSamples += mainTracks.get(i).sampleCount;
            }
            if (currentTrackIndex < mainTracks.size()) {
                TrackData currentTrack = mainTracks.get(currentTrackIndex);
                totalSamples += currentTrack.currentPosition;
            }
            return (totalSamples * 1000) / SAMPLE_RATE;
        }
    }
    
    /**
     * Get total playlist duration in milliseconds
     */
    public long getPlaylistDuration() {
        synchronized (mainTracks) {
            if (mainTracks.isEmpty()) {
                return 0;
            }
            long totalSamples = 0;
            for (TrackData track : mainTracks) {
                totalSamples += track.sampleCount;
            }
            return (totalSamples * 1000) / SAMPLE_RATE;
        }
    }
    
    /**
     * Seek to position in current track (milliseconds)
     */
    public void seekToTrackPosition(long positionMs) {
        synchronized (mainTracks) {
            if (mainTracks.isEmpty() || currentTrackIndex >= mainTracks.size()) {
                return;
            }
            TrackData currentTrack = mainTracks.get(currentTrackIndex);
            long positionSamples = (positionMs * SAMPLE_RATE) / 1000;
            positionSamples = Math.max(0, Math.min(positionSamples, currentTrack.sampleCount));
            currentTrack.currentPosition = positionSamples;
        }
    }
    
    /**
     * Seek to position in playlist (milliseconds)
     */
    public void seekToPlaylistPosition(long positionMs) {
        synchronized (mainTracks) {
            if (mainTracks.isEmpty()) {
                return;
            }
            long positionSamples = (positionMs * SAMPLE_RATE) / 1000;
            long accumulatedSamples = 0;
            
            // Find which track contains this position
            for (int i = 0; i < mainTracks.size(); i++) {
                TrackData track = mainTracks.get(i);
                long trackSamples = track.sampleCount;
                
                if (positionSamples < accumulatedSamples + trackSamples) {
                    // Position is in this track
                    currentTrackIndex = i;
                    long offsetInTrack = positionSamples - accumulatedSamples;
                    track.currentPosition = offsetInTrack;
                    // Reset all subsequent tracks
                    for (int j = i + 1; j < mainTracks.size(); j++) {
                        mainTracks.get(j).currentPosition = 0;
                    }
                    return;
                }
                accumulatedSamples += trackSamples;
            }
            
            // Position is beyond all tracks - go to end
            currentTrackIndex = mainTracks.size() - 1;
            if (currentTrackIndex >= 0) {
                TrackData lastTrack = mainTracks.get(currentTrackIndex);
                lastTrack.currentPosition = lastTrack.sampleCount;
            }
        }
    }
}

