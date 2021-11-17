package edu.ecnu.hwu.algorithms.rp;

import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;

import java.util.List;
import java.util.Set;

public class RoutePlanning {
    List<Task> tasks;
    List<Worker> workers;
    List<Point> points;

    public RoutePlanning(List<Task> tasks, List<Worker> workers, List<Point> points) {
        this.tasks = tasks;
        this.workers = workers;
        this.points = points;
    }

    public List<Integer> pd(Set<Integer> tids, Worker w) {
        ApproximationSolver solver = new ApproximationSolver(tasks, workers, points);
        List<Integer> pidTaskCycle = solver.getCycle(tids, true);
        List<Integer> didTaskCycle = solver.getCycle(tids, false);
        //Note: must construct the complete cycle first, then find the best route
        return solver.findBestPath(getLid(w), pidTaskCycle, didTaskCycle, true, false);
    }

    /**
     * get current location(index in points) of worker
     * @param w
     * @return
     */
    private int getLid(Worker w) {
        int lid;
        if(w.curPos == 0) {
            lid = w.schedule.get(0);
        }
        else {
            int tid2 = w.schedule.get(w.curPos);
            lid = (tid2 & 1) == 1 ? tasks.get(tid2 >> 1).pid : tasks.get(tid2 >> 1).did;
        }
        return lid;
    }
}
