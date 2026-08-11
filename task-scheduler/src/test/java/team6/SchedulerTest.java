package team6;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

public class SchedulerTest {

    // --- 2 PROCESSORS BENCHMARKS ---

    @Test
    public void testNode7OutTree2P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_7_OutTree.dot");
        Scheduler.serialScheduler(graph, 2);
        assertEquals(28, Scheduler.getScheduleLength(graph), "Node_7_OutTree on 2 processors should be 28");
    }

    @Test
    public void testNode8Random2P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_8_Random.dot");
        Scheduler.serialScheduler(graph, 2);
        assertEquals(581, Scheduler.getScheduleLength(graph), "Node_8_Random on 2 processors should be 581");
    }

    @Test
    public void testNode9SeriesParallel2P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_9_SeriesParallel.dot");
        Scheduler.serialScheduler(graph, 2);
        assertEquals(55, Scheduler.getScheduleLength(graph), "Node_9_SeriesParallel on 2 processors should be 55");
    }

    @Test
    public void testNode10Random2P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_10_Random.dot");
        Scheduler.serialScheduler(graph, 2);
        assertEquals(50, Scheduler.getScheduleLength(graph), "Node_10_Random on 2 processors should be 50");
    }

    @Test
    public void testNode11OutTree2P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot");
        Scheduler.serialScheduler(graph, 2);
        assertEquals(350, Scheduler.getScheduleLength(graph), "Node_11_OutTree on 2 processors should be 350");
    }

    // --- 4 PROCESSORS BENCHMARKS ---

    @Test
    public void testNode7OutTree4P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_7_OutTree.dot");
        Scheduler.serialScheduler(graph, 4);
        assertEquals(22, Scheduler.getScheduleLength(graph), "Node_7_OutTree on 4 processors should be 22");
    }

    @Test
    public void testNode8Random4P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_8_Random.dot");
        Scheduler.serialScheduler(graph, 4);
        assertEquals(581, Scheduler.getScheduleLength(graph), "Node_8_Random on 4 processors should be 581");
    }

    @Test
    public void testNode9SeriesParallel4P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_9_SeriesParallel.dot");
        Scheduler.serialScheduler(graph, 4);
        assertEquals(55, Scheduler.getScheduleLength(graph), "Node_9_SeriesParallel on 4 processors should be 55");
    }

    @Test
    public void testNode10Random4P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_10_Random.dot");
        Scheduler.serialScheduler(graph, 4);
        assertEquals(50, Scheduler.getScheduleLength(graph), "Node_10_Random on 4 processors should be 50");
    }

    @Test
    public void testNode11OutTree4P() {
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph("src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot");
        Scheduler.serialScheduler(graph, 4);
        assertEquals(227, Scheduler.getScheduleLength(graph), "Node_11_OutTree on 4 processors should be 227");
    }
}