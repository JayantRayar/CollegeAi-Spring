
package com.collegeai.backend.voice;

import com.collegeai.backend.dto.VoiceChatResponse;
import com.collegeai.backend.service.ChatService;
import org.springframework.stereotype.Service;

@Service
public class VoiceConversationService {

    private final ChatService chatService;

    public VoiceConversationService(ChatService chatService) {
        this.chatService = chatService;
    }

    public VoiceChatResponse processQuestion(String question) {

        if (question == null || question.isBlank()) {
            return new VoiceChatResponse(
                    "I could not hear your question. Please repeat it.",
                    "ERROR"
            );
        }

        // Reuse the existing PDF-first RAG and Gemini fallback logic.
        return chatService.answerForVoice(question);
    }
}
