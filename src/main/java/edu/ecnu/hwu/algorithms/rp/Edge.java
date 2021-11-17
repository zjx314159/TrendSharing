package edu.ecnu.hwu.algorithms.rp;

public class Edge implements Comparable<Edge> {
    int src, dest;
    double weight;

    // Comparator function used for sorting edges  
    // based on their weight 
    public int compareTo(Edge compareEdge) {
        return Double.compare(this.weight, compareEdge.weight);
    }
} 