package com.collegeai.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapPathRequest {

    @NotNull(message = "Starting node ID is required")
    @Positive(message = "Starting node ID must be positive")
    private Long startNodeId;

    @NotNull(message = "Ending node ID is required")
    @Positive(message = "Ending node ID must be positive")
    private Long endNodeId;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "Distance must be greater than zero")
    private Double distanceMeters;

    @NotNull(message = "Bidirectional value is required")
    private Boolean bidirectional = true;
}