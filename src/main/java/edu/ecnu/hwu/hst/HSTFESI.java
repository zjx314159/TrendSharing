package edu.ecnu.hwu.hst;

import edu.ecnu.hwu.pojos.Pair;
import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class HSTFESI extends HST {

    public int[] labels;
    public boolean[] visited;
    public Node[] nodes;
    public Path[] paths;
    public List<List<Integer>> tree;//tree[i] contains all points in node i

    public HSTFESI(List<Task> tasks, List<Worker> workers, List<Point> points) {
        super(tasks, workers, points);
    }

    @Override
    public void build(int start, int end) {
        super.build(start, end);
        //used for FESI
        paths = new Path[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) paths[i] = new Path(i);
        labels = new int[N];
        visited = new boolean[N];
        initNodes(start, end);
        initTree();
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
    public void addWorker(Set<Integer> pids) {
        for (Worker w : workers) {
            int wLid = getLid(w);
            if (!pids.contains(wLid)) {
                pids.add(wLid);
                pi.add(wLid);
                V++;
            }
        }
    }

    /**
     * initialize nodes(clusters) according to far[i][j]
     * the task range is [start,end)
     *
     * @param start start tid
     * @param end   end tid
     */
    public void initNodes(int start, int end) {
        int u, v, nid;
        nodes = new Node[N];
        for (int i = 0; i < N; ++i) {
            nodes[i] = new Node();
        }
        for (int pointIdx = 0; pointIdx < V; ++pointIdx) {
            for (int curHeight = 0; curHeight <= H; ++curHeight) {
                nid = far[pi.get(pointIdx)][curHeight];
                nodes[nid].dep = curHeight;
            }
        }

        for (int tid = start; tid < end; ++tid) {
            u = tasks.get(tid).pid;//pickup point id
            v = tasks.get(tid).did;//delivery point id
            for (int j = 0; j <= H; ++j) {
                if (far[u][j] == far[v][j]) {//pickup and delivery point are within the same node
                    nid = far[u][j];
                    ++nodes[nid].cnt;
                    ++nodes[nid].flow;
                } else {
                    nid = far[u][j];
                    ++nodes[nid].flow;
                    nid = far[v][j];
                    ++nodes[nid].flow;
                }
            }
        }
    }

    public void initTree() {
        int treeId;
        tree = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            tree.add(new ArrayList<>());
        }

        for (int j = 0; j <= H; j++) {
            for (int tid : pi) {
                treeId = far[tid][j];
                tree.get(treeId).add(tid);
            }
        }
    }

    int cnt;

    public void genLabel(Worker w) {
        int wLid = getLid(w);
        int nid = far[wLid][0];
        Arrays.fill(labels, 0);
        Arrays.fill(visited, false);
        cnt = 1;
        dfsLabel(nid, wLid, 0);
    }

    private void dfsLabel(int rt, int vid, int l) {
        if (rt == -1) return;
        visited[rt] = true;
        if (l > 0) {//label children
            List<Integer> vi = tree.get(rt);
            int _rt, _l = l - 1;
            for (int _vid : vi) {
                _rt = far[_vid][_l];
                if (!visited[_rt]) {
                    dfsLabel(_rt, _vid, _l);
                }
            }
        }
        labels[rt] = cnt++;
        if (l < H) {//label father
            int _l = l + 1, _rt = far[vid][_l];
            if (!visited[_rt]) {
                dfsLabel(_rt, vid, _l);
            }
        }
    }

    public void genVec(List<Integer> tids) {
        for (int tid : tids) {
            genVec(tid);
        }
    }

    private void genVec(int tid) {
        Task t = tasks.get(tid);
        int u = t.pid, v = t.did, rt;
        Path path = paths[tid];

        int n = getPathHeight(u, v);
        for (int i = n - 1; i >= 0; i--) {
            rt = far[u][i];
            path.vec.add(labels[rt]);
            rt = far[v][i];
            path.vec.add(labels[rt]);
        }
    }

    private int getPathHeight(int u, int v) {
        int res = 0;
        for (int i = 0; i <= H; ++i) {
            if (far[u][i] == far[v][i]) {
                return res;
            }
            ++res;
        }
        return -1;
    }

    public void sortPath(List<Integer> tids) {
        int n = tids.size();
        Path[] par = new Path[n];
        int tid;
        for (int i = 0; i < n; i++) {
            tid = tids.get(i);
            par[i] = new Path(tid);
            par[i].vec = paths[tid].vec;
        }
        Arrays.sort(par, (o1, o2) -> {
            List<Integer> v1 = o1.vec, v2 = o2.vec;
            int i = 0, j = 0;
            int sz1 = v1.size(), sz2 = v2.size();
            while (i < sz1 && j < sz2) {
                if (v1.get(i).equals(v2.get(j))) {
                    i++;
                    j++;
                } else {
                    return v1.get(i) - v2.get(i);
                }
            }
            if (i == sz1 || j == sz2) return sz1 - sz2;
            return o1.tid - o2.tid;
        });

        for (int i = 0; i < n; i++) {
            tids.set(i, par[i].tid);
            par[i].vec.clear();
        }
    }

    public void updateTree(List<Integer> tids) {
        for (int tid : tids) {
            int u = tasks.get(tid).pid, v = tasks.get(tid).did;
            Pair<Integer, Integer> pTmp = getLCA(u, v);
            int nid = pTmp.getKey();
            Node node = nodes[nid];
            node.pop(tid);

            for (int j = 0; j <= H; j++) {
                if (far[u][j] == far[v][j]) {
                    nid = far[u][j];
                    --nodes[nid].cnt;
                    --nodes[nid].flow;
                } else {
                    nid = far[u][j];
                    --nodes[nid].flow;
                    nid = far[v][j];
                    --nodes[nid].flow;
                }
            }
        }
    }

    private Pair<Integer, Integer> getLCA(int u, int v) {
        for (int i = 0; i < H; i++) {
            if (far[u][i] == far[v][i])
                return new Pair<>(far[u][i], i);
        }
        return new Pair<>(far[u][H], H);
    }

    static class Path {
        int tid;
        List<Integer> vec;

        Path(int tid) {
            this.tid = tid;
            vec = new ArrayList<>();
        }
    }

    /**
     * node in HST
     */
    static class Node {

        public int cnt;

        public int flow;

        public int dep;

        List<Integer> tids;

        Node() {
            cnt = 0;
            flow = 0;
            dep = 0;
            tids = new ArrayList<>();
        }

        void pop(int x) {
            tids.remove((Integer) x);
        }
    }
}
