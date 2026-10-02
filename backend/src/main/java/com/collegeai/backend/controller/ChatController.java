package com.collegeai.backend.controller;

import com.collegeai.backend.dto.ChatRequest;
import com.collegeai.backend.dto.ChatResponse;
import com.collegeai.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request) {

        ChatResponse response =
                chatService.chat(request);

        return ResponseEntity.ok(response);
    }
}