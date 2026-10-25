package com.collegeai.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "map_paths")
@Getter
@Setter
@NoArgsConstructor
public class MapPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Starting point of this path
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_node_id", nullable = false)
    private MapNode startNode;

    // Ending point of this path
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_node_id", nullable = false)
    private MapNode endNode;

    // Walking distance between the two nodes, in metres
    @Column(nullable = false)
    private Double distanceMeters;

    // Whether users can travel in both directions
    @Column(nullable = false)
    private Boolean bidirectional = true;
}