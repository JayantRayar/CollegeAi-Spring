
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.CampusLocationResponse;
import com.collegeai.backend.service.CampusLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campus/locations")
@RequiredArgsConstructor
public class CampusMapQueryController {

    private final CampusLocationService campusLocationService;

    // Retrieve all campus locations for the campus map.
    @GetMapping
    public List<CampusLocationResponse> getAllLocations() {
        return campusLocationService.getAllLocations();
    }

    // Retrieve one campus location by its ID.
    @GetMapping("/{id}")
    public CampusLocationResponse getLocationById(
            @PathVariable Long id) {
        return campusLocationService.getLocationById(id);
    }
}
