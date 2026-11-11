package com.collegeai.backend.voice;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
@Component
@Slf4j
public class VoiceActivityDetector {

    private static final int SILENCE_THRESHOLD = 500;

    public boolean isSpeech(byte[] audioBytes) {

        if (audioBytes == null || audioBytes.length < 2) {
            return false;
        }

        long sumSquares = 0;
        int sampleCount = audioBytes.length / 2;

        for (int i = 0; i < audioBytes.length - 1; i += 2) {

            int sample = (audioBytes[i + 1] << 8)
                    | (audioBytes[i] & 0xFF);

            short pcmSample = (short) sample;

            sumSquares += (long) pcmSample * pcmSample;
        }

        double rms = Math.sqrt((double) sumSquares / sampleCount);

        log.info("Incoming audio RMS: {}", rms);

        return rms >= SILENCE_THRESHOLD;
    }
}