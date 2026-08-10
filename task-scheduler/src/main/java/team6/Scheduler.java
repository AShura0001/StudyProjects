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

    // get an ArrayList of all the adjences/successors of a node
    // private static ArrayList<ArrayList<TaskNode>> getAdjOfEachNode(Graph<TaskNode, DefaultWeightedEdge> graph) {

    //     ArrayList<ArrayList<TaskNode>> adj = new ArrayList<ArrayList<TaskNode>>();

    //     for (TaskNode n: graph.vertexSet()) {
    //         adj.add(new ArrayList<>(Graphs.successorListOf(graph, n)));
    //     }

    //     return adj;
    // }

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


    
    // TODO Implement serial scheduler

    // might be useful?
    // https://www.geeksforgeeks.org/dsa/traveling-salesman-problem-using-branch-and-bound-2/
    // https://www.geeksforgeeks.org/dsa/job-assignment-problem-using-branch-and-bound/
    public static void serialScheduler(Graph g, int nProcessors) {

        // schedule tasks with costly processor transfer costs on current processor

        Set<TaskNode> allNodes = g.vertexSet();

        // Array list that represents each processor and the nodes in each processor
        ArrayList<ArrayList<TaskNode>> processorList = new ArrayList<>(); 
        // add an arraylist for each processor
        for (int i = 0; i < nProcessors; i++) {
            processorList.add(new ArrayList<TaskNode>());
        }

        // for all i ∈ free(s) do
        //     for all P ∈ P do
        //         Create new state: i scheduled on P as early as possible


        // LOGIC
        // maybe find shortest path to the final task then back track to add non added nodes?

        // (this assumes that not all predecessors need to be scheduled before a task can be scheduled)
        // start at root, and set its start time and processor time
        // look at root's children and pick the task that can be started and finished the fastest
        // follow that child until the end of graph
        // 
        // then go to each unscheduled node and schedule them?
        


    }


    // TODO Implement parrallel scheduler
    public static void parallelScheduler(Graph g, int nProcessors, int nCores) {
    }

}
