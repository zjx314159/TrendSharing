package edu.ecnu.hwu.utils;

import edu.ecnu.hwu.pojos.Pair;
import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;

import java.util.List;
import java.util.Set;

public class CommonUtils {
    public static Pair<Point, Point> getCentroid(List<Task> tasks, List<Point> points, Set<Integer> tids) {
        double px = 0, py = 0, dx = 0, dy = 0;
        for (int tid : tids) {
            Task t = tasks.get(tid);
            Point p = points.get(t.pid);
            px += p.getLng();
            py += p.getLat();
            p = points.get(t.did);
            dx += p.getLng();
            dy += p.getLat();
        }
        px /= tids.size();
        py /= tids.size();
        dx /= tids.size();
        dy /= tids.size();
        return new Pair<>(new Point(px, py), new Point(dx, dy));
    }

    public static double calcThreshold(List<Task> tasks, List<Point> points, Set<Integer> tids) {
        if (tids.size() == 1) return 1;
        double minInter = Double.MAX_VALUE;
        double maxInter = Double.MIN_VALUE;
        double maxInner = Double.MIN_VALUE;
        for (int tid1 : tids) {
            Task t1 = tasks.get(tid1);
            for (int tid2 : tids) {
                Task t2 = tasks.get(tid2);
                double inter = DistanceUtils.getDistance(points.get(t1.pid), points.get(t2.did));
                minInter = Math.min(minInter, inter);
                maxInter = Math.max(maxInter, inter);
                maxInner = Math.max(maxInner, DistanceUtils.getDistance(points.get(t1.pid), points.get(t2.pid)));
                maxInner = Math.max(maxInner, DistanceUtils.getDistance(points.get(t1.did), points.get(t2.did)));
            }
        }
//        return (minInter - maxInner) / (maxInter - minInter);
        return (3 * minInter) / (2 * maxInner + maxInter);
    }
}
