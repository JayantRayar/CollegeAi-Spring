
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
}
