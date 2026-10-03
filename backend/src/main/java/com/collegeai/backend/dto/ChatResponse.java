package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatResponse {

    private String answer;

    private String answerType;

    private List<ChatSource> sources;
}