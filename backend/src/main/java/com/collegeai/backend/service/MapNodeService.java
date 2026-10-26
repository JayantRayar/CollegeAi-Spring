package com.collegeai.backend.service;

import com.collegeai.backend.dto.MapNodeRequest;
import com.collegeai.backend.dto.MapNodeResponse;
import com.collegeai.backend.entity.CampusLocation;
import com.collegeai.backend.entity.MapNode;
import com.collegeai.backend.repository.CampusLocationRepository;
import com.collegeai.backend.repository.MapNodeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapNodeService {

    private final MapNodeRepository mapNodeRepository;
    private final CampusLocationRepository campusLocationRepository;

    // Create a new map node.
    @Transactional
    public MapNodeResponse createNode(MapNodeRequest request) {

        MapNode node = new MapNode();

        mapRequestToEntity(request, node);

        MapNode savedNode = mapNodeRepository.save(node);

        return MapNodeResponse.from(savedNode);
    }

    // Retrieve all map nodes.
    @Transactional(readOnly = true)
    public List<MapNodeResponse> getAllNodes() {

        return mapNodeRepository.findAll()
                .stream()
                .map(MapNodeResponse::from)
                .toList();
    }

    // Retrieve all map nodes belonging to a particular floor.
    @Transactional(readOnly = true)
    public List<MapNodeResponse> getNodesByFloor(Integer floor) {

        return mapNodeRepository.findByFloor(floor)
                .stream()
                .map(MapNodeResponse::from)
                .toList();
    }

    // Retrieve a map node by ID.
    @Transactional(readOnly = true)
    public MapNodeResponse getNodeById(Long id) {

        MapNode node = findNodeById(id);

        return MapNodeResponse.from(node);
    }

    // Update an existing map node.
    @Transactional
    public MapNodeResponse updateNode(
            Long id,
            MapNodeRequest request) {

        MapNode existingNode = findNodeById(id);

        mapRequestToEntity(request, existingNode);

        MapNode updatedNode =
                mapNodeRepository.save(existingNode);

        return MapNodeResponse.from(updatedNode);
    }

    // Delete a map node.
    @Transactional
    public void deleteNode(Long id) {

        MapNode node = findNodeById(id);

        mapNodeRepository.delete(node);
    }

    // Find a node or throw an exception if it does not exist.
    private MapNode findNodeById(Long id) {

        return mapNodeRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Map node not found with ID: " + id
                        )
                );
    }

    // Copies request data into the MapNode entity.
    private void mapRequestToEntity(
            MapNodeRequest request,
            MapNode node) {

        node.setName(request.getName());
        node.setNodeType(request.getNodeType());
        node.setXCoordinate(request.getXCoordinate());
        node.setYCoordinate(request.getYCoordinate());
        node.setFloor(request.getFloor());

        // Resolve the campus location ID into the actual entity.
        if (request.getCampusLocationId() != null) {

            CampusLocation location =
                    campusLocationRepository.findById(
                            request.getCampusLocationId()
                    ).orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Campus location not found with ID: "
                                            + request.getCampusLocationId()
                            )
                    );

            node.setCampusLocation(location);

        } else {

            // Junctions/entrances may not belong to a destination.
            node.setCampusLocation(null);
        }
    }
}