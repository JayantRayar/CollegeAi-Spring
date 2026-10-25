
package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CampusRouteResponse {

    // Requested campus location IDs.
    private Long startLocationId;

    private Long endLocationId;

    // Actual map nodes selected by the routing algorithm.
    private Long startNodeId;

    private Long endNodeId;

    private Double totalDistanceMeters;

    // Ordered nodes from starting point to destination.
    private List<MapNodeResponse> route;
}
