package com.collegeai.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "map_nodes")
@Getter
@Setter
@NoArgsConstructor
public class MapNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Example: ENTRANCE, JUNCTION, STAIRS, ELEVATOR, DESTINATION
    @Column(nullable = false)
    private String nodeType;

    // Position on the floor-plan map
    private Double xCoordinate;

    private Double yCoordinate;

    // Floor on which this node exists
    private Integer floor;

    // Connects a destination node to its actual campus location.
    // Optional because junctions and entrances may not represent
    // a specific destination.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_location_id")
    private CampusLocation campusLocation;
}