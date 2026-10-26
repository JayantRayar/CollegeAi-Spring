
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.MapPathResponse;
import com.collegeai.backend.service.MapPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campus/map-paths")
@RequiredArgsConstructor
public class CampusMapPathQueryController {

    private final MapPathService mapPathService;

    // Retrieve all connections between campus map nodes.
    @GetMapping
    public List<MapPathResponse> getAllPaths() {
        return mapPathService.getAllPaths();
    }

    // Retrieve connections where both nodes belong to a particular floor.
    @GetMapping("/floor/{floor}")
    public List<MapPathResponse> getPathsByFloor(
            @PathVariable Integer floor) {

        return mapPathService.getPathsByFloor(floor);
    }

    // Retrieve one connection by its ID.
    @GetMapping("/{id}")
    public MapPathResponse getPathById(@PathVariable Long id) {
        return mapPathService.getPathById(id);
    }
}
