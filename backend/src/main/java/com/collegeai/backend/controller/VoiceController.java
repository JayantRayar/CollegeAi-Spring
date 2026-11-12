
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.VoiceChatRequest;
import com.collegeai.backend.dto.VoiceChatResponse;
import com.collegeai.backend.voice.VoiceConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceConversationService voiceConversationService;

    @PostMapping("/chat")
    public ResponseEntity<VoiceChatResponse> chat(
            @Valid @RequestBody VoiceChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        VoiceChatResponse response =
                voiceConversationService.processQuestion(
                        request.getQuestion(),
                        jwt.getSubject()
                );

        return ResponseEntity.ok(response);
    }
}
