package team6;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jgrapht.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
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

    /**
     * Writes a scheduled graph to a dot file. Each task is written with its weight, along
     * with the start time and processor number assigned to it by the scheduler, and each
     * edge is written with its communication weight.
     *
     * @param graph    a graph whose tasks have already been scheduled
     * @param filePath the path of the dot file to write
     */
    public static void writeGraph(Graph<TaskNode, DefaultWeightedEdge> graph, String filePath) {
        DOTExporter<TaskNode, DefaultWeightedEdge> exporter = new DOTExporter<>(node -> dotId(node.getId()));

        exporter.setGraphIdProvider(() -> graphNameFrom(filePath));

        // Copy the task weight across and add the two attributes describing the schedule.
        exporter.setVertexAttributeProvider(node -> {
            Map<String, Attribute> attributes = new LinkedHashMap<>();
            attributes.put("Weight", DefaultAttribute.createAttribute(node.getWeight()));
            attributes.put("Start", DefaultAttribute.createAttribute(node.getStartTime()));
            attributes.put("Processor", DefaultAttribute.createAttribute(node.getProcessorNum()));
            return attributes;
        });

        // Edges are copied across unchanged.
        exporter.setEdgeAttributeProvider(edge -> {
            Map<String, Attribute> attributes = new LinkedHashMap<>();
            attributes.put("Weight", DefaultAttribute.createAttribute((int) graph.getEdgeWeight(edge)));
            return attributes;
        });

        try (Writer writer = new FileWriter(filePath)) {
            exporter.exportGraph(graph, writer);
        } catch (IOException e) {
            System.out.println("Error: Could not write the output file " + filePath);
            e.printStackTrace();
        }
    }

    /**
     * Returns a task id in a form that is valid in a dot file. Task names can be any string,
     * so names that are not plain identifiers or numbers, such as ones containing spaces, are
     * written as a quoted string instead.
     *
     * @param id the id of the task being written
     * @return the id in a form that is valid to use as a dot identifier
     */
    private static String dotId(String id) {
        // A plain name or a number can be written as it is.
        if (id.matches("[a-zA-Z_][a-zA-Z_0-9]*") || id.matches("-?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)")) {
            return id;
        }

        // Anything else is quoted, escaping any backslashes and quotes it contains.
        return "\"" + id.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Derives a valid dot graph name from an output file path, so that for example
     * "graphs/example-output.dot" becomes "example_output". Characters that dot does not
     * allow in an unquoted identifier are replaced with underscores.
     *
     * @param filePath the path of the dot file being written
     * @return a name that is valid to use as a dot graph identifier
     */
    private static String graphNameFrom(String filePath) {
        // Using File keeps this working with both Windows and Linux path separators.
        String name = new File(filePath).getName();

        if (name.toLowerCase().endsWith(".dot")) {
            name = name.substring(0, name.length() - 4);
        }

        name = name.replaceAll("[^A-Za-z0-9_]", "_");

        // A dot identifier cannot start with a digit, so prefix one that does.
        if (name.isEmpty() || (!Character.isLetter(name.charAt(0)) && name.charAt(0) != '_')) {
            name = "_" + name;
        }

        return name;
    }

}
