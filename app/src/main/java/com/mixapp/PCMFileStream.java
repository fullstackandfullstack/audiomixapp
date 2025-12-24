package com.mixapp;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Streams PCM audio data from disk in chunks instead of loading entire file into memory.
 * This dramatically reduces memory usage for large audio files.
 */
public class PCMFileStream {
    private static final String TAG = "PCMFileStream";
    
    private File pcmFile;
    private RandomAccessFile fileHandle;
    private long fileSizeBytes;
    private long totalSamples;
    private int sampleRate;
    private int channels;
    private static final int BYTES_PER_SAMPLE = 2; // 16-bit = 2 bytes
    
    // Buffer for reading chunks (about 1 second of audio at 44.1kHz stereo)
    private static final int BUFFER_SIZE_SAMPLES = 44100 * 2; // 1 second stereo
    private short[] readBuffer;
    
    /**
     * Create a PCM file stream from a file
     * @param pcmFile The PCM file to stream from
     * @param sampleRate Sample rate (e.g., 44100)
     * @param channels Number of channels (2 for stereo)
     */
    public PCMFileStream(File pcmFile, int sampleRate, int channels) throws IOException {
        this.pcmFile = pcmFile;
        this.sampleRate = sampleRate;
        this.channels = channels;
        
        if (!pcmFile.exists()) {
            throw new IOException("PCM file does not exist: " + pcmFile.getAbsolutePath());
        }
        
        fileSizeBytes = pcmFile.length();
        totalSamples = fileSizeBytes / (BYTES_PER_SAMPLE * channels);
        
        // Open file for random access
        fileHandle = new RandomAccessFile(pcmFile, "r");
        readBuffer = new short[BUFFER_SIZE_SAMPLES * channels];
        
        Log.d(TAG, "Opened PCM stream: " + pcmFile.getName() + 
              " (" + totalSamples + " samples, " + (fileSizeBytes / (1024 * 1024)) + " MB)");
    }
    
    /**
     * Read PCM samples starting from a specific position
     * @param startSample Starting sample index (0-based)
     * @param numSamples Number of samples to read
     * @param outputBuffer Buffer to write samples to (must be large enough)
     * @return Number of samples actually read
     */
    public int readSamples(long startSample, int numSamples, short[] outputBuffer) throws IOException {
        if (fileHandle == null) {
            throw new IOException("File stream is closed");
        }
        
        // Clamp to file bounds
        if (startSample >= totalSamples) {
            return 0; // End of file
        }
        
        long samplesToRead = Math.min(numSamples, totalSamples - startSample);
        if (samplesToRead <= 0) {
            return 0;
        }
        
        // Calculate byte position
        long bytePosition = startSample * BYTES_PER_SAMPLE * channels;
        
        // Seek to position
        fileHandle.seek(bytePosition);
        
        // Read bytes
        int bytesToRead = (int) (samplesToRead * BYTES_PER_SAMPLE * channels);
        byte[] byteBuffer = new byte[bytesToRead];
        int bytesRead = fileHandle.read(byteBuffer);
        
        if (bytesRead <= 0) {
            return 0;
        }
        
        // Convert bytes to short array (little-endian)
        int samplesRead = bytesRead / (BYTES_PER_SAMPLE * channels);
        for (int i = 0; i < samplesRead * channels; i++) {
            int byteIndex = i * BYTES_PER_SAMPLE;
            int low = byteBuffer[byteIndex] & 0xFF;
            int high = byteBuffer[byteIndex + 1] & 0xFF;
            outputBuffer[i] = (short) ((high << 8) | low);
        }
        
        return samplesRead;
    }
    
    /**
     * Get total number of samples in the file
     */
    public long getTotalSamples() {
        return totalSamples;
    }
    
    /**
     * Get sample rate
     */
    public int getSampleRate() {
        return sampleRate;
    }
    
    /**
     * Get number of channels
     */
    public int getChannels() {
        return channels;
    }
    
    /**
     * Close the file stream
     */
    public void close() throws IOException {
        if (fileHandle != null) {
            fileHandle.close();
            fileHandle = null;
        }
    }
    
    /**
     * Check if stream is closed
     */
    public boolean isClosed() {
        return fileHandle == null;
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}

