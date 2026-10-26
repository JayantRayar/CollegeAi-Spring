
package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RouteInstructionResponse {

    private Integer stepNumber;

    // START, WALK, STAIRS or ARRIVAL
    private String instructionType;

    // Human-readable navigation instruction.
    private String instruction;

    private Long fromNodeId;

    private Long toNodeId;

    private Integer fromFloor;

    private Integer toFloor;

    private Double distanceMeters;
}
