package com.collegeai.backend.controller;

import com.collegeai.backend.dto.CampusLocationRequest;
import com.collegeai.backend.dto.CampusLocationResponse;
import com.collegeai.backend.service.CampusLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/campus-locations")
@RequiredArgsConstructor
public class CampusLocationController {

    private final CampusLocationService campusLocationService;

    // Create a new campus location.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampusLocationResponse createLocation(
            @Valid @RequestBody CampusLocationRequest request) {

        return campusLocationService.createLocation(request);
    }

    // Retrieve all campus locations.
    @GetMapping
    public List<CampusLocationResponse> getAllLocations() {

        return campusLocationService.getAllLocations();
    }

    // Retrieve a campus location by ID.
    @GetMapping("/{id}")
    public CampusLocationResponse getLocationById(
            @PathVariable Long id) {

        return campusLocationService.getLocationById(id);
    }

    // Update an existing campus location.
    @PutMapping("/{id}")
    public CampusLocationResponse updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody CampusLocationRequest request) {

        return campusLocationService.updateLocation(id, request);
    }

    // Delete a campus location.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable Long id) {

        campusLocationService.deleteLocation(id);
    }
}