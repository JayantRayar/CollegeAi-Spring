
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.ChatRequest;
import com.collegeai.backend.dto.ChatResponse;
import com.collegeai.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.collegeai.backend.dto.ChatMessageResponse;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        ChatResponse response = chatService.chat(
                request,
                jwt.getSubject()
        );

        return ResponseEntity.ok(response);
    }
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getConversationMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Jwt jwt) {

        List<ChatMessageResponse> messages = chatService.getConversationMessages(
                conversationId,
                jwt.getSubject()
        );

        return ResponseEntity.ok(messages);
    }
}
