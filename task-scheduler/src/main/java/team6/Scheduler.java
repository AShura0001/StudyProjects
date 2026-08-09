package team6;

import java.util.ArrayList;
import java.util.Set;

import org.jgrapht.Graph;

public class Scheduler {


    

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
