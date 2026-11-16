
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.VoiceChatRequest;
import com.collegeai.backend.dto.VoiceChatResponse;
import com.collegeai.backend.voice.VoiceConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceConversationService voiceConversationService;

    @PostMapping("/chat")
    public ResponseEntity<VoiceChatResponse> chat(
            @Valid @RequestBody VoiceChatRequest request) {

        VoiceChatResponse response =
                voiceConversationService.processQuestion(
                        request.getQuestion()
                );

        return ResponseEntity.ok(response);
    }
}
