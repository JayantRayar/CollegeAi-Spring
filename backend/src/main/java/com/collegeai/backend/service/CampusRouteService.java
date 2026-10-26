
package com.collegeai.backend.service;

import com.collegeai.backend.dto.CampusRouteResponse;
import com.collegeai.backend.dto.MapNodeResponse;
import com.collegeai.backend.dto.RouteInstructionResponse;
import com.collegeai.backend.entity.MapNode;
import com.collegeai.backend.entity.MapPath;
import com.collegeai.backend.exception.ResourceNotFoundException;
import com.collegeai.backend.repository.MapNodeRepository;
import com.collegeai.backend.repository.MapPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CampusRouteService {

    private final MapNodeRepository mapNodeRepository;
    private final MapPathRepository mapPathRepository;

    private record Edge(Long destinationNodeId, Double distanceMeters) {}

    private record QueueNode(Long nodeId, double distance) {}

    private record ShortestPathResult(
            Map<Long, Double> distances,
            Map<Long, Long> previousNodes
    ) {}

    // Builds the campus walking graph.
    private Map<Long, List<Edge>> buildGraph() {

        Map<Long, List<Edge>> graph = new HashMap<>();

        for (MapNode node : mapNodeRepository.findAll()) {
            graph.put(node.getId(), new ArrayList<>());
        }

        for (MapPath path : mapPathRepository.findAll()) {

            Long startId = path.getStartNode().getId();
            Long endId = path.getEndNode().getId();
            Double distance = path.getDistanceMeters();

            graph.get(startId).add(new Edge(endId, distance));

            if (Boolean.TRUE.equals(path.getBidirectional())) {
                graph.get(endId).add(new Edge(startId, distance));
            }
        }

        return graph;
    }

    // Dijkstra's shortest-path algorithm.
    private ShortestPathResult calculateShortestDistances(
            Map<Long, List<Edge>> graph,
            Long startNodeId
    ) {

        if (!graph.containsKey(startNodeId)) {
            throw new ResourceNotFoundException(
                    "Starting campus node not found with ID: " + startNodeId
            );
        }

        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previousNodes = new HashMap<>();

        for (Long nodeId : graph.keySet()) {
            distances.put(nodeId, Double.POSITIVE_INFINITY);
        }

        distances.put(startNodeId, 0.0);

        PriorityQueue<QueueNode> queue = new PriorityQueue<>(
                Comparator.comparingDouble(QueueNode::distance)
        );

        queue.offer(new QueueNode(startNodeId, 0.0));

        while (!queue.isEmpty()) {

            QueueNode current = queue.poll();

            Long currentId = current.nodeId();
            double currentDistance = current.distance();

            if (currentDistance > distances.get(currentId)) {
                continue;
            }

            for (Edge edge : graph.get(currentId)) {

                Long neighbourId = edge.destinationNodeId();

                double newDistance =
                        currentDistance + edge.distanceMeters();

                if (newDistance < distances.get(neighbourId)) {

                    distances.put(neighbourId, newDistance);
                    previousNodes.put(neighbourId, currentId);

                    queue.offer(
                            new QueueNode(neighbourId, newDistance)
                    );
                }
            }
        }

        return new ShortestPathResult(distances, previousNodes);
    }

    // Reconstructs the ordered route.
    private List<Long> reconstructRoute(
            Long startNodeId,
            Long endNodeId,
            ShortestPathResult result
    ) {

        if (!result.distances().containsKey(endNodeId)) {
            throw new ResourceNotFoundException(
                    "Destination campus node not found with ID: " + endNodeId
            );
        }

        if (Double.isInfinite(result.distances().get(endNodeId))) {
            return List.of();
        }

        List<Long> route = new ArrayList<>();
        Long currentId = endNodeId;

        while (currentId != null) {

            route.add(currentId);

            if (currentId.equals(startNodeId)) {
                break;
            }

            currentId = result.previousNodes().get(currentId);
        }

        if (!route.get(route.size() - 1).equals(startNodeId)) {
            return List.of();
        }

        Collections.reverse(route);

        return route;
    }

    // Converts route node IDs into response objects.
    private List<MapNodeResponse> convertRoute(List<Long> routeNodeIds) {

        Map<Long, MapNode> nodeMap = new HashMap<>();

        for (MapNode node : mapNodeRepository.findAllById(routeNodeIds)) {
            nodeMap.put(node.getId(), node);
        }

        return routeNodeIds.stream()
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .map(MapNodeResponse::from)
                .toList();
    }

    // Generates human-readable instructions for the calculated route.
    private List<RouteInstructionResponse> generateInstructions(
            List<Long> routeNodeIds,
            Map<Long, List<Edge>> graph
    ) {

        if (routeNodeIds.isEmpty()) {
            return List.of();
        }

        Map<Long, MapNode> nodeMap = new HashMap<>();

        for (MapNode node : mapNodeRepository.findAllById(routeNodeIds)) {
            nodeMap.put(node.getId(), node);
        }

        MapNode startNode = nodeMap.get(routeNodeIds.get(0));

        MapNode destinationNode =
                nodeMap.get(routeNodeIds.get(routeNodeIds.size() - 1));

        if (startNode == null || destinationNode == null) {
            return List.of();
        }

        List<RouteInstructionResponse> instructions = new ArrayList<>();

        // First instruction: tell the user where to start.
        instructions.add(new RouteInstructionResponse(
                1,
                "START",
                "Start at " + startNode.getName() + ".",
                null,
                startNode.getId(),
                null,
                startNode.getFloor(),
                0.0
        ));

        // Generate an instruction for every connection in the route.
        for (int i = 0; i < routeNodeIds.size() - 1; i++) {

            Long fromNodeId = routeNodeIds.get(i);
            Long toNodeId = routeNodeIds.get(i + 1);

            MapNode fromNode = nodeMap.get(fromNodeId);
            MapNode toNode = nodeMap.get(toNodeId);

            if (fromNode == null || toNode == null) {
                continue;
            }

            // Find the distance of this particular connection.
            Double distance = graph.get(fromNodeId).stream()
                    .filter(edge ->
                            edge.destinationNodeId().equals(toNodeId))
                    .map(Edge::distanceMeters)
                    .findFirst()
                    .orElse(null);

            // Different floors mean the user must use the staircase.
            boolean floorChanged =
                    !Objects.equals(fromNode.getFloor(), toNode.getFloor());

            String instructionType;
            String instruction;

            if (floorChanged) {

                instructionType = "STAIRS";

                instruction = "Take the staircase from floor "
                        + fromNode.getFloor()
                        + " to floor "
                        + toNode.getFloor() + ".";

            } else {

                instructionType = "WALK";

                instruction = "Walk from "
                        + fromNode.getName()
                        + " to "
                        + toNode.getName() + ".";
            }

            instructions.add(new RouteInstructionResponse(
                    instructions.size() + 1,
                    instructionType,
                    instruction,
                    fromNodeId,
                    toNodeId,
                    fromNode.getFloor(),
                    toNode.getFloor(),
                    distance
            ));
        }

        // Final instruction: confirm that the destination is reached.
        instructions.add(new RouteInstructionResponse(
                instructions.size() + 1,
                "ARRIVAL",
                "You have arrived at " + destinationNode.getName() + ".",
                destinationNode.getId(),
                null,
                destinationNode.getFloor(),
                null,
                0.0
        ));

        return instructions;
    }

    // Existing routing method using map node IDs.
    @Transactional(readOnly = true)
    public CampusRouteResponse findShortestRoute(
            Long startNodeId,
            Long endNodeId
    ) {

        Map<Long, List<Edge>> graph = buildGraph();

        ShortestPathResult result =
                calculateShortestDistances(graph, startNodeId);

        List<Long> routeNodeIds =
                reconstructRoute(startNodeId, endNodeId, result);

        Long startLocationId = mapNodeRepository.findById(startNodeId)
                .map(node -> node.getCampusLocation() != null
                        ? node.getCampusLocation().getId() : null)
                .orElse(null);

        Long endLocationId = mapNodeRepository.findById(endNodeId)
                .map(node -> node.getCampusLocation() != null
                        ? node.getCampusLocation().getId() : null)
                .orElse(null);

        return new CampusRouteResponse(
                startLocationId,
                endLocationId,
                startNodeId,
                endNodeId,
                routeNodeIds.isEmpty()
                        ? null : result.distances().get(endNodeId),
                convertRoute(routeNodeIds),
                generateInstructions(routeNodeIds, graph)
        );
    }

    // New routing method using campus location IDs.
    @Transactional(readOnly = true)
    public CampusRouteResponse findShortestRouteByLocationIds(
            Long startLocationId,
            Long endLocationId
    ) {

        List<MapNode> startNodes =
                mapNodeRepository.findByCampusLocation_Id(startLocationId);

        List<MapNode> endNodes =
                mapNodeRepository.findByCampusLocation_Id(endLocationId);

        if (startNodes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No map nodes found for starting campus location ID: "
                            + startLocationId
            );
        }

        if (endNodes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No map nodes found for destination campus location ID: "
                            + endLocationId
            );
        }

        Map<Long, List<Edge>> graph = buildGraph();

        double shortestDistance = Double.POSITIVE_INFINITY;

        Long bestStartNodeId = null;
        Long bestEndNodeId = null;

        List<Long> bestRoute = List.of();

        // Compare all possible starting and destination nodes.
        for (MapNode startNode : startNodes) {

            ShortestPathResult result =
                    calculateShortestDistances(graph, startNode.getId());

            for (MapNode endNode : endNodes) {

                Double distance = result.distances().get(endNode.getId());

                if (distance != null && distance < shortestDistance) {

                    List<Long> candidateRoute =
                            reconstructRoute(
                                    startNode.getId(),
                                    endNode.getId(),
                                    result
                            );

                    if (!candidateRoute.isEmpty()) {

                        shortestDistance = distance;
                        bestStartNodeId = startNode.getId();
                        bestEndNodeId = endNode.getId();
                        bestRoute = candidateRoute;
                    }
                }
            }
        }

        // No connected route exists.
        if (bestRoute.isEmpty()) {
            return new CampusRouteResponse(
                    startLocationId,
                    endLocationId,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of()
            );
        }

        return new CampusRouteResponse(
                startLocationId,
                endLocationId,
                bestStartNodeId,
                bestEndNodeId,
                shortestDistance,
                convertRoute(bestRoute),
                generateInstructions(bestRoute, graph)
        );
    }
}
