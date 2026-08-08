package team6;

// TODO figure out how to make the jar file take arguments when you invoke it
// i think this article explains it (https://www.geeksforgeeks.org/java/command-line-arguments-in-java/)
public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println(
                    "Usage: java −jar scheduler.jar INPUT.dot P [OPTION]\n" +
                            "INPUT.dot  a task graph with integer weights in dot format\n" +
                            "P          number of processors to schedule the INPUT graph on\n\n" +
                            "Optional:\n" +
                            "−p N       use N cores for execution in parallel (default is sequential)\n" +
                            "−v         visualise the search\n" +
                            "−o OUPUT   output file is named OUTPUT (default is INPUT−output.dot)");
            return;
        }
        String dotFile = args[0];
        // Check that the provided file is a valid .dot file
        if (dotFile.length() < 5 || !dotFile.substring(dotFile.length() - 4, dotFile.length()).equals(".dot")) {
            System.out.println(
                    "Error: Invalid graph file provided.\n" +
                            "Ensure the file is provided in the format INPUT.dot");
            return;
        }
        int processorCount;
        try {
            processorCount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println(
                    "Error: Invalid number of processors specified.\n" +
                            "Ensure the number is provided as an integer.");
            return;
        }

        int parallelCount = 0;
        boolean visualiseSearch = false;
        String inputName = dotFile.substring(0, dotFile.length() - 4);
        String outputFile = inputName + "-output.dot";

        // Process optional requirements
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
                                return;
                            }
                        } else {
                            System.out.println("Error: -p argument requires a value. (-p N)");
                            return;
                        }
                        i++;
                        break;
                }
            }
        }

        System.out.println("Detected Arguments:\n" +
                "dotFile: " + dotFile + "\n" +
                "processorCount: " + Integer.toString(processorCount) + "\n" +
                "parallelCount: " + Integer.toString(parallelCount) + "\n" +
                "visualiseSearch: " + Boolean.toString(visualiseSearch) + "\n" +
                "outputFile: " + outputFile);
    }
}
