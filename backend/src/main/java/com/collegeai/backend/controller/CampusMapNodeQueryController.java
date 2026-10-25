
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.MapNodeResponse;
import com.collegeai.backend.service.MapNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campus/map-nodes")
@RequiredArgsConstructor
public class CampusMapNodeQueryController {

    private final MapNodeService mapNodeService;

    // Retrieve all walkable points on the campus map.
    @GetMapping
    public List<MapNodeResponse> getAllNodes() {
        return mapNodeService.getAllNodes();
    }

    // Retrieve one map node by its ID.
    @GetMapping("/{id}")
    public MapNodeResponse getNodeById(@PathVariable Long id) {
        return mapNodeService.getNodeById(id);
    }
}
