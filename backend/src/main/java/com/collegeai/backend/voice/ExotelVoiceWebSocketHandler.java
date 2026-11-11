
package com.collegeai.backend.voice;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
public class ExotelVoiceWebSocketHandler extends TextWebSocketHandler {

    private static final String STREAM_SID = "streamSid";
    private static final String CALL_SID = "callSid";
    private static final String CALLER_NUMBER = "callerNumber";
    private static final String SAMPLE_RATE = "sampleRate";

    private static final String AUDIO_BUFFER = "audioBuffer";
    private static final String PRE_ROLL_BUFFER = "preRollBuffer";
    private static final String SPEECH_ACTIVE = "speechActive";
    private static final String SILENCE_BYTES = "silenceBytes";

    // Maximum memory allowed for one utterance.
    private static final int MAX_AUDIO_BUFFER_BYTES = 1024 * 1024;

    // Exotel audio format: 8000 Hz, 16-bit, mono PCM.
    private static final int PRE_ROLL_MAX_BYTES = 3200;
    private static final int SILENCE_DURATION_BYTES = 12800;
    private static final int MIN_UTTERANCE_BYTES = 3200;

    // Exotel outgoing audio chunk size: 3200 bytes = 100 ms.
    private static final int OUTGOING_AUDIO_CHUNK_BYTES = 3200;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final VoiceProcessingService voiceProcessingService;
    private final VoiceActivityDetector voiceActivityDetector;
    private final VoiceConversationService voiceConversationService;

    public ExotelVoiceWebSocketHandler(
            VoiceProcessingService voiceProcessingService,
            VoiceActivityDetector voiceActivityDetector,
            VoiceConversationService voiceConversationService) {

        this.voiceProcessingService = voiceProcessingService;
        this.voiceActivityDetector = voiceActivityDetector;
        this.voiceConversationService = voiceConversationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        session.setTextMessageSizeLimit(256 * 1024);

        log.info(
                "Exotel WebSocket connected. Session ID: {}",
                session.getId()
        );
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) {

        try {
            JsonNode root = objectMapper.readTree(message.getPayload());

            String event = root.path("event").asText("");

            switch (event) {

                case "connected" ->
                        log.info("Exotel stream connection established.");

                case "start" ->
                        handleStartEvent(session, root);

                case "media" ->
                        handleMediaEvent(session, root);

                case "dtmf" ->
                        log.info("Received DTMF event.");

                case "mark" ->
                        log.info(
                                "Received Exotel playback mark: {}",
                                root.toPrettyString()
                        );

                case "stop" ->
                        handleStopEvent(session, root);

                default ->
                        log.warn(
                                "Unknown Exotel event received: {}",
                                event
                        );
            }

        } catch (Exception exception) {

            log.error(
                    "Failed to process Exotel WebSocket message.",
                    exception
            );
        }
    }

    private void handleStartEvent(
            WebSocketSession session,
            JsonNode root) {

        JsonNode start = root.path("start");
        JsonNode mediaFormat = start.path("media_format");

        log.info(
                "EXOTEL START EVENT: {}",
                root.toPrettyString()
        );

        String streamSid = start.path("stream_sid")
                .asText(root.path("stream_sid").asText(""));

        String callSid = start.path("call_sid").asText("");
        String callerNumber = start.path("from").asText("");

        String sampleRate =
                mediaFormat.path("sample_rate").asText("");

        String encoding =
                mediaFormat.path("encoding").asText("");

        log.info(
                "Exotel audio format - Encoding: {}, Sample Rate: {}",
                encoding,
                sampleRate
        );

        session.getAttributes().put(STREAM_SID, streamSid);
        session.getAttributes().put(CALL_SID, callSid);
        session.getAttributes().put(CALLER_NUMBER, callerNumber);
        session.getAttributes().put(SAMPLE_RATE, sampleRate);

        session.getAttributes().put(
                AUDIO_BUFFER,
                new ByteArrayOutputStream()
        );

        session.getAttributes().put(
                PRE_ROLL_BUFFER,
                new ByteArrayOutputStream()
        );

        session.getAttributes().put(SPEECH_ACTIVE, false);
        session.getAttributes().put(SILENCE_BYTES, 0);

        log.info(
                "Exotel call started. Stream ID: {}, Call ID: {}, Sample Rate: {}",
                streamSid,
                callSid,
                sampleRate
        );
    }

