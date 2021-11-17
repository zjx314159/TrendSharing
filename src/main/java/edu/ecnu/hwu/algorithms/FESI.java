package edu.ecnu.hwu.algorithms;

import edu.ecnu.hwu.hst.HSTFESI;
import edu.ecnu.hwu.pojos.Pair;
import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;

import java.util.*;

public class FESI extends BaseInsertionAlgorithm {
    private HSTFESI hst;
    private static final double STEP = 2.0;
    private static final double RHO = 1.0;
    private double delta;
    private Set<Integer> tids;
    
    public double[] dists;

    public FESI(List<Task> tasks, List<Worker> workers, List<Point> points) {
        super(tasks, workers, points);
        this.hst = new HSTFESI(tasks, workers, points);
        dists = new double[workers.size()];
    }

    @Override
    public void run(int start, int end, long curT) {
        hst.build(start, end);

        fesiInit(start, end);

        List<Pair<Double, Integer>> vpi = new ArrayList<>();
        int nW = workers.size();
        for (int i = 0; i < nW; i++) {
            vpi.add(new Pair<>(dists[i], i));
        }
        while (!tids.isEmpty()) {
            vpi.sort(Comparator.comparingDouble(Pair::getKey));
            for (int i = 0; i < nW; i++) {
                int wid = vpi.get(i).getValue();
                List<Integer> Rw = budget(wid, curT);
                if (!Rw.isEmpty()) {
                    hst.updateTree(Rw);
                    removeAssignedTasks(Rw);
                    vpi.set(i, new Pair<>(dists[i], i));
                    if(tids.isEmpty()) break;
                }
            }
            delta *= STEP;
        }
    }

    @Override
    public void updateWorkerPos(long curT) {
        for(Worker w : workers) {
            w.curPos = w.schedule.size() - 1;
        }
    }

    private List<Integer> budget(int wid, long curT) {
        List<Integer> Rw = new ArrayList<>();
        List<Integer> sorted = new ArrayList<>();
        Worker w = workers.get(wid);
        int wLid = getLid(workers.get(wid));
        for (int tid : tids) {
            Task t = tasks.get(tid);
            if (distance(wLid, t.pid) + t.distance > delta) continue;
            sorted.add(tid);
        }
        if (sorted.isEmpty()) return Rw;

        hst.genLabel(w);
        hst.genVec(sorted);
        hst.sortPath(sorted);

        double tot = 0.0;
        for (int tid : sorted) {
            DetInfo info = insertMinDet(w, tid);
            if (info == null || info.detour == Double.MAX_VALUE) continue;
            tot += info.detour;
            if (tot > delta) break;
            insert(w, tid, info.posI, info.posJ, curT);
            Rw.add(tid);
        }
        dists[wid] += tot;
        return Rw;
    }

    private void fesiInit(int start, int end) {
        delta = RHO;
        tids = new HashSet<>();
        for (int tid = start; tid < end; tid++) {
            tids.add(tid);
        }
    }

    private void removeAssignedTasks(List<Integer> Rw) {
        for (int tid : Rw) {
            tids.remove(tid);
        }
    }
}
