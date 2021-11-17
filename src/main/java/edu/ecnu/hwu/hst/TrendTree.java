package edu.ecnu.hwu.hst;

import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;
import edu.ecnu.hwu.utils.CommonUtils;

import java.util.*;

public class TrendTree extends HST {

    public List<TrendNode>[] trendTree;
    public List<TrendNode> TrendNodes;
    public Map<Integer, Integer>[] task2node;
    public int E;//number of trend nodes

    public TrendTree(List<Task> tasks, List<Worker> workers, List<Point> points) {
        super(tasks, workers, points);
    }

    @Override
    public void build(int start, int end) {
        super.build(start, end);
        buildTrendTree(start, end);
    }

    @Override
    public void addTask(int start, int end, Set<Integer> pids) {
        for (int tid = start; tid < end; tid++) {
            Task t = tasks.get(tid);
            int pid = t.pid, did = t.did;
            if (!pids.contains(pid)) {
                pids.add(pid);
                pi.add(pid);
                V++;
            }
            if (!pids.contains(did)) {
                pids.add(did);
                pi.add(did);
                V++;
            }
        }
    }

    @Override
    public void addWorker(Set<Integer> pids) {}

    public void buildTrendTree(int start, int end) {
        G[] trends = new G[H + 1];//trends[i] represent the trend in level i
        trendTree = new List[H + 1];
        task2node = new Map[H + 1];
        TrendNodes = new ArrayList<>();
        for (int i = 0; i <= H; i++) {
            trends[i] = new G();
            trendTree[i] = new ArrayList<>();
            task2node[i] = new HashMap<>();
        }
        Set<Integer> tids = new HashSet<>();
        for (int tid = start; tid < end; tid++) {
            Task t = tasks.get(tid);
            for (int i = 0; i <= H; i++) {
                int pNid = far[t.pid][i];
                int dNid = far[t.did][i];
                trends[i].addE(pNid, dNid, tid);
            }
            tids.add(tid);
            task2node[H].put(tid, 0);
        }
        TrendNode node = new TrendNode(tids, 0, H, 0, 0);
        trendTree[H].add(node);
        TrendNodes.add(node);

        int eid = 1;
        for (int i = H - 1; i >= 0; i--) {
            Map<Integer, Map<Integer, Set<Integer>>> trend = trends[i].out;
            for (int pNid : trend.keySet()) {
                for (Map.Entry<Integer, Set<Integer>> entry : trend.get(pNid).entrySet()) {
                    int dNid = entry.getKey();
                    tids = entry.getValue();
                    TrendNode e = new TrendNode(tids, eid, i, pNid, dNid);
                    for(int tid : e.tids) {
                        task2node[i].put(tid, eid);
                    }
                    int tid = e.tids.iterator().next();
                    for (TrendNode par : trendTree[i + 1]) {
                        if (par.tids.contains(tid)) {
                            par.addChild(e);
                            e.parent = par;
                            break;
                        }
                    }
                    trendTree[i].add(e);
                    TrendNodes.add(e);
                    eid++;
                }
            }
        }
        E = eid;
    }

    public TrendNode findLargestTrend(int tid) {
        int i = 0;
        for(; i <= H; i++) {
            TrendNode node = TrendNodes.get(task2node[i].get(tid));
            if(!node.tids.contains(tid)) return null;
            double threshold = CommonUtils.calcThreshold(tasks, points, node.tids);
            if(threshold < 1) {
                break;
            }
        }
        return TrendNodes.get(task2node[--i].get(tid));
    }

    public static class G {
        public Map<Integer, Map<Integer, Set<Integer>>> out;

        public G() {
            out = new HashMap<>();
        }

        public void addE(int src, int dest, int tid) {
            Set<Integer> tids = out.getOrDefault(src, new HashMap<>()).getOrDefault(dest, new HashSet<>());
            tids.add(tid);
            out.computeIfAbsent(src, x -> new HashMap<>()).put(dest, tids);
        }
    }
}
