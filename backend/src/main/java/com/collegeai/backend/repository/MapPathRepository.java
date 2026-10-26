
package com.collegeai.backend.repository;

import com.collegeai.backend.entity.MapPath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapPathRepository extends JpaRepository<MapPath, Long> {

    // Find paths where both connected nodes belong to the specified floors.
    List<MapPath> findByStartNode_FloorAndEndNode_Floor(
            Integer startFloor,
            Integer endFloor
    );

    // Check whether a connection already exists in the given direction.
    boolean existsByStartNode_IdAndEndNode_Id(
            Long startNodeId,
            Long endNodeId
    );

    // Check whether a connection exists in either direction.
    boolean existsByStartNode_IdAndEndNode_IdOrStartNode_IdAndEndNode_Id(
            Long startNodeId,
            Long endNodeId,
            Long reverseStartNodeId,
            Long reverseEndNodeId
    );

    // Check for duplicate connections while excluding the current path.
    boolean existsByStartNode_IdAndEndNode_IdAndIdNotOrStartNode_IdAndEndNode_IdAndIdNot(
            Long startNodeId,
            Long endNodeId,
            Long excludedPathId,
            Long reverseStartNodeId,
            Long reverseEndNodeId,
            Long reverseExcludedPathId
    );
}
