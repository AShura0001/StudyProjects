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
