package edu.ecnu.hwu.algorithms;

import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;

import java.util.List;

public class GDP extends BaseInsertionAlgorithm {

    public GDP(List<Task> tasks, List<Worker> workers, List<Point> points) {
        super(tasks, workers, points);
    }

    @Override
    public void run(int start, int end, long curT) {
        for (int i = start; i < end; i++) {
            Task t = tasks.get(i);
            int wid = -1;
            DetInfo info = new DetInfo();
            for (int j = 0; j < workers.size(); j++) {
                DetInfo tmp = insertMinDet(workers.get(j), t.tid);
                if (tmp != null && info.detour > tmp.detour) {
                    info = tmp;
                    wid = j;
                }
            }
            insert(workers.get(wid), t.tid, info.posI, info.posJ, curT);
        }
    }

    @Override
    public void updateWorkerPos(long curT) {
        for(Worker w : workers) {
            w.curPos = w.schedule.size() - 1;
        }
    }
}
