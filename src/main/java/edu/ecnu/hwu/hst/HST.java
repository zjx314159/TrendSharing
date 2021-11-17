package edu.ecnu.hwu.hst;

import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;
import edu.ecnu.hwu.utils.DistanceUtils;

import java.util.*;

public abstract class HST {
    public List<Task> tasks;
    public List<Worker> workers;
    public List<Point> points;

    public int H;//height
    public int V;//number of points
    public int N;//number of nodes

    public double beta;
    public double delta;//diameter of points
    public List<Integer> pi;//permutation of points
    public Map<Integer, Integer> reversePi;

    public int[][] far;//far[i][j] denote the node id of ith point in pi in jth level
    public double[] exp;//exp[i]=2^i

    public HST(List<Task> tasks, List<Worker> workers, List<Point> points) {
        this.tasks = tasks;
        this.workers = workers;
        this.points = points;
    }

    public void build(int start, int end) {
        initFields(start, end);
        constructHST();
    }

    public void initFields(int start, int end) {
        Set<Integer> pids = new HashSet<>();
        V = 0;
        pi = new ArrayList<>();
        reversePi = new HashMap<>();

        addTask(start, end, pids);
        addWorker(pids);

        initParams();
        H = (int) Math.ceil(Math.log(delta) / Math.log(2.0));
        exp = new double[H + 1];
        exp[0] = 1.0;
        for (int i = 1; i <= H; i++) {
            exp[i] = exp[i - 1] * 2.0;
        }
    }

    public void initParams() {
        randomization();
        calcDelta();
    }

    public void randomization() {
        Collections.shuffle(pi);
        for (int i = 0; i < V; i++) {
            reversePi.put(pi.get(i), i);
        }
        Random rand = new Random(System.currentTimeMillis());
        beta = rand.nextDouble() * 0.5 + 0.5;//beta in [0.5,1)
        beta = 0.5;
    }

    public void calcDelta() {
        delta = 0.0;
        for (int i = 0; i < V; i++) {
            for (int j = i + 1; j < V; j++) {
                delta = Math.max(delta, distance(pi.get(i), pi.get(j)));
            }
        }
    }

    public abstract void addTask(int start, int end, Set<Integer> pids);

    public abstract void addWorker(Set<Integer> pids);

    public void constructHST() {
        far = new int[points.size()][H + 1];
        for (int i = 0; i < V; i++) {
            for (int j = 0; j <= H; ++j) {
                if (j == H) {//root
                    far[pi.get(i)][j] = 0;
                } else {
                    far[pi.get(i)][j] = reversePi.get(pi.get(i));
                }
            }
        }
        List<List<Integer>> preC = new ArrayList<>(), curC = new ArrayList<>();
        preC.add(new ArrayList<>(pi));
        // construct the HST by brute-force
        int nid = 1; //id of nodes in HST, root is 0
        //update far
        for (int i = H - 1; i >= 0; --i) {
            //STOC03 paper use this formula: (2^(i-1))*beta, beta in [1,2)
            //we transform to this formula: (2^i)*beta, beta in [0.5,1)
            double radius = beta * exp[i];
            for (List<Integer> cluster : preC) {
                for (int j = 0; j < V; j++) {//select the center point of each node
                    List<Integer> newCluster = new ArrayList<>();
                    for (int uid = cluster.size() - 1; uid >= 0; --uid) {
                        int u = cluster.get(uid);
                        double dist = distance(u, pi.get(j));
                        if (dist < radius) {//pi[j] is the center point
                            newCluster.add(u);
                            Collections.swap(cluster, uid, cluster.size() - 1);//swap the element to remove with the last element
                            cluster.remove(cluster.size() - 1);//delete last element
                            far[u][i] = nid;//the node id of point u in ith level = nid
                        }
                    }
                    if (!newCluster.isEmpty()) {
                        curC.add(new ArrayList<>(newCluster));
                        nid++;
                    }
                }
            }
            preC = new ArrayList<>(curC);
            curC.clear();
        }
        N = nid;
    }

    public double distance(int fromId, int toId) {
        return DistanceUtils.getDistance(points.get(fromId), points.get(toId));
    }

    /**
     * determine whether it's pickup point or delivery point
     * odd: pickup point
     * even: delivery point
     *
     * @param num tid in schedule(odd for pickup and even for delivery)
     * @return the corresponding Pid or Did
     */
    public int pos(int num) {
        Task t = getT(num);
        return (num & 1) == 1 ? t.pid : t.did;
    }

    /**
     * get task according to the value in S
     *
     * @param val in S
     * @return get true tid (val / 2)
     */
    protected Task getT(int val) {
        return tasks.get(val >> 1);
    }

    protected int getLid(Worker w) {
        if(w.curPos == 0) return w.schedule.get(0);
        else return pos(w.schedule.get(w.curPos));
    }
}


