package team6;

// TODO figure out how to make the jar file take arguments when you invoke it
// i think this article explains it (https://www.geeksforgeeks.org/java/command-line-arguments-in-java/)
public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println(
                    "Usage:\n" +
                            "java −jar scheduler.jar INPUT.dot P [OPTION]\n" +
                            "INPUT.dot a task graph with integer weights in dot format\n" +
                            "P number of processors to schedule the INPUT graph on\n\n" +
                            "Optional:\n" +
                            "−p N use N cores for execution in parallel (default is sequential)\n" +
                            "−v visualise the search\n" +
                            "−o OUPUT output file is named OUTPUT (default is INPUT−output.dot)");
        }
    }
}
