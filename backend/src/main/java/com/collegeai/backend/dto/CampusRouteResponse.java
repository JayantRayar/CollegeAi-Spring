
package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CampusRouteResponse {

    private Long startLocationId;

    private Long endLocationId;

    private Long startNodeId;

    private Long endNodeId;

    private Double totalDistanceMeters;

    private List<MapNodeResponse> route;

    // Step-by-step navigation instructions.
    private List<RouteInstructionResponse> instructions;
}
