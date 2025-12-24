package com.mixapp;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * MP3Decoder handles decoding MP3 files to PCM format using Android's MediaCodec API.
 * This provides efficient, hardware-accelerated decoding when available.
 * Now supports streaming directly to disk to avoid memory issues with large files.
 */
public class MP3Decoder {
    private static final String TAG = "MP3Decoder";
    
    /**
     * Decode an audio file (MP3, WAV, etc.) to 16-bit PCM stereo at 44.1kHz
     * @param file The audio file to decode
     * @return Array of 16-bit PCM samples (interleaved stereo)
     * @throws IOException If decoding fails
     * @deprecated Use decodeAudioToFile() instead to avoid memory issues
     */
    @Deprecated
    public static short[] decodeAudio(File file) throws IOException {
        return decodeMP3(file);
    }
    
    /**
     * Decode an audio file directly to disk (streaming, low memory usage)
     * @param file The audio file to decode
     * @param outputFile Where to write the PCM data
     * @return Sample count and sample rate info
     * @throws IOException If decoding fails
     */
    public static DecodeResult decodeAudioToFile(File file, File outputFile) throws IOException {
        return decodeMP3ToFile(file, outputFile);
    }
    
    /**
     * Result of decoding to file
     */
    public static class DecodeResult {
        public long sampleCount;
        public int sampleRate;
        public int channels;
        
        public DecodeResult(long sampleCount, int sampleRate, int channels) {
            this.sampleCount = sampleCount;
            this.sampleRate = sampleRate;
            this.channels = channels;
        }
    }
    
    /**
     * Decode an MP3 file directly to disk (streaming, low memory)
     * @param file The MP3 file to decode
     * @param outputFile Where to write the PCM data (16-bit stereo at 44.1kHz)
     * @return DecodeResult with sample count and format info
     * @throws IOException If decoding fails
     */
    public static DecodeResult decodeMP3ToFile(File file, File outputFile) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        FileOutputStream fos = null;
        File tempFile = null;
        
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
            
            Log.d(TAG, "Decoding MP3 to file: sampleRate=" + sampleRate + ", channels=" + channelCount);
            