    private void handleMediaEvent(
            WebSocketSession session,
            JsonNode root) {

        JsonNode media = root.path("media");

        String payload = media.path("payload").asText("");

        if (payload.isBlank()) {

            log.warn(
                    "Received media event without an audio payload."
            );

            return;
        }

        try {

            byte[] audioBytes =
                    Base64.getDecoder().decode(payload);

            log.debug(
                    "EXOTEL MEDIA RECEIVED: Packet bytes: {}, Session: {}",
                    audioBytes.length,
                    session.getId()
            );

            ByteArrayOutputStream audioBuffer =
                    (ByteArrayOutputStream) session.getAttributes()
                            .get(AUDIO_BUFFER);

            ByteArrayOutputStream preRollBuffer =
                    (ByteArrayOutputStream) session.getAttributes()
                            .get(PRE_ROLL_BUFFER);

            if (audioBuffer == null || preRollBuffer == null) {

                log.warn(
                        "Audio received before call initialization."
                );

                return;
            }

            byte[] completedUtterance = null;

            synchronized (audioBuffer) {

                boolean speechDetected =
                        voiceActivityDetector.isSpeech(audioBytes);

                boolean speechActive =
                        Boolean.TRUE.equals(
                                session.getAttributes().get(SPEECH_ACTIVE)
                        );

                int silenceBytes =
                        (Integer) session.getAttributes()
                                .get(SILENCE_BYTES);

                if (!speechActive) {

                    if (speechDetected) {

                        // Preserve audio immediately before speech begins.
                        audioBuffer.write(
                                preRollBuffer.toByteArray()
                        );

                        preRollBuffer.reset();

                        audioBuffer.write(audioBytes);

                        session.getAttributes().put(
                                SPEECH_ACTIVE,
                                true
                        );

                        session.getAttributes().put(
                                SILENCE_BYTES,
                                0
                        );

                        log.info(
                                "Speech started. Session: {}",
                                session.getId()
                        );

                    } else {

                        // Keep only the most recent 200 ms.
                        preRollBuffer.write(audioBytes);

                        trimPreRollBuffer(preRollBuffer);
                    }

                } else {

                    if (audioBuffer.size() + audioBytes.length
                            > MAX_AUDIO_BUFFER_BYTES) {

                        log.warn(
                                "Utterance exceeded buffer limit. Resetting. Session: {}",
                                session.getId()
                        );

                        audioBuffer.reset();
                        preRollBuffer.reset();

                        session.getAttributes().put(
                                SPEECH_ACTIVE,
                                false
                        );

                        session.getAttributes().put(
                                SILENCE_BYTES,
                                0
                        );

                        return;
                    }

                    // Continue collecting the current utterance.
                    audioBuffer.write(audioBytes);

                    if (speechDetected) {
                        silenceBytes = 0;
                    } else {
                        silenceBytes += audioBytes.length;
                    }

                    session.getAttributes().put(
                            SILENCE_BYTES,
                            silenceBytes
                    );

                    // End utterance after 800 ms of silence.
                    if (silenceBytes >= SILENCE_DURATION_BYTES) {

                        byte[] utterance =
                                audioBuffer.toByteArray();

                        audioBuffer.reset();
                        preRollBuffer.reset();

                        session.getAttributes().put(
                                SPEECH_ACTIVE,
                                false
                        );

                        session.getAttributes().put(
                                SILENCE_BYTES,
                                0
                        );

                        if (utterance.length >= MIN_UTTERANCE_BYTES) {

                            completedUtterance = utterance;

                        } else {

                            log.info(
                                    "Ignoring very short utterance. Bytes: {}",
                                    utterance.length
                            );
                        }
                    }
                }
            }

            // Process completed caller utterance.
            if (completedUtterance != null) {

                processCallerUtterance(
                        session,
                        completedUtterance
                );
            }

        } catch (IllegalArgumentException exception) {

            log.error(
                    "Invalid Base64 audio payload received.",
                    exception
            );

        } catch (IOException exception) {

            log.error(
                    "Failed to append audio packet.",
                    exception
            );
        }
    }

