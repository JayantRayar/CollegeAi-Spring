package com.collegeai.backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudinary configuration.
 *
 * Creates the Cloudinary client using the CLOUDINARY_URL
 * environment variable configured in IntelliJ.
 */
@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {

        String cloudinaryUrl = System.getenv("CLOUDINARY_URL");

        return new Cloudinary(cloudinaryUrl);
    }
}