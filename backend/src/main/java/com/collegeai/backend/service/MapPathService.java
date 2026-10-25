package com.collegeai.backend.service;

import com.collegeai.backend.dto.MapPathRequest;
import com.collegeai.backend.dto.MapPathResponse;
import com.collegeai.backend.entity.MapNode;
import com.collegeai.backend.entity.MapPath;
import com.collegeai.backend.repository.MapNodeRepository;
import com.collegeai.backend.repository.MapPathRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapPathService {

    private final MapPathRepository mapPathRepository;
    private final MapNodeRepository mapNodeRepository;

    // Create a connection between two existing map nodes.
    @Transactional
    public MapPathResponse createPath(MapPathRequest request) {

        validateDifferentNodes(
                request.getStartNodeId(),
                request.getEndNodeId()
        );

        MapNode startNode =
                findNodeById(request.getStartNodeId());

        MapNode endNode =
                findNodeById(request.getEndNodeId());

        MapPath path = new MapPath();

        path.setStartNode(startNode);
        path.setEndNode(endNode);
        path.setDistanceMeters(request.getDistanceMeters());
        path.setBidirectional(request.getBidirectional());

        MapPath savedPath = mapPathRepository.save(path);

        return MapPathResponse.from(savedPath);
    }

    // Retrieve all paths.
    @Transactional(readOnly = true)
    public List<MapPathResponse> getAllPaths() {

        return mapPathRepository.findAll()
                .stream()
                .map(MapPathResponse::from)
                .toList();
    }

    // Retrieve a path by ID.
    @Transactional(readOnly = true)
    public MapPathResponse getPathById(Long id) {

        MapPath path = findPathById(id);

        return MapPathResponse.from(path);
    }

    // Update an existing path.
    @Transactional
    public MapPathResponse updatePath(
            Long id,
            MapPathRequest request) {

        validateDifferentNodes(
                request.getStartNodeId(),
                request.getEndNodeId()
        );

        MapPath existingPath = findPathById(id);

        MapNode startNode =
                findNodeById(request.getStartNodeId());

        MapNode endNode =
                findNodeById(request.getEndNodeId());

        existingPath.setStartNode(startNode);
        existingPath.setEndNode(endNode);
        existingPath.setDistanceMeters(request.getDistanceMeters());
        existingPath.setBidirectional(request.getBidirectional());

        MapPath updatedPath =
                mapPathRepository.save(existingPath);

        return MapPathResponse.from(updatedPath);
    }

    // Delete a path.
    @Transactional
    public void deletePath(Long id) {

        MapPath path = findPathById(id);

        mapPathRepository.delete(path);
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

    // Find a path or throw an exception if it does not exist.
    private MapPath findPathById(Long id) {

        return mapPathRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Map path not found with ID: " + id
                        )
                );
    }

    // Prevent a node from connecting to itself.
    private void validateDifferentNodes(
            Long startNodeId,
            Long endNodeId) {

        if (startNodeId.equals(endNodeId)) {

            throw new IllegalArgumentException(
                    "Starting node and ending node must be different."
            );
        }
    }
}