    private void processCallerUtterance(
            WebSocketSession session,
            byte[] audioBytes) {

        log.info(
                "Processing caller utterance. Session: {}, Bytes: {}",
                session.getId(),
                audioBytes.length
        );

        try {
            // Send immediate acknowledgement before slow AI processing.
            byte[] acknowledgementAudio =
                    new ClassPathResource("audio/acknowledgement.pcm")
                            .getInputStream()
                            .readAllBytes();

            sendAudioToExotel(
                    session,
                    acknowledgementAudio
            );

            log.info(
                    "Acknowledgement audio sent. Session: {}",
                    session.getId()
            );

            // Step 1: Speech-to-Text using Python + Groq Whisper.
            String transcribedText =
                    voiceProcessingService.processAudio(
                            audioBytes,
                            session.getId()
                    );

            if (transcribedText == null
                    || transcribedText.isBlank()) {

                log.warn(
                        "No speech transcription received. Session: {}",
                        session.getId()
                );

                return;
            }

            log.info(
                    "CALLER TRANSCRIPTION: Session: {}, Text: {}",
                    session.getId(),
                    transcribedText
            );

            // Step 2: Existing college RAG conversation flow.
            String answer =
                    voiceConversationService.processQuestion(
                            transcribedText,
                            session.getId()
                    );

            if (answer == null || answer.isBlank()) {

                log.warn(
                        "AI returned an empty answer. Session: {}",
                        session.getId()
                );

                return;
            }

            log.info(
                    "AI VOICE ANSWER: Session: {}, Answer: {}",
                    session.getId(),
                    answer
            );

            // Step 3: Text-to-Speech using Python + Gemini.
            byte[] responseAudio =
                    voiceProcessingService.generateSpeech(answer);

            if (responseAudio == null || responseAudio.length == 0) {

                log.warn(
                        "TTS returned empty audio. Session: {}",
                        session.getId()
                );

                return;
            }

            log.info(
                    "TTS audio generated. Session: {}, Bytes: {}",
                    session.getId(),
                    responseAudio.length
            );

            // Step 4: Send generated PCM audio to Exotel.
            sendAudioToExotel(
                    session,
                    responseAudio
            );

        } catch (Exception exception) {

            log.error(
                    "Failed to process caller utterance. Session: {}",
                    session.getId(),
                    exception
            );
        }
    }

    private void sendAudioToExotel(
            WebSocketSession session,
            byte[] pcmAudio) throws IOException {

        if (pcmAudio == null || pcmAudio.length == 0) {

            log.warn("Cannot send empty audio to Exotel.");

            return;
        }

        if (!session.isOpen()) {

            log.warn(
                    "Cannot send audio because WebSocket is closed. Session: {}",
                    session.getId()
            );

            return;
        }

        String streamSid =
                (String) session.getAttributes().get(STREAM_SID);

        if (streamSid == null || streamSid.isBlank()) {

            log.warn(
                    "Cannot send audio because stream SID is missing."
            );

            return;
        }

        if (pcmAudio.length % 2 != 0) {

            throw new IllegalArgumentException(
                    "PCM audio must contain an even number of bytes."
            );
        }

        log.info(
                "Sending audio to Exotel. Session: {}, Total bytes: {}",
                session.getId(),
                pcmAudio.length
        );

        int offset = 0;

        while (offset < pcmAudio.length) {

            if (!session.isOpen()) {

                log.warn(
                        "WebSocket closed while sending audio. Session: {}",
                        session.getId()
                );

                break;
            }

            int remaining = pcmAudio.length - offset;

            int chunkLength = Math.min(
                    OUTGOING_AUDIO_CHUNK_BYTES,
                    remaining
            );

            // Exotel expects chunks of at least 3200 bytes.
            // Pad the final chunk with silence if required.
            byte[] chunk =
                    new byte[OUTGOING_AUDIO_CHUNK_BYTES];

            System.arraycopy(
                    pcmAudio,
                    offset,
                    chunk,
                    0,
                    chunkLength
            );

            String encodedAudio =
                    Base64.getEncoder().encodeToString(chunk);

            ObjectNode event =
                    objectMapper.createObjectNode();

            event.put("event", "media");
            event.put("stream_sid", streamSid);

            ObjectNode media =
                    event.putObject("media");

            media.put("payload", encodedAudio);

            session.sendMessage(
                    new TextMessage(
                            objectMapper.writeValueAsString(event)
                    )
            );

            offset += chunkLength;
        }

        log.info(
                "Finished sending audio to Exotel. Session: {}",
                session.getId()
        );
    }

