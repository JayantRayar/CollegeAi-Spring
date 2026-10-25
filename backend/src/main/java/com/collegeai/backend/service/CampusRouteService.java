
package com.collegeai.backend.service;

import com.collegeai.backend.dto.CampusRouteResponse;
import com.collegeai.backend.dto.MapNodeResponse;
import com.collegeai.backend.entity.MapNode;
import com.collegeai.backend.entity.MapPath;
import com.collegeai.backend.exception.ResourceNotFoundException;
import com.collegeai.backend.repository.MapNodeRepository;
import com.collegeai.backend.repository.MapPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class CampusRouteService {

    private final MapNodeRepository mapNodeRepository;
    private final MapPathRepository mapPathRepository;

    // Represents a connection from one node to another.
    private record Edge(Long destinationNodeId, Double distanceMeters) {}

    // Represents a node stored in the priority queue.
    private record QueueNode(Long nodeId, double distance) {}

    // Stores shortest distances and previous nodes for route reconstruction.
    private record ShortestPathResult(
            Map<Long, Double> distances,
            Map<Long, Long> previousNodes
    ) {}

    /*
     * Builds the campus graph using map nodes and paths.
     *
     * Each node represents a walkable point.
     * Each edge represents a walking connection between two nodes.
     */
    private Map<Long, List<Edge>> buildGraph() {

        Map<Long, List<Edge>> graph = new HashMap<>();

        List<MapNode> nodes = mapNodeRepository.findAll();

        // Initially, every node has an empty list of connections.
        for (MapNode node : nodes) {
            graph.put(node.getId(), new ArrayList<>());
        }

        List<MapPath> paths = mapPathRepository.findAll();

        for (MapPath path : paths) {

            Long startNodeId = path.getStartNode().getId();
            Long endNodeId = path.getEndNode().getId();

            Double distance = path.getDistanceMeters();

            // Add the forward connection.
            graph.get(startNodeId).add(
                    new Edge(endNodeId, distance)
            );

            // Add the reverse connection if the path is bidirectional.
            if (Boolean.TRUE.equals(path.getBidirectional())) {
                graph.get(endNodeId).add(
                        new Edge(startNodeId, distance)
                );
            }
        }

        return graph;
    }

    /*
     * Dijkstra's algorithm calculates the shortest distance
     * from the starting node to every reachable node.
     */
    private ShortestPathResult calculateShortestDistances(
            Map<Long, List<Edge>> graph,
            Long startNodeId
    ) {

        // Check whether the starting node exists.
        if (!graph.containsKey(startNodeId)) {
            throw new ResourceNotFoundException(
                    "Starting campus node not found with ID: " + startNodeId
            );
        }

        Map<Long, Double> shortestDistances = new HashMap<>();
        Map<Long, Long> previousNodes = new HashMap<>();

        // Initially, all nodes have infinite distance.
        for (Long nodeId : graph.keySet()) {
            shortestDistances.put(nodeId, Double.POSITIVE_INFINITY);
        }

        // Distance from the starting node to itself is zero.
        shortestDistances.put(startNodeId, 0.0);

        PriorityQueue<QueueNode> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(QueueNode::distance)
                );

        priorityQueue.offer(
                new QueueNode(startNodeId, 0.0)
        );

        while (!priorityQueue.isEmpty()) {

            QueueNode current = priorityQueue.poll();

            Long currentNodeId = current.nodeId();
            double currentDistance = current.distance();

            // Ignore outdated entries in the priority queue.
            if (currentDistance > shortestDistances.get(currentNodeId)) {
                continue;
            }

            // Explore all neighbouring nodes.
            for (Edge edge : graph.get(currentNodeId)) {

                Long neighbourId = edge.destinationNodeId();

                double newDistance =
                        currentDistance + edge.distanceMeters();

                // Update when a shorter path is discovered.
                if (newDistance < shortestDistances.get(neighbourId)) {

                    shortestDistances.put(neighbourId, newDistance);

                    // Remember the previous node for route reconstruction.
                    previousNodes.put(neighbourId, currentNodeId);

                    priorityQueue.offer(
                            new QueueNode(neighbourId, newDistance)
                    );
                }
            }
        }

        return new ShortestPathResult(
                shortestDistances,
                previousNodes
        );
    }

    /*
     * Reconstructs the shortest route using previous-node information.
     */
    private List<Long> reconstructRoute(
            Long startNodeId,
            Long endNodeId,
            ShortestPathResult result
    ) {

        Map<Long, Double> distances = result.distances();
        Map<Long, Long> previousNodes = result.previousNodes();

        // Check whether the destination node exists.
        if (!distances.containsKey(endNodeId)) {
            throw new ResourceNotFoundException(
                    "Destination campus node not found with ID: " + endNodeId
            );
        }

        // An infinite distance means no route connects the two nodes.
        if (Double.isInfinite(distances.get(endNodeId))) {
            return List.of();
        }

        List<Long> route = new ArrayList<>();

        Long currentNodeId = endNodeId;

        // Trace backwards from destination to starting node.
        while (currentNodeId != null) {

            route.add(currentNodeId);

            if (currentNodeId.equals(startNodeId)) {
                break;
            }

            currentNodeId = previousNodes.get(currentNodeId);
        }

        // Ensure the reconstructed route actually reaches the start node.
        if (!route.get(route.size() - 1).equals(startNodeId)) {
            return List.of();
        }

        // Reverse to get the route from start to destination.
        Collections.reverse(route);

        return route;
    }

    /*
     * Public method used by the controller to find the shortest route.
     */
    @Transactional(readOnly = true)
    public CampusRouteResponse findShortestRoute(
            Long startNodeId,
            Long endNodeId
    ) {

        // Build the campus graph.
        Map<Long, List<Edge>> graph = buildGraph();

        // Calculate shortest distances from the starting node.
        ShortestPathResult result =
                calculateShortestDistances(graph, startNodeId);

        // Reconstruct the route to the destination.
        List<Long> routeNodeIds =
                reconstructRoute(startNodeId, endNodeId, result);

        // Return an empty route when no connection exists.
        if (routeNodeIds.isEmpty()) {
            return new CampusRouteResponse(
                    startNodeId,
                    endNodeId,
                    null,
                    List.of()
            );
        }

        // Fetch the nodes that belong to the shortest route.
        Iterable<MapNode> nodes =
                mapNodeRepository.findAllById(routeNodeIds);

        Map<Long, MapNode> nodeMap = new HashMap<>();

        for (MapNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        // Preserve the correct route order when creating the response.
        List<MapNodeResponse> route =
                routeNodeIds.stream()
                        .map(nodeMap::get)
                        .map(MapNodeResponse::from)
                        .toList();

        Double totalDistance = result.distances().get(endNodeId);

        return new CampusRouteResponse(
                startNodeId,
                endNodeId,
                totalDistance,
                route
        );
    }
}
