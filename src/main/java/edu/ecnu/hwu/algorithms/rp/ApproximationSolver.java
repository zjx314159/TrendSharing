package edu.ecnu.hwu.algorithms.rp;

import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;
import edu.ecnu.hwu.utils.DistanceUtils;

import java.util.*;

public class ApproximationSolver {
    static List<Task> tasks;
    static List<Worker> workers;
    static List<Point> points;

    public ApproximationSolver(List<Task> tasks, List<Worker> workers, List<Point> points) {
        this.tasks = tasks;
        this.workers = workers;
        this.points = points;
    }

    public List<Integer> getCycle(Set<Integer> tids, boolean isPickup) {
        List<int[]> pids = new ArrayList<>();
        Map<Integer, List<Integer>> p2t = new HashMap<>();
        //merge the same point into a single point
        for (var tid : tids) {
            Task t = tasks.get(tid);
            int pid = isPickup ? t.pid : t.did;
            if(!p2t.containsKey(pid)) {
                p2t.put(pid, new ArrayList<>());
                pids.add(new int[]{pids.size(), pid});
            }
            p2t.get(pid).add(tid);
        }
        //stores the order of pids[0]
        List<Integer> pidIndexCycle = solveSingleTsp(pids);
        List<Integer> pidTaskCycle = new ArrayList<>();
        //Note: the last point's corresponding task cannot all be added, since there will be duplicate tasks as the first point
        for(int i = 0; i < pidIndexCycle.size() - 1; i++) {
            int pid = pids.get(pidIndexCycle.get(i))[1];
            pidTaskCycle.addAll(p2t.get(pid));
        }
        pidTaskCycle.add(pidTaskCycle.get(0));
        return pidTaskCycle;
    }

    /**
     * path format:[workerPoint,taskId of pids,taskId of dids]
     *
     * @param curLid       cur location id of worker
     * @param pidTaskCycle pid index cycle
     * @param didTaskCycle did index cycle
     * @return best path:[curLid of worker | taskId * 2 + 1(pickup) | taskId * 2(delivery)]
     */
    public List<Integer> findBestPath(int curLid, List<Integer> pidTaskCycle, List<Integer> didTaskCycle,
                                      boolean pidPickup, boolean didPickup) {
        int m = pidTaskCycle.size();
        int n = didTaskCycle.size();
        double curMin = Double.MAX_VALUE;
        int[] indexes = new int[4];//0:sp,1:tp,2:sd,3:td
        for (int i = 0; i < m - 1; i++) {
            int[] p = new int[]{pidTaskCycle.get(i), pidTaskCycle.get(i + 1)};
            double pEdgeLen = taskDist(p[0], p[1], false);
            for (int j = 0; j < n - 1; j++) {
                int[] d = new int[]{didTaskCycle.get(j), didTaskCycle.get(j + 1)};
                double dEdgeLen = taskDist(d[0], d[1], true);
                double[][] dist = new double[2][2];
                for(int k = 0; k < 2; k++) {
                    for(int l = 0; l < 2; l++) {
                        dist[k][l] = distance(tasks.get(p[k]).pid, tasks.get(d[l]).did);
                    }
                }
                double minP1 = Math.min(dist[0][0], dist[0][1]);
                double minP2 = Math.min(dist[1][0], dist[1][1]);
                double wp1 = distance(curLid, tasks.get(p[0]).pid);
                double wp2 = distance(curLid, tasks.get(p[1]).pid);
                if (wp1 + minP2 > wp2 + minP1) {
                    if (wp2 + minP1 - pEdgeLen - dEdgeLen < curMin) {
                        curMin = wp2 + minP1 - pEdgeLen - dEdgeLen;
                        indexes[0] = i + 1;
                        indexes[1] = i;
                        if (dist[0][0] < dist[0][1]) {
                            indexes[2] = j;
                            indexes[3] = j + 1;
                        } else {
                            indexes[2] = j + 1;
                            indexes[3] = j;
                        }
                    }
                } else if (wp1 + minP2 - pEdgeLen - dEdgeLen < curMin) {
                    curMin = wp1 + minP2 - pEdgeLen - dEdgeLen;
                    indexes[0] = i;
                    indexes[1] = i + 1;
                    if (dist[1][0] < dist[1][1]) {
                        indexes[2] = j;
                        indexes[3] = j + 1;
                    } else {
                        indexes[2] = j + 1;
                        indexes[3] = j;
                    }
                }
            }
        }
        List<Integer> bestPath = new LinkedList<>();
        pidTaskCycle.remove(pidTaskCycle.size() - 1);
        didTaskCycle.remove(didTaskCycle.size() - 1);
        n = pidTaskCycle.size();
        bestPath.add(curLid);
        boolean isLeft = indexes[0] < indexes[1];
        indexes[0] %= n;
        append(bestPath, indexes[0], pidTaskCycle, isLeft, pidPickup);
        isLeft = indexes[2] < indexes[3];
        indexes[2] %= n;
        append(bestPath, indexes[2], didTaskCycle, isLeft, didPickup);
        return bestPath;
    }

