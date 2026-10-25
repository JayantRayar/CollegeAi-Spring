package com.collegeai.backend.dto;

import com.collegeai.backend.entity.MapPath;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MapPathResponse {

    private Long id;

    private Long startNodeId;

    private String startNodeName;

    private Long endNodeId;

    private String endNodeName;

    private Double distanceMeters;

    private Boolean bidirectional;

    // Converts the MapPath entity into a frontend-safe response.
    public static MapPathResponse from(MapPath path) {

        return new MapPathResponse(
                path.getId(),
                path.getStartNode().getId(),
                path.getStartNode().getName(),
                path.getEndNode().getId(),
                path.getEndNode().getName(),
                path.getDistanceMeters(),
                path.getBidirectional()
        );
    }
}