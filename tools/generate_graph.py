#!/usr/bin/env python3
"""Generates a random DAG task graph in the .dot format used by
src/main/resources/taskGraphExamples/*.dot.

Node ids are 0..N-1. Every edge i -> j satisfies i < j, which guarantees the
graph is acyclic without needing a separate cycle check. Node 0 has no
incoming edges among 1..N-1; every other node gets at least one predecessor
chosen at random from the nodes before it, so the graph is connected, then
extra edges are added at random until the requested density is reached.

CCR (communication-to-computation ratio) is the ratio of total edge weight to
total node weight. A high CCR (as in the existing Nodes_10_Random.dot, CCR=10)
makes communication so expensive that the optimal schedule collapses onto one
processor and the branch-and-bound search prunes almost everything. A low CCR
(0.5-2.0) makes spreading work across processors worthwhile, so the search
actually has to explore.

Usage:
    python tools/generate_graph.py --nodes 20 --density 1.9 --ccr 1.0 \
        --seed 1 -o src/main/resources/taskGraphExamples/Nodes_20_Demo.dot
"""

import argparse
import random


def generate(nodes, density, ccr, seed, min_node_weight, max_node_weight):
    rng = random.Random(seed)

    node_weights = [rng.randint(min_node_weight, max_node_weight) for _ in range(nodes)]

    edges = set()
    for j in range(1, nodes):
        i = rng.randint(0, j - 1)
        edges.add((i, j))

    target_edges = max(len(edges), round(density * nodes))
    all_possible = [(i, j) for i in range(nodes) for j in range(i + 1, nodes)]
    rng.shuffle(all_possible)
    for i, j in all_possible:
        if len(edges) >= target_edges:
            break
        edges.add((i, j))

    avg_node_weight = sum(node_weights) / nodes
    target_avg_edge_weight = max(1.0, ccr * avg_node_weight)
    low = max(1, round(target_avg_edge_weight * 0.5))
    high = max(low, round(target_avg_edge_weight * 1.5))
    edge_weights = {edge: rng.randint(low, high) for edge in edges}

    return node_weights, edge_weights


def write_dot(path, node_weights, edge_weights, density, ccr):
    nodes = len(node_weights)
    total_seq_time = sum(node_weights)
    name = f"Random_Nodes_{nodes}_Density_{density:.2f}_CCR_{ccr:.2f}_WeightType_Random"

    with open(path, "w", newline="\n") as f:
        f.write(f'digraph "{name}" {{\n')
        f.write("\tgraph [CCR=%s,\n" % ccr)
        f.write("\t\tDensity=%s,\n" % density)
        f.write('\t\t"No of edges"=%d,\n' % len(edge_weights))
        f.write('\t\t"No of nodes"=%d,\n' % nodes)
        f.write('\t\t"Node concurrency"=-1,\n')
        f.write('\t\t"Total sequential time"=%d\n' % total_seq_time)
        f.write("\t];\n")

        written_nodes = set()
        for (i, j), weight in edge_weights.items():
            for node in (i, j):
                if node not in written_nodes:
                    f.write(f"\t{node}\t [Weight={node_weights[node]}];\n")
                    written_nodes.add(node)
            f.write(f"\t{i} -> {j}\t [Weight={weight}];\n")

        for node in range(nodes):
            if node not in written_nodes:
                f.write(f"\t{node}\t [Weight={node_weights[node]}];\n")

        f.write("}\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--nodes", type=int, required=True, help="number of nodes")
    parser.add_argument("--density", type=float, default=1.9, help="target edges / nodes ratio")
    parser.add_argument("--ccr", type=float, default=1.0, help="target communication-to-computation ratio")
    parser.add_argument("--seed", type=int, default=1, help="random seed, for reproducibility")
    parser.add_argument("--min-node-weight", type=int, default=3)
    parser.add_argument("--max-node-weight", type=int, default=10)
    parser.add_argument("-o", "--output", required=True, help="output .dot path")
    args = parser.parse_args()

    node_weights, edge_weights = generate(
        args.nodes, args.density, args.ccr, args.seed, args.min_node_weight, args.max_node_weight
    )
    write_dot(args.output, node_weights, edge_weights, args.density, args.ccr)
    print(f"Wrote {args.output}: {args.nodes} nodes, {len(edge_weights)} edges, target CCR {args.ccr}")


if __name__ == "__main__":
    main()
