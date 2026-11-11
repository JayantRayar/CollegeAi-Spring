
package com.collegeai.backend.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class VoiceProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(VoiceProcessingService.class);

    private final RestClient restClient;
    private final String sttUrl;
    private final String ttsUrl;

    public VoiceProcessingService(
            RestClient.Builder restClientBuilder,
            @Value("${voice.stt.url:http://127.0.0.1:8001/api/speech-to-text}")
            String sttUrl,
            @Value("${voice.tts.url:http://127.0.0.1:8001/api/text-to-speech/exotel}")
            String ttsUrl) {

        this.restClient = restClientBuilder.build();
        this.sttUrl = sttUrl;
        this.ttsUrl = ttsUrl;
    }

    // Speech-to-Text: Exotel PCM -> Python Groq Whisper
    public String processAudio(byte[] audioBytes, String sessionId) {

        if (audioBytes == null || audioBytes.length == 0) {
            log.warn("Received empty audio segment. Session: {}", sessionId);
            return "";
        }

        log.info(
                "Voice processing started. Session: {}, Audio bytes: {}",
                sessionId,
                audioBytes.length
        );

        byte[] wavAudio = convertPcmToWav(audioBytes);

        log.info(
                "WAV conversion successful. Original bytes: {}, WAV bytes: {}",
                audioBytes.length,
                wavAudio.length
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource audioResource = new ByteArrayResource(wavAudio) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        };

        body.add("file", audioResource);

        Map<String, Object> response = restClient.post()
                .uri(sttUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null
                || !Boolean.TRUE.equals(response.get("success"))) {
            throw new IllegalStateException(
                    "Python STT service returned an unsuccessful response"
            );
        }

        String transcribedText =
                (String) response.get("transcribedText");

        if (transcribedText == null || transcribedText.isBlank()) {
            log.warn("No speech detected. Session: {}", sessionId);
            return "";
        }

        log.info(
                "Transcription successful. Session: {}, Text: {}",
                sessionId,
                transcribedText
        );

        return transcribedText;
    }

    // Text-to-Speech: AI answer -> Python Gemini TTS -> raw 8 kHz PCM
    public byte[] generateSpeech(String text) {

        if (text == null || text.isBlank()) {
            log.warn("Cannot generate speech for empty text.");
            return new byte[0];
        }

        log.info("Generating speech. Text length: {}", text.length());

        byte[] audioBytes = restClient.post()
                .uri(ttsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .body(Map.of("text", text))
                .retrieve()
                .body(byte[].class);

        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalStateException(
                    "Python TTS service returned empty audio"
            );
        }

        log.info("Speech generated successfully. Audio bytes: {}", audioBytes.length);

        return audioBytes;
    }

    /*
     * Converts 16-bit, 8000 Hz, mono PCM audio into WAV format.
     */
    private byte[] convertPcmToWav(byte[] pcmData) {

        int sampleRate = 8000;
        int channels = 1;
        int bitsPerSample = 16;

        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        ByteBuffer buffer = ByteBuffer.allocate(44 + pcmData.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // RIFF header
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(36 + pcmData.length);
        buffer.put("WAVE".getBytes(StandardCharsets.US_ASCII));

        // fmt subchunk
        buffer.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);

        // data subchunk
        buffer.put("data".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(pcmData.length);
        buffer.put(pcmData);

        return buffer.array();
    }
}
