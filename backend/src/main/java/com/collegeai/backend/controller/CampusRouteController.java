
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.CampusRouteResponse;
import com.collegeai.backend.service.CampusRouteService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campus/routes")
@RequiredArgsConstructor
@Validated
public class CampusRouteController {

    private final CampusRouteService campusRouteService;

    /*
     * Finds a route using internal map node IDs.
     *
     * Example:
     * GET /api/campus/routes?startNodeId=1&endNodeId=2
     */
    @GetMapping
    public ResponseEntity<CampusRouteResponse> findShortestRoute(

            @RequestParam(required = false)
            @Positive(message = "Starting node ID must be positive")
            Long startNodeId,

            @RequestParam(required = false)
            @Positive(message = "Destination node ID must be positive")
            Long endNodeId,

            @RequestParam(required = false)
            @Positive(message = "Starting location ID must be positive")
            Long startLocationId,

            @RequestParam(required = false)
            @Positive(message = "Destination location ID must be positive")
            Long endLocationId
    ) {

        boolean nodeIdsProvided =
                startNodeId != null && endNodeId != null;

        boolean locationIdsProvided =
                startLocationId != null && endLocationId != null;

        // Exactly one complete pair must be provided.
        if (nodeIdsProvided == locationIdsProvided
                || (startNodeId != null) != (endNodeId != null)
                || (startLocationId != null) != (endLocationId != null)) {

            throw new IllegalArgumentException(
                    "Provide either startNodeId and endNodeId "
                            + "or startLocationId and endLocationId."
            );
        }

        CampusRouteResponse response;

        if (locationIdsProvided) {

            response = campusRouteService.findShortestRouteByLocationIds(
                    startLocationId,
                    endLocationId
            );

        } else {

            response = campusRouteService.findShortestRoute(
                    startNodeId,
                    endNodeId
            );
        }

        return ResponseEntity.ok(response);
    }
}
