package team6;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

public class Scheduler {

    // REFERENCE CODE
    // https://www.geeksforgeeks.org/dsa/topological-sort-using-dfs/

    private static void findTopoSort(TaskNode node, Set<TaskNode> visited, Graph<TaskNode, DefaultWeightedEdge> adj, Stack<TaskNode> stack) {
        visited.add(node);

        List<TaskNode> successors = Graphs.successorListOf(adj, node);

        for (TaskNode n: successors) {
            if (!visited.contains(n)) {
                findTopoSort(n, visited, adj, stack);
            }
        }

        // push the node after visting all of its neighbours
        stack.push(node);
    }
    
    private static ArrayList<TaskNode> topoSort(Graph<TaskNode, DefaultWeightedEdge> adj) {
        Stack<TaskNode> stack = new Stack<>();
        Set<TaskNode> visited = new HashSet<>();

        for (TaskNode n: adj.vertexSet()) {
            if (!visited.contains(n)) {
                findTopoSort(n, visited, adj, stack);
            }
        }

        ArrayList<TaskNode> sorted = new ArrayList<>();

        // popping elements from stack and pushing into the list
        while (!stack.isEmpty()) {
            sorted.add(stack.pop());
        }
        return sorted;
    }

    // TESTING FUNCTION
    public static void testTopo(Graph<TaskNode, DefaultWeightedEdge> adj) {
        System.out.println(topoSort(adj));
    }

    /**
     * Calculates the earliest time that a task could start if it were placed on the
     * given processor. A task can only start once the processor is free and once every
     * incoming communication has arrived.
     *
     * @param graph             the task graph being scheduled
     * @param task              the task being placed
     * @param processor         the processor number (1 to nProcessors) being considered
     * @param processorFreeTime the time at which that processor finishes its already assigned tasks
     * @return the earliest valid start time for the task on that processor
     */
    public static int earliestStartTime(Graph<TaskNode, DefaultWeightedEdge> graph, TaskNode task, int processor, int processorFreeTime) {
        // Calculating start times

        int startTime = processorFreeTime;

        for(TaskNode predecessor : Graphs.predecessorListOf(graph, task)) {
            // Get the edge weight (communication time) from the predecessor to the current task
            DefaultWeightedEdge edge = graph.getEdge(predecessor, task);
            int communicationTime = (int) graph.getEdgeWeight(edge);

            // Calculate the finish time of the predecessor task
            int predecessorFinishTime = predecessor.getWeight() + communicationTime;

            // Update the start time if the predecessor finish time is greater
            if (predecessorFinishTime > startTime) {
                startTime = predecessorFinishTime;
            }
            startTime = Math.max(startTime, predecessorFinishTime);
        }
        return startTime;
    }

    // TODO Implement serial scheduler

    // might be useful?
    // https://www.geeksforgeeks.org/dsa/traveling-salesman-problem-using-branch-and-bound-2/
    // https://www.geeksforgeeks.org/dsa/job-assignment-problem-using-branch-and-bound/
    public static void serialScheduler(Graph g, int nProcessors) {
    
        // helpful stuff:
        // Graphs.successorListOf(g, node) -> gives succesor list of a certain node
        // Graphs.predecessorListOf(g, node) -> gives predecessor list of a certain node
    }


    // TODO Implement parrallel scheduler
    public static void parallelScheduler(Graph g, int nProcessors, int nCores) {
    }

}
