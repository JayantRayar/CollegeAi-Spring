package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentChunk {

    private final String text;
    private final int pageNumber;
    private final int chunkNumber;
}