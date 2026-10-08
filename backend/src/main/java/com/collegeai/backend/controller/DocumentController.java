package com.collegeai.backend.controller;

import com.collegeai.backend.dto.DocumentChunk;
import com.collegeai.backend.dto.DocumentResponse;
import com.collegeai.backend.service.DocumentProcessingService;
import com.collegeai.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;


    @GetMapping
    public List<DocumentResponse> getAllDocuments() {

        return documentService.getAllDocuments();
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @RequestParam("file") MultipartFile file) {

        return documentService.uploadDocument(file);
    }


}