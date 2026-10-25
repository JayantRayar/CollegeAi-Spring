package com.collegeai.backend.controller;

import com.collegeai.backend.dto.MapNodeRequest;
import com.collegeai.backend.dto.MapNodeResponse;
import com.collegeai.backend.service.MapNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/map-nodes")
@RequiredArgsConstructor
public class MapNodeController {

    private final MapNodeService mapNodeService;

    // Create a new map node.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MapNodeResponse createNode(
            @Valid @RequestBody MapNodeRequest request) {

        return mapNodeService.createNode(request);
    }

    // Retrieve all map nodes.
    @GetMapping
    public List<MapNodeResponse> getAllNodes() {

        return mapNodeService.getAllNodes();
    }

    // Retrieve a map node by ID.
    @GetMapping("/{id}")
    public MapNodeResponse getNodeById(
            @PathVariable Long id) {

        return mapNodeService.getNodeById(id);
    }

    // Update an existing map node.
    @PutMapping("/{id}")
    public MapNodeResponse updateNode(
            @PathVariable Long id,
            @Valid @RequestBody MapNodeRequest request) {

        return mapNodeService.updateNode(id, request);
    }

    // Delete a map node.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable Long id) {

        mapNodeService.deleteNode(id);
    }
}