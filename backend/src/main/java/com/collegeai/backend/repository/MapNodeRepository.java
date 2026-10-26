
package com.collegeai.backend.repository;

import com.collegeai.backend.entity.MapNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapNodeRepository extends JpaRepository<MapNode, Long> {

    // Find all map nodes associated with a campus location.
    List<MapNode> findByCampusLocation_Id(Long campusLocationId);

    // Find all map nodes belonging to a particular floor.
    List<MapNode> findByFloor(Integer floor);
}
