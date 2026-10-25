package com.collegeai.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampusLocationRequest {

    @NotBlank(message = "Location name is required")
    @Size(max = 255, message = "Location name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "Location category is required")
    @Size(max = 255, message = "Category cannot exceed 255 characters")
    private String category;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
    @DecimalMax(value = "90.0", message = "Latitude cannot exceed 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
    @DecimalMax(value = "180.0", message = "Longitude cannot exceed 180")
    private Double longitude;

    @Size(max = 255, message = "Building name cannot exceed 255 characters")
    private String building;

    private Integer floor;
}