            // Create decoder
            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, null, null, 0);
            decoder.start();
            
            // Create temp file if we need resampling/conversion
            boolean needsProcessing = (sampleRate != 44100) || (channelCount != 2);
            if (needsProcessing) {
                tempFile = new File(outputFile.getParentFile(), outputFile.getName() + ".tmp");
                fos = new FileOutputStream(tempFile);
            } else {
                // Write directly to output file
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                fos = new FileOutputStream(outputFile);
            }
            
            // Decode and write chunks directly to file
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long totalSamplesWritten = 0;
            ByteBuffer writeBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
            
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
                        // Write directly to file (little-endian 16-bit samples)
                        outputBuffer.position(info.offset);
                        outputBuffer.limit(info.offset + info.size);
                        
                        // Write bytes directly (they're already in the right format)
                        byte[] chunk = new byte[info.size];
                        outputBuffer.get(chunk);
                        fos.write(chunk);
                        
                        totalSamplesWritten += (info.size / 2); // 2 bytes per sample
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
            
            fos.close();
            fos = null;
            
            // Calculate actual sample count (divide by channels)
            long totalSamples = totalSamplesWritten / channelCount;
            
            // Process temp file if needed (resample/convert channels)
            if (needsProcessing && tempFile != null) {
                Log.d(TAG, "Processing: resample=" + (sampleRate != 44100) + ", convertChannels=" + (channelCount != 2));
                processAudioFile(tempFile, outputFile, sampleRate, 44100, channelCount, 2, totalSamples);
                tempFile.delete();
            }
            
            // Calculate final sample count (stereo at 44.1kHz)
            long finalSampleCount = totalSamples;
            if (sampleRate != 44100) {
                // Resampling changes sample count
                finalSampleCount = (totalSamples * 44100L) / sampleRate;
            }
            
            Log.d(TAG, "Decoded to file: " + totalSamples + " samples -> " + finalSampleCount + " samples (stereo @ 44.1kHz)");
            
            return new DecodeResult(finalSampleCount, 44100, 2);
            
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing file", e);
                }
            }
            if (decoder != null) {
                try {
                    decoder.stop();
                    decoder.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing decoder", e);
                }
            }
            extractor.release();
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    /**
     * Process audio file: resample and/or convert channels (streaming, low memory)
     */
    private static void processAudioFile(File inputFile, File outputFile, 
                                        int inputRate, int outputRate,
                                        int inputChannels, int outputChannels,
                                        long inputSampleCount) throws IOException {
        FileInputStream fis = new FileInputStream(inputFile);
        FileOutputStream fos = new FileOutputStream(outputFile);
        
        try {
            // Buffer for reading (about 1 second of audio)
            int bufferSize = (int)(inputRate * inputChannels * 2); // 1 second in bytes
            byte[] inputBuffer = new byte[bufferSize];
            ByteBuffer inputByteBuffer = ByteBuffer.wrap(inputBuffer).order(ByteOrder.LITTLE_ENDIAN);
            
            // Buffer for output
            int outputBufferSize = (int)(outputRate * outputChannels * 2); // 1 second in bytes
            byte[] outputBuffer = new byte[outputBufferSize];
            ByteBuffer outputByteBuffer = ByteBuffer.wrap(outputBuffer).order(ByteOrder.LITTLE_ENDIAN);
            
            long samplesRead = 0;
            
            while (samplesRead < inputSampleCount) {
                // Read chunk
                int bytesRead = fis.read(inputBuffer);
                if (bytesRead <= 0) break;
                
                // Convert to shorts
                int samplesInChunk = bytesRead / (2 * inputChannels);
                short[] inputSamples = new short[samplesInChunk * inputChannels];
                inputByteBuffer.position(0);
                inputByteBuffer.limit(bytesRead);
                inputByteBuffer.asShortBuffer().get(inputSamples);
                
                // Process: resample and/or convert channels
                short[] outputSamples;
                if (inputRate != outputRate) {
                    // Resample first
                    outputSamples = resample(inputSamples, inputRate, outputRate, inputChannels);
                } else {
                    outputSamples = inputSamples;
                }
                
                // Convert channels if needed
                if (inputChannels != outputChannels) {
                    if (inputChannels == 1 && outputChannels == 2) {
                        outputSamples = monoToStereo(outputSamples);
                    } else if (inputChannels > 2 && outputChannels == 2) {
                        outputSamples = extractStereo(outputSamples, inputChannels);
                    }
                }
                
                // Write to file
                outputByteBuffer.clear();
                for (short sample : outputSamples) {
                    fos.write((byte)(sample & 0xFF));
                    fos.write((byte)((sample >> 8) & 0xFF));
                }
                
                samplesRead += samplesInChunk;
            }
            
        } finally {
            fis.close();
            fos.close();
        }
    }
    
    /**
     * Decode an MP3 file to 16-bit PCM stereo at 44.1kHz (old method, loads into memory)
     * @param file The MP3 file to decode
     * @return Array of 16-bit PCM samples (interleaved stereo)
     * @throws IOException If decoding fails
     * @deprecated Use decodeMP3ToFile() instead
     */
    @Deprecated
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
            
            Log.d(TAG, "Total samples to allocate: " + totalSamples + " (" + 
                  (totalSamples * 2L / (1024 * 1024)) + " MB)");
            
            // Check if we're about to allocate too much memory
            long estimatedMemoryMB = (totalSamples * 2L) / (1024 * 1024);
            if (estimatedMemoryMB > 200) {
                Log.w(TAG, "Large memory allocation: " + estimatedMemoryMB + " MB");
            }
            
            short[] allSamples;
            try {
                allSamples = new short[totalSamples];
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory allocating PCM array: " + totalSamples + " samples (" + 
                      estimatedMemoryMB + " MB)", e);
                // Clear chunks to free memory before throwing
                decodedChunks.clear();
                System.gc();
                throw new IOException("File is too large. Out of memory while decoding. Please use a smaller file.");
            }
            
            int offset = 0;
            for (short[] chunk : decodedChunks) {
                System.arraycopy(chunk, 0, allSamples, offset, chunk.length);
                offset += chunk.length;
            }
            
            // Clear chunks to free memory
            decodedChunks.clear();
            
            // Resample to 44.1kHz if needed
            if (sampleRate != 44100) {
                try {
                    allSamples = resample(allSamples, sampleRate, 44100, channelCount);
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory during resampling", e);
                    System.gc();
                    throw new IOException("File is too large. Out of memory during resampling. Please use a smaller file.");
                }
            }
            
            // Convert to stereo if needed
            if (channelCount == 1) {
                try {
                    allSamples = monoToStereo(allSamples);
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory during mono to stereo conversion", e);
                    System.gc();
                    throw new IOException("File is too large. Out of memory during conversion. Please use a smaller file.");
                }
            } else if (channelCount > 2) {
                // Take first two channels
                try {
                    allSamples = extractStereo(allSamples, channelCount);
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory during stereo extraction", e);
                    System.gc();
                    throw new IOException("File is too large. Out of memory during conversion. Please use a smaller file.");
                }
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

