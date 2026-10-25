package com.collegeai.backend.repository;

import com.collegeai.backend.entity.CampusLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusLocationRepository extends JpaRepository<CampusLocation, Long> {
}