    private void trimPreRollBuffer(
            ByteArrayOutputStream preRollBuffer) {

        if (preRollBuffer.size() <= PRE_ROLL_MAX_BYTES) {
            return;
        }

        byte[] currentBytes =
                preRollBuffer.toByteArray();

        int startIndex =
                currentBytes.length - PRE_ROLL_MAX_BYTES;

        preRollBuffer.reset();

        preRollBuffer.write(
                currentBytes,
                startIndex,
                PRE_ROLL_MAX_BYTES
        );
    }

    private void handleStopEvent(
            WebSocketSession session,
            JsonNode root) {

        // Log the complete event to investigate why Exotel stops the stream.
        log.info(
                "EXOTEL STOP EVENT: {}",
                root.toPrettyString()
        );

        log.info("Exotel stream stopped.");

        ByteArrayOutputStream audioBuffer =
                (ByteArrayOutputStream) session.getAttributes()
                        .get(AUDIO_BUFFER);

        if (audioBuffer != null) {

            synchronized (audioBuffer) {

                boolean speechActive =
                        Boolean.TRUE.equals(
                                session.getAttributes().get(SPEECH_ACTIVE)
                        );

                byte[] remainingAudio =
                        audioBuffer.toByteArray();

                if (speechActive
                        && remainingAudio.length >= MIN_UTTERANCE_BYTES) {

                    log.info(
                            "Processing final utterance on stream stop. Bytes: {}",
                            remainingAudio.length
                    );

                    // Do not attempt TTS here. The stop event indicates
                    // that the media stream is ending.
                    // We first need to establish why Exotel sends stop.
                    try {

                        String transcribedText =
                                voiceProcessingService.processAudio(
                                        remainingAudio,
                                        session.getId()
                                );

                        if (transcribedText != null
                                && !transcribedText.isBlank()) {

                            log.info(
                                    "FINAL CALLER TRANSCRIPTION: Session: {}, Text: {}",
                                    session.getId(),
                                    transcribedText
                            );

                            String answer =
                                    voiceConversationService.processQuestion(
                                            transcribedText,
                                            session.getId()
                                    );

                            log.info(
                                    "FINAL AI VOICE ANSWER: Session: {}, Answer: {}",
                                    session.getId(),
                                    answer
                            );
                        }

                    } catch (Exception exception) {

                        log.error(
                                "Failed to process final caller utterance. Session: {}",
                                session.getId(),
                                exception
                        );
                    }
                }
            }
        }

        clearSessionAttributes(session);
    }

    private void clearSessionAttributes(
            WebSocketSession session) {

        session.getAttributes().remove(STREAM_SID);
        session.getAttributes().remove(CALL_SID);
        session.getAttributes().remove(CALLER_NUMBER);
        session.getAttributes().remove(SAMPLE_RATE);

        session.getAttributes().remove(AUDIO_BUFFER);
        session.getAttributes().remove(PRE_ROLL_BUFFER);
        session.getAttributes().remove(SPEECH_ACTIVE);
        session.getAttributes().remove(SILENCE_BYTES);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        clearSessionAttributes(session);

        log.info(
                "Exotel WebSocket disconnected. Session ID: {}, Status: {}",
                session.getId(),
                status
        );
    }
}
