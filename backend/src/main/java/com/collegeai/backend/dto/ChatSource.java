package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatSource {

    private Long documentId;
    private String documentName;
    private Integer pageNumber;
    private Integer chunkNumber;
    private Double score;

    public static ChatSource from(RetrievedChunk chunk) {

        return new ChatSource(
                chunk.getDocumentId(),
                chunk.getDocumentName(),
                chunk.getPageNumber(),
                chunk.getChunkNumber(),
                chunk.getScore()
        );
    }
}