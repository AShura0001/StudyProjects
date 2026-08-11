package team6;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.nio.ImportException;
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

            // A malformed file can leave an attribute without a value.
            if (attribute == null) {
                return;
            }

            if (attributeName.toLowerCase().equals("weight")) {
                node.setWeight(parseWeight(attribute.getValue(), "task " + node.getId()));
            }
        });

        // Add weights to the edges, ignoring other attributes.
        importer.addEdgeAttributeConsumer((pair, attribute) -> {
            DefaultWeightedEdge edge = pair.getFirst();
            String attributeName = pair.getSecond();

            if (attribute == null) {
                return;
            }

            if (attributeName.toLowerCase().equals("weight")) {
                graph.setEdgeWeight(edge, parseWeight(attribute.getValue(), "edge " + edge));
            }
        });

        // Read the file
        try (Reader reader = new FileReader(filePath)) {
            importer.importGraph(graph, reader);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Could not find the input file " + filePath, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the input file " + filePath, e);
        } catch (ImportException | NullPointerException e) {
            // A problem with the contents, such as a weight that is not an integer, already
            // describes itself, so that message is kept rather than the general one below.
            if (e.getCause() instanceof IllegalArgumentException cause) {
                throw cause;
            }

            throw new IllegalArgumentException(filePath + " is not a valid dot graph.", e);
        }

        return graph;
    }

    /**
     * Reads a weight attribute as an integer.
     *
     * @param value the value of the weight attribute
     * @param owner a description of the task or edge the weight belongs to, used for errors
     * @return the weight as an integer
     */
    private static int parseWeight(String value, String owner) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The weight of " + owner + " is not an integer: " + value, e);
        }
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
        // The file is written out by hand so that it has the same layout as the output shown
        // in the project description, which the dot exporter in jgrapht does not produce.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("digraph \"" + graphNameFrom(filePath) + "\" {");
            writer.newLine();

            // Each task keeps its weight and gains the two attributes for its schedule.
            for (TaskNode task : graph.vertexSet()) {
                writer.write("\t" + dotId(task.getId()) + "\t [Weight=" + task.getWeight()
                        + ",Start=" + task.getStartTime()
                        + ",Processor=" + task.getProcessorNum() + "];");
                writer.newLine();
            }

            // Edges are copied across unchanged.
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                writer.write("\t" + dotId(graph.getEdgeSource(edge).getId())
                        + " -> " + dotId(graph.getEdgeTarget(edge).getId())
                        + "\t [Weight=" + (int) graph.getEdgeWeight(edge) + "];");
                writer.newLine();
            }

            writer.write("}");
            writer.newLine();
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
     * Names the graph after the output file, so that for example "graphs/example-output.dot"
     * becomes "example-output". The name is written inside quotes, so it only needs escaping
     * rather than having awkward characters replaced.
     *
     * @param filePath the path of the dot file being written
     * @return the name to give the graph
     */
    private static String graphNameFrom(String filePath) {
        // Using File keeps this working with both Windows and Linux path separators.
        String name = new File(filePath).getName();

        if (name.toLowerCase().endsWith(".dot")) {
            name = name.substring(0, name.length() - 4);
        }

        return name.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
