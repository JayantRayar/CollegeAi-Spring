
package com.collegeai.backend.voice;

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

    public String processQuestion(String question, String sessionId) {

        if (question == null || question.isBlank()) {
            return "I could not hear your question. Please repeat it.";
        }

        log.info("Processing voice question. Session: {}", sessionId);

        String answer = chatService.answerForVoice(question);

        log.info(
                "Voice answer generated. Session: {}",
                sessionId
        );

        return answer;
    }
}
