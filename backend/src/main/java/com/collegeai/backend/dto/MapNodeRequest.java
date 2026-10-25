package com.collegeai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapNodeRequest {

    @NotBlank(message = "Node name is required")
    @Size(max = 255, message = "Node name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "Node type is required")
    @Size(max = 50, message = "Node type cannot exceed 50 characters")
    private String nodeType;

    private Double xCoordinate;

    private Double yCoordinate;

    private Integer floor;

    @Positive(message = "Campus location ID must be positive")
    private Long campusLocationId;
}