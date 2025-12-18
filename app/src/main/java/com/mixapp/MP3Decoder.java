package com.mixapp;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * MP3Decoder handles decoding MP3 files to PCM format using Android's MediaCodec API.
 * This provides efficient, hardware-accelerated decoding when available.
 */
public class MP3Decoder {
    private static final String TAG = "MP3Decoder";
    
    /**
     * Decode an audio file (MP3, WAV, etc.) to 16-bit PCM stereo at 44.1kHz
     * @param file The audio file to decode
     * @return Array of 16-bit PCM samples (interleaved stereo)
     * @throws IOException If decoding fails
     */
    public static short[] decodeAudio(File file) throws IOException {
        return decodeMP3(file);
    }
    
    /**
     * Decode an MP3 file to 16-bit PCM stereo at 44.1kHz
     * @param file The MP3 file to decode
     * @return Array of 16-bit PCM samples (interleaved stereo)
     * @throws IOException If decoding fails
     */
    public static short[] decodeMP3(File file) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        
        try {
            // Support both file paths and content URIs
            String dataSource = file.getAbsolutePath();
            try {
                extractor.setDataSource(dataSource);
            } catch (Exception e) {
                // Try as content URI if file path fails
                extractor.setDataSource(file.getPath());
            }
            
            // Find the audio track
            int trackIndex = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    trackIndex = i;
                    break;
                }
            }
            
            if (trackIndex == -1) {
                throw new IOException("No audio track found in file");
            }
            
            extractor.selectTrack(trackIndex);
            
            // Get sample rate and channel count
            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            
            Log.d(TAG, "Decoding MP3: sampleRate=" + sampleRate + ", channels=" + channelCount);
            
            // Create decoder
            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, null, null, 0);
            decoder.start();
            
            // Decode the file
            List<short[]> decodedChunks = new ArrayList<>();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            
            boolean inputDone = false;
            boolean outputDone = false;
            
            while (!outputDone) {
                // Feed input
                if (!inputDone) {
                    int inputIndex = decoder.dequeueInputBuffer(10000);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                        if (inputBuffer != null) {
                            int sampleSize = extractor.readSampleData(inputBuffer, 0);
                            
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                inputDone = true;
                            } else {
                                long presentationTimeUs = extractor.getSampleTime();
                                decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                                extractor.advance();
                            }
                        }
                    }
                }
                
                // Get output
                int outputIndex = decoder.dequeueOutputBuffer(info, 10000);
                if (outputIndex >= 0) {
                    ByteBuffer outputBuffer = decoder.getOutputBuffer(outputIndex);
                    
                    if (outputBuffer != null && info.size > 0) {
                        // Position buffer at the correct offset
                        outputBuffer.position(info.offset);
                        outputBuffer.limit(info.offset + info.size);
                        
                        // Convert to short array
                        int outputSize = info.size;
                        byte[] outputBytes = new byte[outputSize];
                        outputBuffer.get(outputBytes);
                        
                        // Convert bytes to shorts (16-bit samples)
                        short[] samples = new short[outputSize / 2];
                        ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                        
                        decodedChunks.add(samples);
                    }
                    
                    decoder.releaseOutputBuffer(outputIndex, false);
                    
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true;
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    format = decoder.getOutputFormat();
                    Log.d(TAG, "Output format changed: " + format);
                }
            }
            
            // Combine all chunks
            int totalSamples = 0;
            for (short[] chunk : decodedChunks) {
                totalSamples += chunk.length;
            }
            
            short[] allSamples = new short[totalSamples];
            int offset = 0;
            for (short[] chunk : decodedChunks) {
                System.arraycopy(chunk, 0, allSamples, offset, chunk.length);
                offset += chunk.length;
            }
            
            // Resample to 44.1kHz if needed
            if (sampleRate != 44100) {
                allSamples = resample(allSamples, sampleRate, 44100, channelCount);
            }
            
            // Convert to stereo if needed
            if (channelCount == 1) {
                allSamples = monoToStereo(allSamples);
            } else if (channelCount > 2) {
                // Take first two channels
                allSamples = extractStereo(allSamples, channelCount);
            }
            
            Log.d(TAG, "Decoded " + allSamples.length + " samples");
            return allSamples;
            
        } finally {
            if (decoder != null) {
                try {
                    decoder.stop();
                    decoder.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing decoder", e);
                }
            }
            extractor.release();
        }
    }
    
    /**
     * Simple resampling (linear interpolation)
     * For production, use a proper resampling library
     */
    private static short[] resample(short[] input, int inputRate, int outputRate, int channels) {
        if (inputRate == outputRate) {
            return input;
        }
        
        double ratio = (double) outputRate / inputRate;
        int outputLength = (int) (input.length * ratio);
        short[] output = new short[outputLength];
        
        for (int i = 0; i < outputLength; i += channels) {
            double srcIndex = i / ratio;
            int srcIndexInt = (int) srcIndex;
            double fraction = srcIndex - srcIndexInt;
            
            if (srcIndexInt + channels < input.length) {
                for (int ch = 0; ch < channels; ch++) {
                    int idx = srcIndexInt + ch;
                    int nextIdx = Math.min(idx + channels, input.length - channels + ch);
                    output[i + ch] = (short) (input[idx] * (1 - fraction) + input[nextIdx] * fraction);
                }
            } else {
                for (int ch = 0; ch < channels; ch++) {
                    output[i + ch] = input[Math.min(srcIndexInt + ch, input.length - 1)];
                }
            }
        }
        
        return output;
    }
    
    /**
     * Convert mono to stereo by duplicating channels
     */
    private static short[] monoToStereo(short[] mono) {
        short[] stereo = new short[mono.length * 2];
        for (int i = 0; i < mono.length; i++) {
            stereo[i * 2] = mono[i];     // Left
            stereo[i * 2 + 1] = mono[i]; // Right
        }
        return stereo;
    }
    
    /**
     * Extract first two channels from multi-channel audio
     */
    private static short[] extractStereo(short[] multi, int channelCount) {
        int samples = multi.length / channelCount;
        short[] stereo = new short[samples * 2];
        for (int i = 0; i < samples; i++) {
            stereo[i * 2] = multi[i * channelCount];         // Left
            stereo[i * 2 + 1] = multi[i * channelCount + 1]; // Right
        }
        return stereo;
    }
}

