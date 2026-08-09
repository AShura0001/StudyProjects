package team6;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.nio.dot.DOTImporter;

public class FileHandler {
    /**
     * Returns a <code>Graph</code> object corresponding to the dot file input.
     *
     * @param filePath the path of the dot that is being read
     * @return graph of the input dot file
     */
    public static Graph<TaskNode, DefaultWeightedEdge> readGraph(String filePath) {
        // Use the jgrapht implementation of graphs temporarily
        Graph<TaskNode, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        DOTImporter<TaskNode, DefaultWeightedEdge> importer = new DOTImporter<>();

        // Map of all nodes
        Map<String, TaskNode> nodeMap = new HashMap<>();

        // Only make a new node if one does not exist.
        importer.setVertexFactory(node -> nodeMap.computeIfAbsent(node, TaskNode::new));

        // Add weights to the nodes, ignoring other attributes.
        importer.addVertexAttributeConsumer((pair, attribute) -> {
            // jgrapht stores stuff in pairs, i.e. edge A -> B has attribute = value.
            // For each attribute, this will run and the name and value will change.
            TaskNode node = pair.getFirst();
            String attributeName = pair.getSecond();
            String attributeValue = attribute.getValue();

            if (attributeName.toLowerCase().equals("weight")) {
                node.setWeight(Integer.parseInt(attributeValue));
            }
        });

        // Add weights to the edges, ignoring other attributes.
        importer.addEdgeAttributeConsumer((pair, attribute) -> {
            DefaultWeightedEdge edge = pair.getFirst();
            String attributeName = pair.getSecond();
            String attributeValue = attribute.getValue();

            if (attributeName.toLowerCase().equals("weight")) {
                graph.setEdgeWeight(edge, Integer.parseInt(attributeValue));
            }
        });

        // Read the file
        try (Reader reader = new FileReader(filePath)) {
            importer.importGraph(graph, reader);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return graph;
    }
    // TODO create method that reads graph from DOT file
    // input/out can be changed
    // public static Graph readGraph(Object file) {
    // return null;
    // }

    // TODO Writes graph g into a file
    public static void writeGraph(Graph g) {
    }

}
