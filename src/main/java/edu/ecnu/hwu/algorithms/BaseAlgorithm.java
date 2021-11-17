package edu.ecnu.hwu.algorithms;


import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;
import edu.ecnu.hwu.utils.DistanceUtils;

import java.util.List;

public abstract class BaseAlgorithm {
    protected List<Task> tasks;
    protected List<Worker> workers;
    protected List<Point> points;

    protected BaseAlgorithm(List<Task> tasks, List<Worker> workers, List<Point> points) {
        this.tasks = tasks;
        this.workers = workers;
        this.points = points;
    }

    public abstract void run(int start, int end, long curT);

    /**
     * update worker's position after each batch
     *
     * @param curT
     */
    public void updateWorkerPos(long curT) {
        for (Worker w : workers) {
            for (int i = w.curPos + 1; i < w.schedule.size(); i++) {
                if (w.arrivalTime.get(i) <= curT) w.curPos++;
                else break;
            }
        }
    }

    protected void appendSchedule(Worker w, List<Integer> schedule, long curT) {
        for(int i = 1; i < schedule.size(); i++) {
            w.schedule.add(schedule.get(i));
        }
        int carry = w.picked.get(w.curPos);
        for (int i = w.curPos + 1; i < w.schedule.size(); i++) {
            if (isPickup(w.schedule.get(i))) carry += getT(w.schedule.get(i)).weight;
            else carry -= getT(w.schedule.get(i)).weight;
            w.picked.add(carry);
        }
        for (int i = w.curPos; i + 1 < w.schedule.size(); i++) {//recompute arrivalTime according to the schedule
            long lastArr = w.arrivalTime.get(w.arrivalTime.size() - 1);
            int fromId = (i == 0 ? w.schedule.get(0) : pos(w.schedule.get(i)));//bug fix mark
            int toId = pos(w.schedule.get(i + 1));
            long arrivalTime;
            if (isPickup(w.schedule.get(i + 1))) {
                arrivalTime = Math.max(curT, lastArr) + time(fromId, toId, w.speed);
            } else {
                arrivalTime = lastArr + time(fromId, toId, w.speed);
            }
            w.arrivalTime.add(arrivalTime);
        }
    }

    /**
     * get the nearest worker according to the last point in worker's schedule
     * @param t
     * @return
     */
    public int getNearestWorker(Task t, long curT) {
        int wid = -1;
        long earliestArrivalTime = Long.MAX_VALUE;
        for (Worker w : workers) {
            if (w.picked.get(w.picked.size() - 1) + t.weight > w.cap) continue;
            int lastIndex = w.schedule.size() - 1;
            int lastPid;
            if (w.schedule.size() == 1) {
                lastPid = w.schedule.get(lastIndex);
            } else {
                lastPid = pos(w.schedule.get(lastIndex));
            }
            long timeConsuming = time(lastPid, t.pid, w.speed) + time(t.distance, w.speed);
            long arrivalTime = Math.max(curT, w.arrivalTime.get(lastIndex)) + timeConsuming;
            if (arrivalTime < earliestArrivalTime) {
                earliestArrivalTime = arrivalTime;
                wid = w.wid;
            }
        }
        return wid;
    }

    protected void appendTask(Task t, Worker w, long curT) {
        int lastIndex = w.schedule.size() - 1;
        int lastPid;
        if (w.schedule.size() == 1) {
            lastPid = w.schedule.get(lastIndex);
        } else {
            lastPid = pos(w.schedule.get(lastIndex));
        }
        long timeToPick = Math.max(curT, w.arrivalTime.get(lastIndex)) + time(lastPid, t.pid, w.speed);
        long timeToDeliver = timeToPick + time(t.distance, w.speed);
        w.picked.add(w.picked.get(lastIndex) + t.weight);
        w.picked.add(w.picked.get(lastIndex));
        w.schedule.add(t.tid << 1 | 1);
        w.schedule.add(t.tid << 1);
        w.arrivalTime.add(timeToPick);
        w.arrivalTime.add(timeToDeliver);
    }

    protected double distance(int fromId, int toId) {
        return DistanceUtils.getDistance(points.get(fromId), points.get(toId));
    }

    protected double distance(Point from, Point to) {
        return DistanceUtils.getDistance(from, to);
    }

    protected long time(int fromId, int toId, double speed) {
        return DistanceUtils.timeConsuming(points.get(fromId), points.get(toId), speed);
    }

    protected long time(double distance, double speed) {
        return DistanceUtils.timeConsuming(distance, speed);
    }

    protected int getLid(Worker w) {
        if(w.curPos == 0) return w.schedule.get(0);
        else return pos(w.schedule.get(w.curPos));
    }

    /**
     * determine whether it is pickup point or delivery point
     * odd: pickup point
     * even: delivery point
     *
     * @param num tid in schedule(odd for pickup and even for delivery)
     * @return the corresponding Pid or Did
     */
    protected int pos(int num) {
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

    protected static boolean isPickup(int num) {
        return (num & 1) == 1;
    }
}
