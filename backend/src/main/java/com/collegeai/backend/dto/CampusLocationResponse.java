package com.collegeai.backend.dto;

import com.collegeai.backend.entity.CampusLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CampusLocationResponse {

    private Long id;
    private String name;
    private String category;
    private String description;
    private Double latitude;
    private Double longitude;
    private String building;
    private Integer floor;
    private LocalDateTime createdAt;

    // Converts the database entity into a frontend-safe response.
    public static CampusLocationResponse from(CampusLocation location) {

        return new CampusLocationResponse(
                location.getId(),
                location.getName(),
                location.getCategory(),
                location.getDescription(),
                location.getLatitude(),
                location.getLongitude(),
                location.getBuilding(),
                location.getFloor(),
                location.getCreatedAt()
        );
    }
}