package com.collegeai.backend.repository;

import com.collegeai.backend.entity.MapNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapNodeRepository extends JpaRepository<MapNode, Long> {
}