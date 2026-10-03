package com.collegeai.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.collegeai.backend.exception.DocumentUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map uploadFile(MultipartFile file) {

        try {

            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "raw"
                    )
            );

        } catch (Exception e) {

            throw new DocumentUploadException(
                    "Failed to upload PDF to Cloudinary.",
                    e
            );
        }
    }
}