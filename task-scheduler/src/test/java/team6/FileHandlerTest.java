package team6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for reading task graphs from dot files and writing scheduled graphs back out.
 * Covers the task names and the broken input files that the scheduler has to cope with.
 */
public class FileHandlerTest {

    /** A task graph whose task names are not all valid dot identifiers. */
    private static final String NASTY_GRAPH = "/nasty.dot";

    // --- WRITING AWKWARD TASK NAMES ---

    @Test
    public void writesAwkwardTaskNamesAsQuotedStrings(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("nasty-output.dot");

        FileHandler.writeGraph(scheduledNastyGraph(), output.toString());

        String contents = Files.readString(output);

        // Names that dot does not allow to be written plainly have to be quoted.
        assertTrue(contents.contains("\"a-b\""), "The name a-b should have been quoted");
        assertTrue(contents.contains("\"3rd task\""), "The name 3rd task should have been quoted");

        // A plain number is already a valid dot name, so it is left as it is.
        assertTrue(contents.contains("\t42\t"), "The name 42 should not have been quoted");
    }

    @Test
    public void writesAwkwardTaskNamesThatCanBeReadBackUnchanged(@TempDir Path tempDir) {
        Path output = tempDir.resolve("nasty-output.dot");
        Graph<TaskNode, DefaultWeightedEdge> graph = scheduledNastyGraph();

        FileHandler.writeGraph(graph, output.toString());
        Graph<TaskNode, DefaultWeightedEdge> reread = FileHandler.readGraph(output.toString());

        assertEquals(graph.vertexSet().size(), reread.vertexSet().size(), "A task was lost when writing the graph");
        assertEquals(graph.edgeSet().size(), reread.edgeSet().size(), "An edge was lost when writing the graph");

        for (TaskNode task : graph.vertexSet()) {
            assertEquals(task.getWeight(), taskNamed(reread, task.getId()).getWeight(),
                    "The weight of task " + task.getId() + " changed when the graph was written");
        }
    }

    @Test
    public void writesAStartTimeAndProcessorForEveryTask(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("nasty-output.dot");
        Graph<TaskNode, DefaultWeightedEdge> graph = scheduledNastyGraph();

        FileHandler.writeGraph(graph, output.toString());

        String contents = Files.readString(output);
        assertEquals(graph.vertexSet().size(), countOccurrences(contents, "Start="),
                "Every task should have been given a start time");
        assertEquals(graph.vertexSet().size(), countOccurrences(contents, "Processor="),
                "Every task should have been given a processor");
    }

    // --- READING INPUT FILES ---

    @Test
    public void ignoresDotSyntaxThatIsNotAWeight(@TempDir Path tempDir) throws IOException {
        // The input may contain any other dot syntax, which should not upset the reader.
        Path input = writeDotFile(tempDir, "extras.dot",
                "digraph \"extras\" {",
                "  rankdir=LR;",
                "  node [shape=circle, color=blue];",
                "  edge [style=dashed];",
                "  a [Weight=2, label=\"hello\", color=red];",
                "  b [Weight=3];",
                "  a -> b [Weight=1, label=\"edge\"];",
                "}");

        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph(input.toString());

        assertEquals(2, graph.vertexSet().size());
        assertEquals(1, graph.edgeSet().size());
        assertEquals(2, taskNamed(graph, "a").getWeight());
        assertEquals(3, taskNamed(graph, "b").getWeight());
    }

    @Test
    public void reportsAnInputFileThatDoesNotExist(@TempDir Path tempDir) {
        String missing = tempDir.resolve("doesNotExist.dot").toString();

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> FileHandler.readGraph(missing));

        assertTrue(thrown.getMessage().contains("Could not find"),
                "The error should say the file could not be found, but was: " + thrown.getMessage());
    }

    @Test
    public void reportsAnInputFileThatIsNotAValidGraph(@TempDir Path tempDir) throws IOException {
        Path input = writeDotFile(tempDir, "broken.dot",
                "digraph broken {",
                "  a [Weight=2",
                "  b ->");

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> FileHandler.readGraph(input.toString()));

        assertTrue(thrown.getMessage().contains("not a valid dot graph"),
                "The error should say the graph is not valid, but was: " + thrown.getMessage());
    }

    @Test
    public void reportsAWeightThatIsNotAnInteger(@TempDir Path tempDir) throws IOException {
        Path input = writeDotFile(tempDir, "badweight.dot",
                "digraph g {",
                "  a [Weight=2];",
                "  b [Weight=zzz];",
                "  a -> b [Weight=1];",
                "}");

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> FileHandler.readGraph(input.toString()));

        // The message should name the task at fault so the file can be corrected.
        assertTrue(thrown.getMessage().contains("task b"),
                "The error should name the task at fault, but was: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("zzz"),
                "The error should quote the bad value, but was: " + thrown.getMessage());
    }

    // --- HELPERS ---

    /**
     * Returns the awkwardly named task graph, scheduled onto two processors.
     *
     * @return a scheduled graph
     */
    private static Graph<TaskNode, DefaultWeightedEdge> scheduledNastyGraph() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph(resourcePath(NASTY_GRAPH));
        Scheduler.serialScheduler(graph, 2);
        return graph;
    }

    /**
     * Writes the given lines to a dot file in the given directory.
     *
     * @param directory the directory to write the file into
     * @param name      the name of the file to write
     * @param lines     the lines making up the file
     * @return the path of the file that was written
     * @throws IOException if the file cannot be written
     */
    private static Path writeDotFile(Path directory, String name, String... lines) throws IOException {
        Path file = directory.resolve(name);
        Files.write(file, java.util.Arrays.asList(lines));
        return file;
    }

    /**
     * Finds the task with the given name in the graph.
     *
     * @param graph the graph to search
     * @param id    the name of the task to find
     * @return the matching task
     */
    private static TaskNode taskNamed(Graph<TaskNode, DefaultWeightedEdge> graph, String id) {
        return graph.vertexSet().stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No task named " + id));
    }

    /**
     * Counts how many times a piece of text appears.
     *
     * @param contents the text to search through
     * @param text     the text to look for
     * @return the number of times it appears
     */
    private static int countOccurrences(String contents, String text) {
        int count = 0;
        int index = contents.indexOf(text);

        while (index >= 0) {
            count++;
            index = contents.indexOf(text, index + text.length());
        }

        return count;
    }

    /**
     * Returns the absolute path of a dot file on the test classpath. Resolving the resource
     * this way rather than hard coding a path keeps the tests working on any operating system.
     *
     * @param resource the classpath location of the file
     * @return the absolute path of the file
     */
    private static String resourcePath(String resource) {
        URL url = FileHandlerTest.class.getResource(resource);

        if (url == null) {
            throw new IllegalArgumentException("Test resource not found: " + resource);
        }

        try {
            return Paths.get(url.toURI()).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not resolve test resource " + resource, e);
        }
    }
}
