package com.collegeai.backend.service;

import com.collegeai.backend.dto.CampusLocationRequest;
import com.collegeai.backend.dto.CampusLocationResponse;
import com.collegeai.backend.entity.CampusLocation;
import com.collegeai.backend.repository.CampusLocationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampusLocationService {

    private final CampusLocationRepository campusLocationRepository;

    // Create a new campus location.
    public CampusLocationResponse createLocation(
            CampusLocationRequest request) {

        CampusLocation location = new CampusLocation();

        mapRequestToEntity(request, location);

        CampusLocation savedLocation =
                campusLocationRepository.save(location);

        return CampusLocationResponse.from(savedLocation);
    }

    // Retrieve all campus locations.
    public List<CampusLocationResponse> getAllLocations() {

        return campusLocationRepository.findAll()
                .stream()
                .map(CampusLocationResponse::from)
                .toList();
    }

    // Retrieve a campus location by ID.
    public CampusLocationResponse getLocationById(Long id) {

        CampusLocation location = findLocationById(id);

        return CampusLocationResponse.from(location);
    }

    // Update an existing campus location.
    public CampusLocationResponse updateLocation(
            Long id,
            CampusLocationRequest request) {

        CampusLocation existingLocation = findLocationById(id);

        mapRequestToEntity(request, existingLocation);

        CampusLocation updatedLocation =
                campusLocationRepository.save(existingLocation);

        return CampusLocationResponse.from(updatedLocation);
    }

    // Delete a campus location.
    public void deleteLocation(Long id) {

        CampusLocation location = findLocationById(id);

        campusLocationRepository.delete(location);
    }

    // Find a location or throw an exception if it does not exist.
    private CampusLocation findLocationById(Long id) {

        return campusLocationRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Campus location not found with ID: " + id
                        )
                );
    }

    // Copies request data into the entity.
    private void mapRequestToEntity(
            CampusLocationRequest request,
            CampusLocation location) {

        location.setName(request.getName());
        location.setCategory(request.getCategory());
        location.setDescription(request.getDescription());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setBuilding(request.getBuilding());
        location.setFloor(request.getFloor());
    }
}