package team6;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

public class CommandLineInterface {
    private String[] args;

    String dotFile;
    int processorCount;
    int parallelCount;
    boolean visualiseSearch;
    String outputFile;

    public CommandLineInterface(String[] args) {
        this.args = args;
    }

    /**
     * Runs the CLI using the values defined in <code>args</code>
     */

    // CALL THIS TO RUN THE PROGRAM
    // java -jar target/task-scheduler-1.0-SNAPSHOT.jar src\main\resources\taskGraphExamples\Nodes_7_OutTree.dot 2
    public void run() {
        parseArgs();
        displayVals();

        System.out.println("\nReading dot file.");
        Graph<TaskNode, DefaultWeightedEdge> graph = FileHandler.readGraph(dotFile);

        System.out.println("Scheduling " + graph.vertexSet().size() + " tasks on " + processorCount + " processors.");
        int workerCount = Math.max(1, parallelCount);
        Scheduler.parallelScheduler(graph, processorCount, workerCount);
        System.out.println("Schedule length: " + Scheduler.getScheduleLength(graph));

        FileHandler.writeGraph(graph, outputFile);
        System.out.println("Schedule written to " + outputFile);
    }

    /**
     * Populates corresponding variables for each argument
     */
    private void parseArgs() {
        if (args.length < 2) {
            displayUsage();
            System.exit(1);
        }
        this.dotFile = args[0];
        // Check that the provided file is a valid .dot file
        if (dotFile.length() < 5 || !dotFile.substring(dotFile.length() - 4, dotFile.length()).equals(".dot")) {
            System.out.println(
                    "Error: Invalid graph file provided.\n" +
                            "Ensure the file is provided in the format INPUT.dot");
            System.exit(1);
        }
        try {
            this.processorCount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println(
                    "Error: Invalid number of processors specified.\n" +
                            "Ensure the number is provided as an integer.");
            System.exit(1);
        }

        // Set default values before optional arguments
        this.parallelCount = 0;
        this.visualiseSearch = false;
        String inputName = dotFile.substring(0, dotFile.length() - 4);
        String outputName = inputName + "-output";
        // Process optional arguments
        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                switch (args[i]) {
                    case "-p":
                        // Check that number of cores is provided
                        if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                            try {
                                parallelCount = Integer.parseInt(args[i + 1]);
                            } catch (NumberFormatException e) {
                                System.out.println(
                                        "Error: Invalid number specified for number of cores for parallel execution.");
                                System.exit(1);
                            }
                        } else {
                            System.out.println("Error: -p argument requires a value. (-p N)");
                            System.exit(1);
                        }
                        i++;
                        break;
                    case "-v":
                        visualiseSearch = true;
                        break;
                    case "-o":
                        if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                            outputName = args[i + 1];
                        } else {
                            System.out.println("Error: -o argument requires a value. (-o OUTPUT)");
                            System.exit(1);
                        }
                        i++;
                        break;
                }
            }
        }
        this.outputFile = outputName + ".dot";
    }

    /**
     * Method that displays the currently stored values by printing to terminal.
     */
    public void displayVals() {
        System.out.println("Current Values:\n" +
                "dotFile: " + dotFile + "\n" +
                "processorCount: " + Integer.toString(processorCount) + "\n" +
                "parallelCount: " + Integer.toString(parallelCount) + "\n" +
                "visualiseSearch: " + Boolean.toString(visualiseSearch) + "\n" +
                "outputFile: " + outputFile);
    }

    /**
     * Displays how to use the program by printing to terminal.
     */
    private void displayUsage() {
        System.out.println(
                "Usage: java -jar scheduler.jar INPUT.dot P [OPTION]\n" +
                        "INPUT.dot  a task graph with integer weights in dot format\n" +
                        "P          number of processors to schedule the INPUT graph on\n\n" +
                        "Optional:\n" +
                        "-p N       use N cores for execution in parallel (default is sequential)\n" +
                        "-v         visualise the search\n" +
                        "-o OUTPUT  output file is named OUTPUT (default is INPUT-output.dot)");
    }

}
