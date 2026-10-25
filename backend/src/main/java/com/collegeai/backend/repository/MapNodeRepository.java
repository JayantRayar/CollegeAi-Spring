
package com.collegeai.backend.repository;

import com.collegeai.backend.entity.MapNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapNodeRepository extends JpaRepository<MapNode, Long> {

    /*
     * Finds all map nodes associated with a campus location.
     *
     * One campus location can have multiple map nodes,
     * such as multiple entrances or access points.
     */
    List<MapNode> findByCampusLocation_Id(Long campusLocationId);
}
