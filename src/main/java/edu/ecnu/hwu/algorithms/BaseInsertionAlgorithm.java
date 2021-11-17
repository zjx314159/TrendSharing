package edu.ecnu.hwu.algorithms;

import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseInsertionAlgorithm extends BaseAlgorithm {

    protected BaseInsertionAlgorithm(List<Task> tasks, List<edu.ecnu.hwu.pojos.Worker> workers, List<Point> points) {
        super(tasks, workers, points);
    }

    /**
     * O(n) insertion which can finds the optimal position to insert a task that can minimize the increased travel distance
     *
     * @param w   worker
     * @param tid task id
     * @return the optimal position to insert a task that can minimize the increased travel distance
     */
    protected DetInfo insertMinDet(Worker w, int tid) {
        Task t = tasks.get(tid);
        if (t.weight > w.cap) return null;
        int n = w.schedule.size();//(curPos:n) are the positions can insert
        if (n - w.curPos == 1) {
            double detour = distance(w.curPos == 0 ? w.schedule.get(w.curPos) : pos(w.schedule.get(w.curPos)), t.pid) + t.distance;
            return new DetInfo(detour, w.curPos, w.curPos);
        }
        DetInfo info = new DetInfo();

        //insert into the same interval
        for (int i = w.curPos; i < n; ++i) {
            if (w.picked.get(i) + t.weight > w.cap)
                continue;
            double detour;
            if (i == n - 1) {
                detour = distance(getT(w.schedule.get(i)).did, t.pid) + t.distance;
            } else if (i == w.curPos) {
                int fromId = i == 0 ? w.schedule.get(i) : pos(w.schedule.get(i));
                int toId = pos(w.schedule.get(w.curPos + 1));
                detour = distance(fromId, t.pid) + t.distance + distance(t.did, toId) - distance(fromId, toId);
            } else {
                int fromId = pos(w.schedule.get(i));
                int toId = pos(w.schedule.get(i + 1));
                detour = distance(fromId, t.pid) + t.distance + distance(t.did, toId) - distance(fromId, toId);
            }

            if (detour < info.detour) {
                info = new DetInfo(detour, i, i);
            }
        }

        List<Double> Dio = new ArrayList<>();
        List<Integer> Plc = new ArrayList<>();
        for (int j = 0; j < w.curPos; j++) {
            Dio.add(Double.MAX_VALUE);
            Plc.add(-1);
        }
        for (int j = w.curPos; j < n; j++) {
            if (w.picked.get(j) + t.weight > w.cap) {
                Dio.add(Double.MAX_VALUE);
                Plc.add(-1);
                continue;
            }

            if (j == w.curPos) {
                int fromId = w.curPos == 0 ? w.schedule.get(j) : pos(w.schedule.get(j));
                int toId = pos(w.schedule.get(j + 1));
                double det = distance(fromId, t.pid) + distance(t.pid, toId) - distance(fromId, toId);
                Dio.add(det);
                Plc.add(j);
                continue;
            }

            double detD, detP;//detD: detour of inserting delivery point，detP: detour of inserting pickup point

            int fromId = pos(w.schedule.get(j));
            if (j == n - 1) {
                detD = distance(fromId, t.did);
                detP = distance(fromId, t.pid);
            } else {
                int toId = pos(w.schedule.get(j + 1));
                detD = distance(fromId, t.did) + distance(t.did, toId) - distance(fromId, toId);
                detP = distance(fromId, t.pid) + distance(t.pid, toId) - distance(fromId, toId);
            }

            double tmp = detD + Dio.get(Dio.size() - 1);

            if (tmp < info.detour) {
                info = new DetInfo(tmp, Plc.get(Plc.size() - 1), j);
            }

            //update the detour of inserting pickup point in advance, so that it can be used in next iteration
            if (detP <= Dio.get(Dio.size() - 1)) {
                Dio.add(detP);
                Plc.add(j);
            } else {
                Dio.add(Dio.get(Dio.size() - 1));
                Plc.add(Plc.get(Plc.size() - 1));
            }
        }
        return info;
    }

    /**
     * insert a task into a specific worker's schedule
     *
     * @param w    worker
     * @param tid  task id
     * @param posI position where pickup point will be inserted
     * @param posJ position where delivery point will be inserted
     */
    protected void insert(Worker w, int tid, int posI, int posJ, long curT) {
        w.schedule.add(posI + 1, tid << 1 | 1);
        w.schedule.add(posJ + 2, tid << 1);
        w.arrivalTime.add(posI + 1, -1L);
        w.arrivalTime.add(posJ + 2, -1L);
        w.picked.add(posI + 1, -1);
        w.picked.add(posJ + 2, -1);
        int picked = w.picked.get(posI);
        for(int i = posI + 1; i < w.schedule.size(); i++) {
            if(isPickup(w.schedule.get(i))) {
                picked += getT(w.schedule.get(i)).weight;
            } else {
                picked -= getT(w.schedule.get(i)).weight;
            }
            w.picked.set(i, picked);
            long lastArr = w.arrivalTime.get(i - 1);
            int fromId = (i == 1 ? w.schedule.get(0) : pos(w.schedule.get(i - 1)));
            int toId = pos(w.schedule.get(i));
            long arrivalTime;
            if (isPickup(w.schedule.get(i))) {
                arrivalTime = Math.max(curT, lastArr) + time(fromId, toId, w.speed);
            } else {
                arrivalTime = lastArr + time(fromId, toId, w.speed);
            }
            w.arrivalTime.set(i, arrivalTime);
        }
    }

    static class DetInfo {
        public double detour;//increased travel time
        public int posI;
        public int posJ;

        public DetInfo() {
            detour = Double.MAX_VALUE;
            posI = -1;
            posJ = -1;
        }

        public DetInfo(double detour, int posI, int posJ) {
            this.detour = detour;
            this.posI = posI;
            this.posJ = posJ;
        }
    }
}
