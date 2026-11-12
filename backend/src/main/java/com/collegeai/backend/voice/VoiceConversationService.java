
package com.collegeai.backend.voice;

import com.collegeai.backend.dto.VoiceChatResponse;
import com.collegeai.backend.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VoiceConversationService {

    private static final Logger log =
            LoggerFactory.getLogger(VoiceConversationService.class);

    private final ChatService chatService;

    public VoiceConversationService(ChatService chatService) {
        this.chatService = chatService;
    }

    public VoiceChatResponse processQuestion(
            String question,
            String sessionId) {

        // Validate the incoming voice transcription.
        if (question == null || question.isBlank()) {
            return new VoiceChatResponse(
                    "I could not hear your question. Please repeat it.",
                    "ERROR"
            );
        }

        // Log the voice session without exposing the user's question.
        log.info(
                "Processing voice question. Session: {}",
                sessionId
        );

        // Reuse the existing PDF-first RAG and Gemini fallback logic.
        VoiceChatResponse response =
                chatService.answerForVoice(question);

        // Log the response source.
        log.info(
                "Voice answer generated. Session: {}, Source: {}",
                sessionId,
                response.getAnswerType()
        );

        return response;
    }
}
