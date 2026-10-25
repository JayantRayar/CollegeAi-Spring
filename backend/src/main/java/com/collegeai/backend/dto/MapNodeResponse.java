package com.collegeai.backend.dto;

import com.collegeai.backend.entity.MapNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MapNodeResponse {

    private Long id;

    private String name;

    private String nodeType;

    private Double xCoordinate;

    private Double yCoordinate;

    private Integer floor;

    private Long campusLocationId;

    private String campusLocationName;

    // Converts the MapNode entity into a frontend-safe response.
    public static MapNodeResponse from(MapNode node) {

        return new MapNodeResponse(
                node.getId(),
                node.getName(),
                node.getNodeType(),
                node.getXCoordinate(),
                node.getYCoordinate(),
                node.getFloor(),
                node.getCampusLocation() != null
                        ? node.getCampusLocation().getId()
                        : null,
                node.getCampusLocation() != null
                        ? node.getCampusLocation().getName()
                        : null
        );
    }
}