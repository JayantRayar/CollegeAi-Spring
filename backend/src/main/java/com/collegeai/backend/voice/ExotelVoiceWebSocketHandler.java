
package com.collegeai.backend.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class ExotelVoiceWebSocketHandler extends TextWebSocketHandler {

    private static final String STREAM_SID = "streamSid";
    private static final String CALL_SID = "callSid";
    private static final String CALLER_NUMBER = "callerNumber";
    private static final String SAMPLE_RATE = "sampleRate";

    // Creates a Jackson ObjectMapper for parsing Exotel JSON messages.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Exotel WebSocket connected. Session ID: {}",
                session.getId());
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());

            String event = root.path("event").asText("");

            switch (event) {
                case "connected" ->
                        log.info("Exotel stream connection established.");

                case "start" ->
                        handleStartEvent(session, root);

                case "media" ->
                        log.debug("Received audio packet.");

                case "dtmf" ->
                        log.info("Received DTMF event.");

                case "mark" ->
                        log.debug("Received audio playback mark.");

                case "stop" -> {
                    log.info("Exotel stream stopped.");
                    clearSessionAttributes(session);
                }

                default ->
                        log.warn("Unknown Exotel event received: {}", event);
            }

        } catch (Exception exception) {
            log.error("Failed to process Exotel WebSocket message.",
                    exception);
        }
    }

    private void handleStartEvent(
            WebSocketSession session,
            JsonNode root
    ) {
        JsonNode start = root.path("start");
        JsonNode mediaFormat = start.path("media_format");

        String streamSid = start.path("stream_sid")
                .asText(root.path("stream_sid").asText(""));

        String callSid = start.path("call_sid").asText("");
        String callerNumber = start.path("from").asText("");
        String sampleRate = mediaFormat.path("sample_rate").asText("");

        session.getAttributes().put(STREAM_SID, streamSid);
        session.getAttributes().put(CALL_SID, callSid);
        session.getAttributes().put(CALLER_NUMBER, callerNumber);
        session.getAttributes().put(SAMPLE_RATE, sampleRate);

        log.info(
                "Exotel call started. Stream ID: {}, Call ID: {}, Sample Rate: {}",
                streamSid,
                callSid,
                sampleRate
        );
    }

    private void clearSessionAttributes(WebSocketSession session) {
        session.getAttributes().remove(STREAM_SID);
        session.getAttributes().remove(CALL_SID);
        session.getAttributes().remove(CALLER_NUMBER);
        session.getAttributes().remove(SAMPLE_RATE);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        clearSessionAttributes(session);

        log.info("Exotel WebSocket disconnected. Session ID: {}, Status: {}",
                session.getId(), status);
    }
}
