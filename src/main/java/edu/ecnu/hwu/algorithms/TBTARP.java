package edu.ecnu.hwu.algorithms;

import edu.ecnu.hwu.algorithms.rp.RoutePlanning;
import edu.ecnu.hwu.hst.TrendNode;
import edu.ecnu.hwu.hst.TrendTree;
import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;

import java.util.*;

public class TBTARP extends BaseAlgorithm {
    public TrendTree trendTree;

    public TBTARP(List<Task> tasks, List<Worker> workers, List<Point> points) {
        super(tasks, workers, points);
        trendTree = new TrendTree(tasks, workers, points);
    }

    @Override
    public void run(int start, int end, long curT) {
        trendTree.build(start, end);
        Queue<Task> taskQueue = new PriorityQueue<>((o1, o2) -> Double.compare(o2.distance, o1.distance));
        for (int tid = start; tid < end; tid++) taskQueue.offer(tasks.get(tid));
        while (!taskQueue.isEmpty()) {
            Task t = taskQueue.poll();
            int wid = getNearestWorker(t, curT);
            Worker w = workers.get(wid);
            TrendNode trendNode = trendTree.findLargestTrend(t.tid);
            if (trendNode == null) {//has been assigned
                continue;
            }
            Set<Integer> tids = new HashSet<>();
            tids.add(t.tid);
            for (int tid : trendNode.tids) {
                if (tid == t.tid) continue;
                if (tids.size() + w.picked.get(w.curPos) == w.cap) break;
                tids.add(tid);
            }
            RoutePlanning rp = new RoutePlanning(tasks, workers, points);
            List<Integer> schedule = rp.pd(tids, w);
            appendSchedule(w, schedule, curT);
            w.curPos = w.schedule.size() - 1;
            deleteChildTids(trendNode, tids);
            deleteParentTids(trendNode, tids);
        }
    }

    @Override
    public void updateWorkerPos(long curT) {
        for(Worker w : workers) {
            w.curPos = w.schedule.size() - 1;
        }
    }

    private void deleteParentTids(TrendNode cur, Set<Integer> assignedTasks) {
        if (cur == null) return;
        Set<Integer> curTids = new HashSet<>(cur.tids);
        for (int tid : curTids) {
            if (assignedTasks.contains(tid)) cur.tids.remove(tid);
        }
        deleteParentTids(cur.parent, assignedTasks);
    }

    private void deleteChildTids(TrendNode cur, Set<Integer> assignedTasks) {
        for (TrendNode child : cur.children) {
            Set<Integer> childTids = new HashSet<>(child.tids);
            for (int tid : childTids) {
                if (assignedTasks.contains(tid)) child.tids.remove(tid);
            }
            deleteChildTids(child, assignedTasks);
        }
    }
}
