package team6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

public class Scheduler {

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

    private static Map<TaskNode, Integer> computeBottomLevels(Graph<TaskNode, DefaultWeightedEdge> graph) {
        Map<TaskNode, Integer> memo = new HashMap<>();
        Map<TaskNode, Integer> visiting = new HashMap<>();

        for (TaskNode task : graph.vertexSet()) {
            computeBottomLevel(graph, task, memo, visiting);
        }

        return memo;
    }

    private static int computeBottomLevel(
            Graph<TaskNode, DefaultWeightedEdge> graph,
            TaskNode task,
            Map<TaskNode, Integer> memo,
            Map<TaskNode, Integer> visiting) {
        Integer cached = memo.get(task);
        if (cached != null) {
            return cached;
        }

        if (visiting.containsKey(task)) {
            throw new IllegalArgumentException("Graph contains a cycle.");
        }
        visiting.put(task, 1);

        int bestSuccessorPath = 0;
        for (TaskNode successor : Graphs.successorListOf(graph, task)) {
            bestSuccessorPath = Math.max(bestSuccessorPath, computeBottomLevel(graph, successor, memo, visiting));
        }

        int level = task.getWeight() + bestSuccessorPath;
        visiting.remove(task);
        memo.put(task, level);
        return level;
    }

    private static void greedyScheduler(Graph<TaskNode, DefaultWeightedEdge> graph, int nProcessors) {
        if (nProcessors < 1) {
            throw new IllegalArgumentException("Number of processors must be at least 1.");
        }

        if (graph.vertexSet().isEmpty()) {
            return;
        }

        Map<TaskNode, Integer> bottomLevels = computeBottomLevels(graph);
        Map<TaskNode, Integer> remainingPredecessors = new HashMap<>();
        List<TaskNode> readyTasks = new ArrayList<>();
        Set<TaskNode> scheduledTasks = new HashSet<>();
        int[] processorFreeTimes = new int[nProcessors];

        for (TaskNode task : graph.vertexSet()) {
            task.setStartTime(0);
            task.setProcessorNum(0);
            int predecessorCount = Graphs.predecessorListOf(graph, task).size();
            remainingPredecessors.put(task, predecessorCount);
            if (predecessorCount == 0) {
                readyTasks.add(task);
            }
        }

        while (!readyTasks.isEmpty()) {
            readyTasks.sort((left, right) -> {
                int compare = Integer.compare(bottomLevels.getOrDefault(right, 0), bottomLevels.getOrDefault(left, 0));
                if (compare != 0) {
                    return compare;
                }

                compare = Integer.compare(right.getWeight(), left.getWeight());
                if (compare != 0) {
                    return compare;
                }

                return left.getId().compareTo(right.getId());
            });

            TaskNode task = readyTasks.remove(0);
            int bestProcessor = 1;
            int bestStartTime = Integer.MAX_VALUE;
            int bestFinishTime = Integer.MAX_VALUE;

            for (int processor = 1; processor <= nProcessors; processor++) {
                int startTime = earliestStartTime(graph, task, processor, processorFreeTimes[processor - 1]);
                int finishTime = startTime + task.getWeight();

                if (finishTime < bestFinishTime || (finishTime == bestFinishTime && startTime < bestStartTime)) {
                    bestFinishTime = finishTime;
                    bestStartTime = startTime;
                    bestProcessor = processor;
                }
            }

            task.setProcessorNum(bestProcessor);
            task.setStartTime(bestStartTime);
            processorFreeTimes[bestProcessor - 1] = bestFinishTime;
            scheduledTasks.add(task);

            for (TaskNode successor : Graphs.successorListOf(graph, task)) {
                if (scheduledTasks.contains(successor)) {
                    continue;
                }

                int remaining = remainingPredecessors.get(successor) - 1;
                remainingPredecessors.put(successor, remaining);
                if (remaining == 0) {
                    readyTasks.add(successor);
                }
            }
        }

        if (scheduledTasks.size() != graph.vertexSet().size()) {
            throw new IllegalArgumentException("Graph contains a cycle.");
        }
    }

