package team6;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class SchedulerTest {

    // --- SCHEDULER OPTIMALNESS TEST ---

    @ParameterizedTest(name = "{0} on {1} processors -> expected length {3}")
    @MethodSource("provideSchedulerArguments")
    public void testScheduler(String filepath, int processors, int cores, int expectedLength) {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph(filepath);
        Scheduler.parallelScheduler(graph, processors, cores);
        assertEquals(expectedLength, Scheduler.getScheduleLength(graph),
                filepath + " failed: got " + Scheduler.getScheduleLength(graph) + ", expected " + expectedLength);
    }

    static Stream<Arguments> provideSchedulerArguments() {
        return Stream.of(
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_7_OutTree.dot", 2, 1, 28),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_8_Random.dot", 2, 1, 581),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_9_SeriesParallel.dot", 2, 1, 55),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_10_Random.dot", 2, 1, 50),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot", 2, 1, 350),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_7_OutTree.dot", 4, 1, 22),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_8_Random.dot", 4, 1, 581),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_9_SeriesParallel.dot", 4, 1, 55),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_10_Random.dot", 4, 1, 50),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot", 4, 1, 227),

                // Same graphs and expected optimal lengths as above, but run with more than one
                // search core. The optimal length must not depend on how many threads found it,
                // and this is the only place that path (the ExecutorService/thread pool
                // coordination in parallelScheduler) is actually exercised with real concurrency
                // rather than a single-worker pool.
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_7_OutTree.dot", 2, 4, 28),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_8_Random.dot", 2, 4, 581),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_9_SeriesParallel.dot", 2, 4, 55),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_10_Random.dot", 2, 4, 50),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot", 2, 4, 350),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_7_OutTree.dot", 4, 4, 22),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_8_Random.dot", 4, 4, 581),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_9_SeriesParallel.dot", 4, 4, 55),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_10_Random.dot", 4, 4, 50),
                Arguments.of("src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot", 4, 4, 227),

                // Small graphs chosen for the shape of their precedence structure rather than
                // size, each with its optimal length confirmed independently by a separate,
                // unpruned brute-force search (not by trusting this algorithm's own output):
                // a pure chain (single successor at every step), a diamond (converging
                // predecessors), a fully independent task set (every task ready at once, the
                // case the empty-processor and closed-list pruning target hardest), and a mixed
                // shape combining branching, convergence, and two separate roots. Run at both
                // 1 and multiple cores.
                Arguments.of("src/test/resources/chain6.dot", 2, 1, 31),
                Arguments.of("src/test/resources/chain6.dot", 3, 1, 31),
                Arguments.of("src/test/resources/chain6.dot", 2, 3, 31),
                Arguments.of("src/test/resources/chain6.dot", 3, 3, 31),
                Arguments.of("src/test/resources/diamond.dot", 2, 1, 20),
                Arguments.of("src/test/resources/diamond.dot", 3, 1, 20),
                Arguments.of("src/test/resources/diamond.dot", 2, 3, 20),
                Arguments.of("src/test/resources/diamond.dot", 3, 3, 20),
                Arguments.of("src/test/resources/independent6.dot", 2, 1, 16),
                Arguments.of("src/test/resources/independent6.dot", 3, 1, 11),
                Arguments.of("src/test/resources/independent6.dot", 2, 3, 16),
                Arguments.of("src/test/resources/independent6.dot", 3, 3, 11),
                Arguments.of("src/test/resources/mixed8.dot", 2, 1, 25),
                Arguments.of("src/test/resources/mixed8.dot", 3, 1, 25),
                Arguments.of("src/test/resources/mixed8.dot", 2, 3, 25),
                Arguments.of("src/test/resources/mixed8.dot", 3, 3, 25)
        );
    }

    // --- SCHEDULER RESPECTS DEPENDANCIES TESTING ---

    @ParameterizedTest(name = "{0} Test scheduler respects dependancies")
    @ValueSource(strings = {
                "src/main/resources/taskGraphExamples/Nodes_7_OutTree.dot",
                "src/main/resources/taskGraphExamples/Nodes_8_Random.dot",
                "src/main/resources/taskGraphExamples/Nodes_9_SeriesParallel.dot",
                "src/main/resources/taskGraphExamples/Nodes_10_Random.dot",
                "src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot",
                "src/test/resources/chain6.dot",
                "src/test/resources/diamond.dot",
                "src/test/resources/independent6.dot",
                "src/test/resources/mixed8.dot"
    })
    public void testParallelScheduleRespectsDependencies(String filepath) {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph(filepath);

        Scheduler.parallelScheduler(graph, 2, 1);

        assertValidSchedule(graph);
    }

    // --- SCHEDULER DOES NOT CHANGE GRAPH VALUES TESTING ---

    @ParameterizedTest(name = "{0} Test scheduler doesnt change graph values")
    @ValueSource(strings = {
                "src/main/resources/taskGraphExamples/Nodes_7_OutTree.dot",
                "src/main/resources/taskGraphExamples/Nodes_8_Random.dot",
                "src/main/resources/taskGraphExamples/Nodes_9_SeriesParallel.dot",
                "src/main/resources/taskGraphExamples/Nodes_10_Random.dot",
                "src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot",
                "src/test/resources/chain6.dot",
                "src/test/resources/diamond.dot",
                "src/test/resources/independent6.dot",
                "src/test/resources/mixed8.dot"
    })
    public void testParallelScheduleDoesNotModifyGraphStructureOrWeights(String filepath) {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph(filepath);
        Graph<TaskNode, DefaultWeightedEdge> originalCopy = deepCopyGraph(graph);

        Scheduler.parallelScheduler(graph, 2, 1);

        assertGraphStructureAndWeightsEqual(originalCopy, graph);
    }

    // HELPER METHODS

    private void assertValidSchedule(Graph<TaskNode, DefaultWeightedEdge> graph) {
        for (TaskNode task : graph.vertexSet()) {
            int taskStart = task.getStartTime();
            int taskWeight = task.getWeight();

            assertTrue(taskStart >= 0, "Task " + task.getId() + " has a negative start time");
            assertTrue(taskWeight > 0, "Task " + task.getId() + " has a non-positive weight");

            for (TaskNode predecessor : Graphs.predecessorListOf(graph, task)) {
                int predecessorFinish = predecessor.getStartTime() + predecessor.getWeight();
                int communicationDelay = predecessor.getProcessorNum() == task.getProcessorNum()
                        ? 0
                        : (int) graph.getEdgeWeight(graph.getEdge(predecessor, task));
                assertTrue(taskStart >= predecessorFinish + communicationDelay,
                        "Task " + task.getId() + " starts too early relative to predecessor " + predecessor.getId());
            }
        }
    }

    private Graph<TaskNode, DefaultWeightedEdge> deepCopyGraph(Graph<TaskNode, DefaultWeightedEdge> graph) {
        Graph<TaskNode, DefaultWeightedEdge> copy = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Map<String, TaskNode> clones = new HashMap<>();

        for (TaskNode original : graph.vertexSet()) {
            TaskNode clone = new TaskNode(original.getId());
            clone.setWeight(original.getWeight());
            clone.setStartTime(original.getStartTime());
            clone.setProcessorNum(original.getProcessorNum());
            copy.addVertex(clone);
            clones.put(clone.getId(), clone);
        }

        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            TaskNode source = graph.getEdgeSource(edge);
            TaskNode target = graph.getEdgeTarget(edge);
            TaskNode sourceClone = clones.get(source.getId());
            TaskNode targetClone = clones.get(target.getId());
            DefaultWeightedEdge copyEdge = copy.addEdge(sourceClone, targetClone);
            copy.setEdgeWeight(copyEdge, graph.getEdgeWeight(edge));
        }

        return copy;
    }

    private void assertGraphStructureAndWeightsEqual(
            Graph<TaskNode, DefaultWeightedEdge> expected,
            Graph<TaskNode, DefaultWeightedEdge> actual) {

        assertEquals(expected.vertexSet().size(), actual.vertexSet().size(), "Vertex count changed");
        assertEquals(expected.edgeSet().size(), actual.edgeSet().size(), "Edge count changed");

        for (TaskNode expectedTask : expected.vertexSet()) {
            TaskNode actualTask = findTaskById(actual, expectedTask.getId());
            assertNotNull(actualTask, "Missing task: " + expectedTask.getId());
            assertEquals(expectedTask.getWeight(), actualTask.getWeight(),
                    "Weight changed for task " + expectedTask.getId());
        }

        for (DefaultWeightedEdge expectedEdge : expected.edgeSet()) {
            TaskNode expectedSource = expected.getEdgeSource(expectedEdge);
            TaskNode expectedTarget = expected.getEdgeTarget(expectedEdge);
            TaskNode actualSource = findTaskById(actual, expectedSource.getId());
            TaskNode actualTarget = findTaskById(actual, expectedTarget.getId());
            assertNotNull(actualSource, "Missing source task: " + expectedSource.getId());
            assertNotNull(actualTarget, "Missing target task: " + expectedTarget.getId());

            DefaultWeightedEdge actualEdge = actual.getEdge(actualSource, actualTarget);
            assertNotNull(actualEdge, "Missing edge: " + expectedSource.getId() + " -> " + expectedTarget.getId());
            assertEquals(expected.getEdgeWeight(expectedEdge), actual.getEdgeWeight(actualEdge), 0.0,
                    "Edge weight changed for " + expectedSource.getId() + " -> " + expectedTarget.getId());
        }
    }

    private TaskNode findTaskById(Graph<TaskNode, DefaultWeightedEdge> graph, String id) {
        return graph.vertexSet().stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
