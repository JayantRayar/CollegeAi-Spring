
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
     * Finds the shortest walking route between two campus nodes.
     *
     * Example:
     * GET /api/campus/routes?startNodeId=1&endNodeId=5
     */
    @GetMapping
    public ResponseEntity<CampusRouteResponse> findShortestRoute(

            @RequestParam
            @Positive(message = "Starting node ID must be positive")
            Long startNodeId,

            @RequestParam
            @Positive(message = "Destination node ID must be positive")
            Long endNodeId
    ) {

        CampusRouteResponse response =
                campusRouteService.findShortestRoute(
                        startNodeId,
                        endNodeId
                );

        return ResponseEntity.ok(response);
    }
}