    /**
     * Schedules every task in the graph onto the given number of processors using a branch-and-bound
     * search seeded by a greedy list-scheduling heuristic. The search explores processor assignments
     * for ready tasks and keeps the best feasible schedule found.
     *
     * @param graph       the task graph to schedule
     * @param nProcessors the number of processors to schedule the graph on
     */
    public static void serialScheduler(Graph<TaskNode, DefaultWeightedEdge> graph, int nProcessors) {
        if (nProcessors < 1) {
            throw new IllegalArgumentException("Number of processors must be at least 1.");
        }

        if (graph.vertexSet().isEmpty()) {
            return;
        }

        ScheduleProblem problem = buildProblem(graph, nProcessors);
        greedyScheduler(graph, nProcessors);
        ScheduleSnapshot initialSnapshot = snapshotFromGraph(problem);

        AtomicInteger bestLength = new AtomicInteger(initialSnapshot.makespan);
        AtomicReference<ScheduleSnapshot> bestSnapshot = new AtomicReference<>(initialSnapshot);

        SearchState root = createInitialState(problem);
        depthFirstSearch(problem, root, computeLowerBound(problem, root), bestLength, bestSnapshot, new HashSet<>(), SearchMonitor.disabled(), 0);

        applySnapshotToGraph(problem, bestSnapshot.get());
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

    private static final class ScheduleProblem {
        final List<TaskNode> tasks;
        final Map<TaskNode, Integer> taskIndex;
        final int[] weights;
        final int[][] predecessors;
        final int[][] predecessorCosts;
        final int[][] successors;
        final int[] bottomLevels;
        final int totalWeight;
        final int nProcessors;

        ScheduleProblem(Graph<TaskNode, DefaultWeightedEdge> graph, int nProcessors) {
            this.nProcessors = nProcessors;
            this.tasks = new ArrayList<>(graph.vertexSet());
            this.taskIndex = new HashMap<>();

            for (int i = 0; i < tasks.size(); i++) {
                taskIndex.put(tasks.get(i), i);
            }

            int nTasks = tasks.size();
            this.weights = new int[nTasks];
            this.predecessors = new int[nTasks][];
            this.predecessorCosts = new int[nTasks][];
            this.successors = new int[nTasks][];
            int weightSum = 0;

            for (int i = 0; i < nTasks; i++) {
                TaskNode task = tasks.get(i);
                weights[i] = task.getWeight();
                weightSum += task.getWeight();

                List<TaskNode> predecessorNodes = Graphs.predecessorListOf(graph, task);
                int[] predecessorIndices = new int[predecessorNodes.size()];
                int[] predecessorWeights = new int[predecessorNodes.size()];
                for (int j = 0; j < predecessorNodes.size(); j++) {
                    TaskNode predecessor = predecessorNodes.get(j);
                    predecessorIndices[j] = taskIndex.get(predecessor);
                    predecessorWeights[j] = (int) graph.getEdgeWeight(graph.getEdge(predecessor, task));
                }
                predecessors[i] = predecessorIndices;
                predecessorCosts[i] = predecessorWeights;

                List<TaskNode> successorNodes = Graphs.successorListOf(graph, task);
                int[] successorIndices = new int[successorNodes.size()];
                for (int j = 0; j < successorNodes.size(); j++) {
                    successorIndices[j] = taskIndex.get(successorNodes.get(j));
                }
                successors[i] = successorIndices;
            }

            this.bottomLevels = new int[nTasks];
            Arrays.fill(bottomLevels, -1);
            for (int i = 0; i < nTasks; i++) {
                computeBottomLevel(i);
            }

            this.totalWeight = weightSum;
        }

        private int computeBottomLevel(int taskIndex) {
            if (bottomLevels[taskIndex] != -1) {
                return bottomLevels[taskIndex];
            }

            int bestSuccessorPath = 0;
            for (int successor : successors[taskIndex]) {
                bestSuccessorPath = Math.max(bestSuccessorPath, computeBottomLevel(successor));
            }

            bottomLevels[taskIndex] = weights[taskIndex] + bestSuccessorPath;
            return bottomLevels[taskIndex];
        }
    }

    private static final class SearchState {
        final int[] startTimes;
        final int[] processors;
        final int[] processorFreeTimes;
        final boolean[] scheduled;
        final ArrayList<Integer> readyTasks;
        int scheduledCount;
        int currentMakespan;
        int remainingWeight;

        SearchState(int nTasks, int nProcessors, int totalWeight) {
            this.startTimes = new int[nTasks];
            this.processors = new int[nTasks];
            this.processorFreeTimes = new int[nProcessors];
            this.scheduled = new boolean[nTasks];
            this.readyTasks = new ArrayList<>();
            this.remainingWeight = totalWeight;
        }

        SearchState(SearchState other) {
            this.startTimes = other.startTimes.clone();
            this.processors = other.processors.clone();
            this.processorFreeTimes = other.processorFreeTimes.clone();
            this.scheduled = other.scheduled.clone();
            this.readyTasks = new ArrayList<>(other.readyTasks);
            this.scheduledCount = other.scheduledCount;
            this.currentMakespan = other.currentMakespan;
            this.remainingWeight = other.remainingWeight;
        }
    }

    private static final class ScheduleSnapshot {
        final int[] startTimes;
        final int[] processors;
        final int makespan;

        ScheduleSnapshot(int[] startTimes, int[] processors, int makespan) {
            this.startTimes = startTimes;
            this.processors = processors;
            this.makespan = makespan;
        }
    }

    /**
     * Identifies a partial schedule by the (processor, start time) assigned to each task so
     * far. Unscheduled tasks always carry processor 0, so two states with the same startTimes
     * and processors arrays represent the exact same partial schedule, however different the
     * order was in which their tasks got placed. Independent ready tasks placed on different
     * processors reach an identical resulting state regardless of which was scheduled first,
     * so hashing on this key lets the search recognise and skip a state it has already fully
     * explored under an earlier, equally-thorough visit.
     */
    private static final class StateKey {
        private final int[] startTimes;
        private final int[] processors;
        private final int hash;

        StateKey(SearchState state) {
            // Defensive copies: the search mutates a shared SearchState's arrays in place
            // and undoes each change afterward, so a stored reference to the live arrays
            // would silently change underneath every key already sitting in the closed list.
            this.startTimes = state.startTimes.clone();
            this.processors = state.processors.clone();
            this.hash = 31 * Arrays.hashCode(startTimes) + Arrays.hashCode(processors);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof StateKey that)) {
                return false;
            }
            return Arrays.equals(this.startTimes, that.startTimes) && Arrays.equals(this.processors, that.processors);
        }
    }

    private static ScheduleProblem buildProblem(Graph<TaskNode, DefaultWeightedEdge> graph, int nProcessors) {
        return new ScheduleProblem(graph, nProcessors);
    }

    private static SearchState createInitialState(ScheduleProblem problem) {
        SearchState state = new SearchState(problem.tasks.size(), problem.nProcessors, problem.totalWeight);

        for (int taskIndex = 0; taskIndex < problem.tasks.size(); taskIndex++) {
            if (problem.predecessors[taskIndex].length == 0) {
                state.readyTasks.add(taskIndex);
            }
        }

        return state;
    }

    private static int earliestStartTime(ScheduleProblem problem, SearchState state, int taskIndex, int processorIndex) {
        int startTime = state.processorFreeTimes[processorIndex];

        for (int i = 0; i < problem.predecessors[taskIndex].length; i++) {
            int predecessorIndex = problem.predecessors[taskIndex][i];
            int predecessorFinish = state.startTimes[predecessorIndex] + problem.weights[predecessorIndex];
            if (state.processors[predecessorIndex] != processorIndex + 1) {
                predecessorFinish += problem.predecessorCosts[taskIndex][i];
            }
            startTime = Math.max(startTime, predecessorFinish);
        }

        return startTime;
    }

    private static int computeLowerBound(ScheduleProblem problem, SearchState state) {
        int longestRemainingPath = 0;

        for (int taskIndex = 0; taskIndex < problem.tasks.size(); taskIndex++) {
            if (!state.scheduled[taskIndex]) {
                longestRemainingPath = Math.max(longestRemainingPath, problem.bottomLevels[taskIndex]);
            }
        }

        // A ready task's earliest possible start is already known exactly, so the makespan
        // cannot be lower than that start plus the longest chain running through the task.
        // This is tighter than bottomLevel alone, which ignores processor availability and
        // communication delay entirely.
        int readyTaskBound = 0;
        for (int taskIndex : state.readyTasks) {
            int bestStart = Integer.MAX_VALUE;
            for (int processorIndex = 0; processorIndex < problem.nProcessors; processorIndex++) {
                bestStart = Math.min(bestStart, earliestStartTime(problem, state, taskIndex, processorIndex));
            }
            readyTaskBound = Math.max(readyTaskBound, bestStart + problem.bottomLevels[taskIndex]);
        }

        int workBound = (state.remainingWeight + problem.nProcessors - 1) / problem.nProcessors;
        return Math.max(state.currentMakespan, Math.max(longestRemainingPath, Math.max(workBound, readyTaskBound)));
    }

    private static ArrayList<Integer> orderedReadyTasks(ScheduleProblem problem, SearchState state) {
        ArrayList<Integer> ordered = new ArrayList<>(state.readyTasks);
        ordered.sort((left, right) -> {
            int compare = Integer.compare(problem.bottomLevels[right], problem.bottomLevels[left]);
            if (compare != 0) {
                return compare;
            }

            compare = Integer.compare(problem.weights[right], problem.weights[left]);
            if (compare != 0) {
                return compare;
            }

            return Integer.compare(left, right);
        });
        return ordered;
    }

    /**
     * @param state          the partial schedule being expanded
     * @param processorIndex the processor being checked (0 to nProcessors - 1)
     * @return true if the processor has no task scheduled on it yet
     */
    private static boolean isProcessorEmpty(SearchState state, int processorIndex) {
        return state.processorFreeTimes[processorIndex] == 0;
    }

    private static boolean allPredecessorsScheduled(ScheduleProblem problem, SearchState state, int taskIndex) {
        for (int predecessor : problem.predecessors[taskIndex]) {
            if (!state.scheduled[predecessor]) {
                return false;
            }
        }
        return true;
    }

    private static SearchState scheduleTask(ScheduleProblem problem, SearchState state, int taskIndex, int processorIndex) {
        SearchState child = new SearchState(state);

        int startTime = earliestStartTime(problem, child, taskIndex, processorIndex);
        child.startTimes[taskIndex] = startTime;
        child.processors[taskIndex] = processorIndex + 1;
        child.processorFreeTimes[processorIndex] = startTime + problem.weights[taskIndex];
        child.scheduled[taskIndex] = true;
        child.scheduledCount++;
        child.currentMakespan = Math.max(child.currentMakespan, startTime + problem.weights[taskIndex]);
        child.remainingWeight -= problem.weights[taskIndex];

        child.readyTasks.remove(Integer.valueOf(taskIndex));
        for (int successor : problem.successors[taskIndex]) {
            if (!child.scheduled[successor] && allPredecessorsScheduled(problem, child, successor) && !child.readyTasks.contains(successor)) {
                child.readyTasks.add(successor);
            }
        }

        return child;
    }

    /**
     * Records exactly what {@link #mutateSchedule} changed, so {@link #undoSchedule} can put
     * a shared, in-place-mutated SearchState back to precisely how it was before that move.
     */
    private static final class Undo {
        final int taskIndex;
        final int processorIndex;
        final int previousProcessorFreeTime;
        final int previousMakespan;
        final int[] addedReadyTasks;

        Undo(int taskIndex, int processorIndex, int previousProcessorFreeTime, int previousMakespan, int[] addedReadyTasks) {
            this.taskIndex = taskIndex;
            this.processorIndex = processorIndex;
            this.previousProcessorFreeTime = previousProcessorFreeTime;
            this.previousMakespan = previousMakespan;
            this.addedReadyTasks = addedReadyTasks;
        }
    }

    /**
     * Schedules a task directly onto the shared search state, in place, instead of cloning a
     * new one. Used only by depthFirstSearch, where the search backtracks through the same
     * state object rather than keeping many states alive at once (unlike buildFrontier, which
     * still needs {@link #scheduleTask}'s persistent clones). Every mutation made here must be
     * exactly reversed by the paired {@link #undoSchedule} call once the recursive call below
     * it returns, or every state explored afterward is corrupted.
     *
     * @return an Undo record describing exactly what to reverse
     */
    private static Undo mutateSchedule(ScheduleProblem problem, SearchState state, int taskIndex, int processorIndex) {
        int startTime = earliestStartTime(problem, state, taskIndex, processorIndex);
        int previousProcessorFreeTime = state.processorFreeTimes[processorIndex];
        int previousMakespan = state.currentMakespan;

        state.startTimes[taskIndex] = startTime;
        state.processors[taskIndex] = processorIndex + 1;
        state.processorFreeTimes[processorIndex] = startTime + problem.weights[taskIndex];
        state.scheduled[taskIndex] = true;
        state.scheduledCount++;
        state.currentMakespan = Math.max(state.currentMakespan, startTime + problem.weights[taskIndex]);
        state.remainingWeight -= problem.weights[taskIndex];

        state.readyTasks.remove(Integer.valueOf(taskIndex));

        int[] successors = problem.successors[taskIndex];
        int[] addedBuffer = new int[successors.length];
        int addedCount = 0;
        for (int successor : successors) {
            if (!state.scheduled[successor] && allPredecessorsScheduled(problem, state, successor) && !state.readyTasks.contains(successor)) {
                state.readyTasks.add(successor);
                addedBuffer[addedCount++] = successor;
            }
        }

        return new Undo(taskIndex, processorIndex, previousProcessorFreeTime, previousMakespan, Arrays.copyOf(addedBuffer, addedCount));
    }

    /**
     * Reverses exactly the mutation {@link #mutateSchedule} made, restoring the shared state to
     * how it was before that task was placed.
     */
    private static void undoSchedule(ScheduleProblem problem, SearchState state, Undo undo) {
        for (int added : undo.addedReadyTasks) {
            state.readyTasks.remove(Integer.valueOf(added));
        }
        state.readyTasks.add(undo.taskIndex);

        state.remainingWeight += problem.weights[undo.taskIndex];
        state.scheduledCount--;
        state.currentMakespan = undo.previousMakespan;
        state.processorFreeTimes[undo.processorIndex] = undo.previousProcessorFreeTime;
        state.scheduled[undo.taskIndex] = false;
        state.processors[undo.taskIndex] = 0;
        state.startTimes[undo.taskIndex] = 0;
    }

    private static void updateBest(SearchState state, AtomicInteger bestLength, AtomicReference<ScheduleSnapshot> bestSnapshot, SearchMonitor monitor) {
        int candidateLength = state.currentMakespan;

        while (true) {
            int currentBest = bestLength.get();
            if (candidateLength >= currentBest) {
                return;
            }

            if (bestLength.compareAndSet(currentBest, candidateLength)) {
                int[] startTimes = state.startTimes.clone();
                int[] processors = state.processors.clone();
                bestSnapshot.set(new ScheduleSnapshot(startTimes, processors, candidateLength));
                monitor.recordBest(candidateLength, startTimes, processors);
                return;
            }
        }
    }

    private static void depthFirstSearch(ScheduleProblem problem, SearchState state, int lowerBound, AtomicInteger bestLength, AtomicReference<ScheduleSnapshot> bestSnapshot, Set<StateKey> visited, SearchMonitor monitor, int worker) {
        monitor.recordVisit(worker, state.scheduledCount);

        if (lowerBound >= bestLength.get()) {
            return;
        }

        if (!visited.add(new StateKey(state))) {
            return;
        }

        if (state.scheduledCount == problem.tasks.size()) {
            updateBest(state, bestLength, bestSnapshot, monitor);
            return;
        }

        ArrayList<Integer> readyTasks = orderedReadyTasks(problem, state);
        for (int taskIndex : readyTasks) {
            boolean triedEmptyProcessor = false;
            for (int processorIndex = 0; processorIndex < problem.nProcessors; processorIndex++) {
                boolean isEmpty = isProcessorEmpty(state, processorIndex);
                if (isEmpty) {
                    if (triedEmptyProcessor) {
                        continue;
                    }
                    triedEmptyProcessor = true;
                }

                monitor.recordCurrent(worker, taskIndex, processorIndex);
                Undo undo = mutateSchedule(problem, state, taskIndex, processorIndex);

                int childBound = computeLowerBound(problem, state);
                if (childBound < bestLength.get()) {
                    depthFirstSearch(problem, state, childBound, bestLength, bestSnapshot, visited, monitor, worker);
                }

                undoSchedule(problem, state, undo);
            }
        }
    }

    private static List<SearchState> buildFrontier(ScheduleProblem problem, SearchState root, AtomicInteger bestLength, int targetSize) {
        ArrayList<SearchState> frontier = new ArrayList<>();
        frontier.add(root);
        int depth = 0;

        while (frontier.size() < targetSize && depth < 3) {
            ArrayList<SearchState> nextLevel = new ArrayList<>();

            for (SearchState state : frontier) {
                if (state.scheduledCount == problem.tasks.size()) {
                    nextLevel.add(state);
                    continue;
                }

                if (computeLowerBound(problem, state) >= bestLength.get()) {
                    continue;
                }

                ArrayList<Integer> readyTasks = orderedReadyTasks(problem, state);
                for (int taskIndex : readyTasks) {
                    boolean triedEmptyProcessor = false;
                    for (int processorIndex = 0; processorIndex < problem.nProcessors; processorIndex++) {
                        boolean isEmpty = isProcessorEmpty(state, processorIndex);
                        if (isEmpty) {
                            if (triedEmptyProcessor) {
                                continue;
                            }
                            triedEmptyProcessor = true;
                        }

                        SearchState child = scheduleTask(problem, state, taskIndex, processorIndex);
                        if (computeLowerBound(problem, child) < bestLength.get()) {
                            nextLevel.add(child);
                        }
                    }
                }
            }

            if (nextLevel.isEmpty()) {
                break;
            }

            frontier = nextLevel;
            depth++;
        }

        return frontier;
    }

    private static ScheduleSnapshot snapshotFromGraph(ScheduleProblem problem) {
        int[] startTimes = new int[problem.tasks.size()];
        int[] processors = new int[problem.tasks.size()];
        int makespan = 0;

        for (int i = 0; i < problem.tasks.size(); i++) {
            TaskNode task = problem.tasks.get(i);
            startTimes[i] = task.getStartTime();
            processors[i] = task.getProcessorNum();
            makespan = Math.max(makespan, task.getStartTime() + task.getWeight());
        }

        return new ScheduleSnapshot(startTimes, processors, makespan);
    }

    private static void applySnapshotToGraph(ScheduleProblem problem, ScheduleSnapshot snapshot) {
        for (int i = 0; i < problem.tasks.size(); i++) {
            TaskNode task = problem.tasks.get(i);
            task.setStartTime(snapshot.startTimes[i]);
            task.setProcessorNum(snapshot.processors[i]);
        }
    }

    /**
     * Finds an optimal schedule using branch-and-bound. The search is seeded with the
     * greedy schedule as an initial upper bound and then split across multiple workers.
     */
    public static void parallelScheduler(Graph<TaskNode, DefaultWeightedEdge> graph, int nProcessors, int nCores) {
        parallelScheduler(graph, nProcessors, nCores, SearchMonitor.disabled());
    }

    /**
     * Same as {@link #parallelScheduler(Graph, int, int)}, publishing progress through monitor
     * as it searches.
     */
    public static void parallelScheduler(Graph<TaskNode, DefaultWeightedEdge> graph, int nProcessors, int nCores, SearchMonitor monitor) {
        monitor.markStarted();

        if (nProcessors < 1) {
            throw new IllegalArgumentException("Number of processors must be at least 1.");
        }

        if (graph.vertexSet().isEmpty()) {
            return;
        }

        ScheduleProblem problem = buildProblem(graph, nProcessors);

        greedyScheduler(graph, nProcessors);
        ScheduleSnapshot initialSnapshot = snapshotFromGraph(problem);
        monitor.recordBest(initialSnapshot.makespan, initialSnapshot.startTimes, initialSnapshot.processors);

        AtomicInteger bestLength = new AtomicInteger(initialSnapshot.makespan);
        AtomicReference<ScheduleSnapshot> bestSnapshot = new AtomicReference<>(initialSnapshot);

        SearchState root = createInitialState(problem);
        List<SearchState> frontier = buildFrontier(problem, root, bestLength, Math.max(1, nCores * 4));

        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, nCores), workerThreadFactory());
        CountDownLatch latch = new CountDownLatch(frontier.size());

        for (SearchState state : frontier) {
            executor.execute(() -> {
                // Read once per submitted task, never inside the depthFirstSearch recursion.
                int worker = WORKER_INDEX.get();
                monitor.workerStarted(worker);
                try {
                    depthFirstSearch(problem, state, computeLowerBound(problem, state), bestLength, bestSnapshot, new HashSet<>(), monitor, worker);
                } finally {
                    monitor.workerFinished(worker);
                    latch.countDown();
                }
            });
        }

        executor.shutdown();

        try {
            latch.await();
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        applySnapshotToGraph(problem, bestSnapshot.get());
    }

    private static final ThreadLocal<Integer> WORKER_INDEX = new ThreadLocal<>();

    /** Assigns each pool thread a stable index 0..nCores-1, read once per task via WORKER_INDEX. */
    private static ThreadFactory workerThreadFactory() {
        AtomicInteger nextIndex = new AtomicInteger();
        return runnable -> {
            int index = nextIndex.getAndIncrement();
            return new Thread(() -> {
                WORKER_INDEX.set(index);
                runnable.run();
            });
        };
    }

}
