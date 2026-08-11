# 6 - Dream Team (Cooked Team)

Finds the shortest possible schedule for a task graph on a fixed number of processors.

You give it a DAG in dot format, where every task has a run time and every edge has a
communication cost. That cost only applies if the two tasks end up on different processors,
which is what makes the problem interesting - spreading work out isn't automatically better.
The scheduler picks a processor and a start time for every task and minimises the makespan.

The search is exhaustive, so the schedule it returns is genuinely optimal, not just good.

## Getting started

You'll need Java 17+ and Maven. From the `task-scheduler` directory:

```
mvn clean package
```

That gives you `target/scheduler.jar`.

Heads up: the tests actually search for optimal schedules, so a full build takes about two
and a half minutes. While you're working on something, use `mvn clean package -DskipTests`
and save the real run for before you push.

## Running it

```
java -jar scheduler.jar INPUT.dot P [OPTION]

INPUT.dot  a task graph with integer weights in dot format
P          number of processors to schedule the INPUT graph on

Optional:
-p N       use N cores for execution in parallel (default is sequential)
-v         visualise the search (not implemented yet)
-o OUTPUT  output file is named OUTPUT (default is INPUT-output.dot)
```

Schedule the example graph on two processors:

```
java -jar target/scheduler.jar src/main/resources/taskGraphExamples/example.dot 2
```

Bigger graph, four processors, searching with eight cores, and naming the output:

```
java -jar target/scheduler.jar src/main/resources/taskGraphExamples/Nodes_11_OutTree.dot 4 -p 8 -o myschedule
```

There are example graphs from 7 to 11 tasks in `src/main/resources/taskGraphExamples`.
The bigger ones on 3 or 4 processors take a while - that's the search doing its job.

## Input and output

Tasks carry a `Weight`, edges carry a `Weight` for their communication cost. Names can be
any string, and anything else in the file (colours, labels, layout hints) is ignored.

```
digraph "example" {
	a		 [Weight=2];
	b		 [Weight=3];
	a -> b	 [Weight=1];
}
```

You get the same graph back with `Start` and `Processor` added to each task. Processors are
numbered from 1.

```
digraph "example-output" {
	a	 [Weight=2,Start=0,Processor=1];
	b	 [Weight=3,Start=2,Processor=1];
	a -> b	 [Weight=1];
}
```

## How it works

A greedy pass schedules everything quickly to get a valid schedule up front. That gives the
branch-and-bound search an upper bound to work with straight away, so it can start throwing
away dead ends immediately instead of wandering into them.

The search then walks the tree of possible task-to-processor assignments and drops any
partial schedule that already can't beat the best one found so far. Three things feed the
bound: how long the partial schedule already is, the longest remaining chain of dependent
tasks, and the leftover work spread evenly across the processors. Whichever is largest wins.

`-p N` splits that search across N threads. They share the current best schedule, so a good
bound found by one thread immediately prunes work for all the others.

## Layout

```
task-scheduler/
  src/main/java/team6/
    Main.java                   entry point
    CommandLineInterface.java   argument parsing and program flow
    FileHandler.java            reading and writing dot files
    Scheduler.java              the scheduling algorithms
    TaskNode.java               a task, its weight, start time and processor
    Visualiser.java             visualisation (not implemented yet)
  src/main/resources/           example task graphs
  src/test/java/team6/          tests
  src/test/resources/           task graphs used by the tests
```

## Tests

```
mvn test
```

Each example graph gets scheduled on 2 and 4 processors, and the result is checked against
the known optimal length for that graph. If a change breaks optimality, these fail.

## Libraries

Everything under `src` is ours. We use two external libraries, declared in `pom.xml` and
unmodified:

- **JGraphT 1.5.3** (`jgrapht-core`, `jgrapht-io`) - graph data structure, and parsing the
  input dot file. Output is written by us. Dual licensed LGPL 2.1 / EPL 2.0.
- **JUnit Jupiter 5.10.0** - tests only, not shipped inside the jar. EPL 2.0.

## Team

| Name | GitHub |
| --- | --- |
| TODO | TODO |
| TODO | TODO |
| TODO | TODO |
| TODO | TODO |
| TODO | TODO |
