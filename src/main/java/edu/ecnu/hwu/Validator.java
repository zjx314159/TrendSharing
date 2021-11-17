package edu.ecnu.hwu;

import edu.ecnu.hwu.pojos.Pair;
import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.*;

public class Validator {

    List<Point> points;
    List<Task> tasks;
    List<Worker> workers;

    public Validator(List<Point> points, List<Task> tasks, List<Worker> workers) {
        this.points = points;
        this.tasks = tasks;
        this.workers = workers;
    }

    public TestResult testCorrectness() {
        TestResult result = validateSchedule();
        if (!result.isValid) {
            return result;
        }
        Pair<Set<Integer>, Map<Integer, Integer>> p = assignedTasks();
        Set<Integer> assignedTasks = p.getKey();
        Map<Integer, Integer> redundant = p.getValue();
        if(tasks.size() > assignedTasks.size()) {
            Set<Integer> unassigned = new HashSet<>();
            for(Task task : tasks) {
                if(!assignedTasks.contains(task.tid)) unassigned.add(task.tid);
            }
            return new TestResult(String.format("exist %d tasks unassigned: %s", tasks.size() - assignedTasks.size(), unassigned), false);
        } else {
            if(!redundant.isEmpty()) {
                return new TestResult(String.format("exist %d redundant tasks: %s", redundant.size(), redundant), false);
            }
        }
        return result;
    }

    /**
     * 1. the length of schedule, picked and arrivalTime should be the same
     * 2. capacity constraint cannot be violated
     * 3. a task can only be assigned once
     * 4. the timestamp worker arrives at a pickup point is later than its release time
     * 5. the pickup point appears earlier than the delivery point in the schedule
     *
     * @return
     */
    private TestResult validateSchedule() {
        for (Worker w : workers) {
            if(w.schedule.size() % 2 != 1) {
                return new TestResult(String.format("len(schedule)=%d is not valid", w.schedule.size()), false);
            }
            if (w.schedule.size() != w.picked.size()) {
                return new TestResult(String.format("len(schedule)=%d neq len(picked)=%d", w.schedule.size(), w.picked.size()), false);
            }
            if (w.schedule.size() != w.arrivalTime.size()) {
                return new TestResult(String.format("len(schedule)=%d neq len(arrivalTime)=%d", w.schedule.size(), w.arrivalTime.size()), false);
            }
            Map<Integer, Integer> pickup = new HashMap<>();
            Map<Integer, Integer> delivery = new HashMap<>();
            for (int i = 1; i < w.schedule.size(); i++) {
                if (w.picked.get(i) > w.cap) {
                    return new TestResult(String.format("capacity constraint violated: wid=%d, index=%d", w.wid, i), false);
                }
                int tid = w.schedule.get(i) >> 1;
                if (isDelivery(w.schedule.get(i))) {
                    if (delivery.containsKey(tid)) {
                        return new TestResult(String.format("duplicate delivery point: wid=%d, index=%d", w.wid, i), false);
                    }
                    delivery.put(tid, i);
                } else {
                    if (tasks.get(tid).releaseTime > w.arrivalTime.get(i)) {
                        return new TestResult(String.format("arrivalTime is earlier than release time: wid=%d, index=%d", w.wid, i), false);
                    }
                    if (pickup.containsKey(tid)) {
                        return new TestResult(String.format("duplicate pickup point: wid=%d, index=%d", w.wid, i), false);
                    }
                    pickup.put(tid, i);
                }
            }
            if (pickup.size() != delivery.size()) {
                return new TestResult(String.format("len(pickup)=%d neq len(delivery)=%d: wid=%d", pickup.size(), delivery.size(), w.wid), false);
            }
            for (int key : pickup.keySet()) {
                if (!delivery.containsKey(key)) {
                    return new TestResult(String.format("pickup but not delivery: wid=%d, tid=%d", w.wid, key), false);
                }
                if (delivery.get(key) < pickup.get(key)) {
                    return new TestResult(String.format("delivery point appears earlier than pickup point: wid=%d, tid=%d", w.wid, key), false);
                }
            }
        }
        return new TestResult("", true);
    }

    private Pair<Set<Integer>, Map<Integer, Integer>> assignedTasks() {
        Set<Integer> tids = new HashSet<>();
        Map<Integer, Integer> redundant = new HashMap<>();//key:tid,value:count
        for (Worker w : workers) {
            for(int i = 1; i < w.schedule.size(); i++) {
                if(isDelivery(w.schedule.get(i))) {
                    int tid = w.schedule.get(i) >> 1;
                    if(tids.contains(tid)) {
                        redundant.put(tid, redundant.getOrDefault(tid, 0) + 1);
                    } else {
                        tids.add(tid);
                    }
                }
            }
        }
        return new Pair<>(tids, redundant);
    }

    public static boolean isDelivery(int num) {
        return (num & 1) == 0;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestResult {
        String message;
        boolean isValid;
    }
}
