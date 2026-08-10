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
        // Only one task runs on a processor at a time, so we must wait for it to be free.
        int startTime = processorFreeTime;

        for (TaskNode predecessor : Graphs.predecessorListOf(graph, task)) {
            // The predecessor must have finished running before its data is available.
            int dataReadyTime = predecessor.getStartTime() + predecessor.getWeight();

            // Communication only costs time when the data has to travel between processors.
            if (predecessor.getProcessorNum() != processor) {
                DefaultWeightedEdge edge = graph.getEdge(predecessor, task);
                dataReadyTime += (int) graph.getEdgeWeight(edge);
            }

            // Update the start time if this incoming communication arrives later.
            startTime = Math.max(startTime, dataReadyTime);
        }
        return startTime;
    }

    /**
     * Chooses the processor that allows the given task to start as early as possible.
     * Ties are broken by choosing the lowest processor number.
     *
     * @param graph              the task graph being scheduled
     * @param task               the task being placed
     * @param processorFreeTimes the time each processor becomes available, indexed by processor number - 1
     * @return the processor number (1 to nProcessors) the task should be placed on
     */
    private static int selectProcessor(Graph<TaskNode, DefaultWeightedEdge> graph, TaskNode task, int[] processorFreeTimes) {
        
        int bestProcessor = 1;
        int bestStartTime = earliestStartTime(graph, task, 1, processorFreeTimes[0]);

        for(int processor = 2; processor <= processorFreeTimes.length; processor++){
            int StartTime = earliestStartTime(graph, task, processor, processorFreeTimes[processor - 1]);

            if(StartTime < bestStartTime){
                bestStartTime = StartTime;
                bestProcessor = processor;
            }
        }
        
        return bestProcessor;
    }

    // TODO Implement serial scheduler

    /**
     * Schedules every task in the graph onto the given number of processors using a greedy
     * list scheduling heuristic. Tasks are considered in topological order and each one is
     * placed on the processor that lets it start earliest. The chosen start time and
     * processor number are stored on each <code>TaskNode</code>.
     * <p>
     * The resulting schedule is always valid, but it is not guaranteed to be optimal.
     *
     * @param graph       the task graph to schedule
     * @param nProcessors the number of processors to schedule the graph on
     */
    public static void serialScheduler(Graph<TaskNode, DefaultWeightedEdge> graph, int nProcessors) {
    
        // helpful stuff:
        // Graphs.successorListOf(g, node) -> gives succesor list of a certain node
        // Graphs.predecessorListOf(g, node) -> gives predecessor list of a certain node

        if(nProcessors < 1){
            throw new IllegalArgumentException("Number of processors must be at least 1.");
        }

        // Tracks when each processor will be free to run a new task. Indexed by processor number - 1.
        int[] processorFreeTimes = new int[nProcessors];

        // Topological order makes sure that each predecessor is scheduled before its successors.
        for(TaskNode task : topoSort(graph)){
            int processor = selectProcessor(graph, task, processorFreeTimes);
            int startTime = earliestStartTime(graph, task, processor, processorFreeTimes[processor - 1]);

            task.setProcessorNum(processor);
            task.setStartTime(startTime);
            processorFreeTimes[processor - 1] = startTime + task.getWeight();

        }
    }

    /**
     * Returns the length (makespan) of the schedule currently stored on the graph's tasks,
     * which is the finish time of the last task to complete.
     *
     * @param graph a graph whose tasks have already been scheduled
     * @return the length of the schedule
     */
    public static int getScheduleLength(Graph<TaskNode, DefaultWeightedEdge> graph) {
         int scheduleLength = 0;

         for(TaskNode task : graph.vertexSet()) {
             scheduleLength = Math.max(scheduleLength, task.getStartTime() + task.getWeight());
         }
        
        return scheduleLength;
    }


    // TODO Implement parrallel scheduler
    
    // might be useful?
    // https://www.geeksforgeeks.org/dsa/traveling-salesman-problem-using-branch-and-bound-2/
    // https://www.geeksforgeeks.org/dsa/job-assignment-problem-using-branch-and-bound/
    
    public static void parallelScheduler(Graph g, int nProcessors, int nCores) {
    }

}