    /**
     *
     * @param cityList 0:vertex id(consecutive,[0:V)), 1: point id(not consecutive)
     * @return
     */
    List<Integer> solveSingleTsp(List<int[]> cityList) {
        if (cityList.size() == 1) {
            List<Integer> res = new ArrayList<>();
            res.add(0);
            res.add(0);
            return res;
        }

        double[][] distanceMatrix;

        int V = cityList.size();
        // Number of vertices in graph
        int E = (V * V) - V;
        // Number of edges in graph

        distanceMatrix = new double[V][V];

        //creating distance matrix(weighted-adjacency)
        for (int i = 0; i < V; i++) {
            for (int k = 0; k < V; k++) {
                // Create and edge if the vertices are different
                if (i != k) {
                    distanceMatrix[i][k] = distance(cityList.get(i)[1], cityList.get(k)[1]);
                }
            }
        }

        //creating graph for given inputs
        Graph g = new Graph(V, E);

        //getting Minumum spanning tree with prim algorithm
        Edge[] primsResult = g.primMST(distanceMatrix);

        //finding odd degree vertices and creating match between them to create euler cycle
        Edge[] mst = g.findAndAddPerfectMatches(primsResult, cityList);


        //creating new graph for our cyclic mst
        Graph g1 = new Graph(V);

        for (int i = 1; i < mst.length; i++) {
            g1.addEdge(mst[i].src, mst[i].dest);
        }

        //creating euler cycle from cyclic mst
        g1.createEulerCircuit();
        if(g1.eulerianCircuit.isEmpty()) {
            System.err.println(cityList.size());
        }

        //deleting repeated vertex for final form
        return g1.clearRepeatedCities(g1.eulerianCircuit);
    }

    private void append(List<Integer> path, int start, List<Integer> cycle, boolean isLeft, boolean isPickup) {
        int n = cycle.size();
        int step = isLeft ? -1 : 1;
        for (int i = 0; i < n; i++) {
            if(isPickup) {
                path.add(cycle.get(mod(start + i * step, n)) << 1 | 1);
            } else {
                path.add(cycle.get(mod(start + i * step, n)) << 1);
            }
        }
    }

    /**
     * compute the distance between two pids or two dids
     *
     * @param tid1       task id 1
     * @param tid2       task id 2
     * @param isDelivery true: dist(did1,did2) false:dist(pid1,pid2)
     * @return distance
     */
    double taskDist(int tid1, int tid2, boolean isDelivery) {
        Task task1 = tasks.get(tid1);
        Task task2 = tasks.get(tid2);
        if (!isDelivery) {
            return distance(task1.pid, task2.pid);
        } else {
            return distance(task1.did, task2.did);
        }
    }

    /**
     * return x % m (>=0)
     *
     * @param x x
     * @param m mod
     * @return x%m (>=0)
     */
    static int mod(int x, int m) {
        return ((x % m) + m) % m;
    }

    double distance(int fromId, int toId) {
        return DistanceUtils.getDistance(points.get(fromId), points.get(toId));
    }
}
