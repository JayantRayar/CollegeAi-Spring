package com.collegeai.backend.controller;

import com.collegeai.backend.dto.MapPathRequest;
import com.collegeai.backend.dto.MapPathResponse;
import com.collegeai.backend.service.MapPathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/map-paths")
@RequiredArgsConstructor
public class MapPathController {

    private final MapPathService mapPathService;

    // Create a connection between two map nodes.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MapPathResponse createPath(
            @Valid @RequestBody MapPathRequest request) {

        return mapPathService.createPath(request);
    }

    // Retrieve all map paths.
    @GetMapping
    public List<MapPathResponse> getAllPaths() {

        return mapPathService.getAllPaths();
    }

    // Retrieve a map path by ID.
    @GetMapping("/{id}")
    public MapPathResponse getPathById(
            @PathVariable Long id) {

        return mapPathService.getPathById(id);
    }

    // Update an existing map path.
    @PutMapping("/{id}")
    public MapPathResponse updatePath(
            @PathVariable Long id,
            @Valid @RequestBody MapPathRequest request) {

        return mapPathService.updatePath(id, request);
    }

    // Delete a map path.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePath(@PathVariable Long id) {

        mapPathService.deletePath(id);
    }
}