package edu.ecnu.hwu.algorithms;

import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;
import edu.ecnu.hwu.pojos.Pair;

import java.util.*;

public class GAS extends BaseInsertionAlgorithm {
    private long picktime = 1800;
    private int rand_car[];
    Set<Integer> working_Set = new HashSet<>();
    
    public GAS(List<Task> tasks, List<Worker> workers, List<Point> points) {
        super(tasks, workers, points);
        rand_car = new int[workers.size()];
        for(int i = 0; i < workers.size(); i++){
            rand_car[i] = i;
        }
    }

    @Override
    public void run(int start, int end, long curT){
        for(int i = start; i < end; i++){
            working_Set.add(i);
        }
        random_shuffle(rand_car);
        for(int w: rand_car){
            Worker worker = workers.get(w);
            int curPos = worker.curPos;
            if(curPos != worker.schedule.size() - 1) continue;
            Set<Integer> cands = new HashSet<>();
            for(int i: working_Set){
                Task t = tasks.get(i);
                
                int lastPid=curPos==0?worker.schedule.get(0):pos(worker.schedule.get(curPos));
                long timeConsuming = time(lastPid, t.pid, worker.speed) + time(t.distance, worker.speed);
                if(curT + timeConsuming <= t.expectedDeliveryTime){
                    cands.add(i);
                }
            }
            if(cands.isEmpty()) continue;
            Pair<List<Integer>,Worker> schedule = get_best_u(cands, worker, curT);
            workers.set(w, schedule.getValue());//System.out.println(schedule.getValue().schedule);
            //System.out.println(schedule.getKey());//for(int i:working_Set)System.out.print(i);
            working_Set.removeAll(schedule.getKey());//System.out.println("after");for(int i:working_Set)System.out.print(i);
        }//System.out.println("after");for(int i:working_Set)System.out.print(i);
        for(int i:working_Set){//System.out.println(i);
            Task t = tasks.get(i);
                int wid = getNearestFreeWorker(t,curT,rand_car);
                if(wid!=-1)appendTask(t, workers.get(wid), curT);//System.out.println(workers.get(wid).schedule+":"+t);
                else {
                    wid=getNearestWorker(t, curT);
                    appendTask(t, workers.get(wid), curT);
                }
        }
        working_Set.clear();
    }

    private void random_shuffle(int[] arr){
        for(int i = 0; i < arr.length; i++){
            int j = (int)(Math.random() * (arr.length - i) + i);
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }

    private Pair<List<Integer>,Worker> get_best_u(Set<Integer> cands, Worker worker, long curT){
        Pair<List<Integer>,Worker> best_schedule=null;
        Map<Integer,List<Pair<List<Integer>,Worker>>> schedules = construct_additive_tree(worker, cands, curT);
        double best = 0;
        for(int i=1;i<=worker.cap;i++){
            for(int j=0;j<schedules.get(i).size();j++){
                List<Integer> s = schedules.get(i).get(j).getKey();
                double score = 0;
                for(int k:s){
                    score += tasks.get(k).distance;
                }
                if(score > best){
                    best = score;
                    best_schedule = schedules.get(i).get(j);
                }
            }
        }
        return best_schedule;
    }

    private Map<Integer,List<Pair<List<Integer>,Worker>>> construct_additive_tree(Worker worker, Set<Integer> cands, long curT){
        Map<Integer,List<Pair<List<Integer>,Worker>>> schedules = new HashMap<>();
        Set<List<Integer>> build=new HashSet<>();
        schedules.put(1, new ArrayList<>());
        for(int i:cands){//System.out.println("第一行");
            Worker new_worker = try_insert(worker, i, curT);
            if(new_worker != null){
                List<Integer> new_schedule = new ArrayList<>();
                new_schedule.add(i);
                schedules.get(1).add( new Pair<>(new_schedule, new_worker));
                build.add(new_schedule);
            }
        }
        for(int i=2;i<=worker.cap;i++){//System.out.println("第"+i);
            schedules.put(i, new ArrayList<>());
            for(int j=0;j<schedules.get(i-1).size();j++){
                for(int k=j+1;k<schedules.get(i-1).size();k++){
                    Pair<List<Integer>,List<Integer>> merged = merge_and_diff(schedules.get(i-1).get(j).getKey(), schedules.get(i-1).get(k).getKey());
                    if(merged.getValue().size()!=1||build.contains(merged.getKey())) continue;
                    //System.out.println("merge"+merged.getKey()+" "+merged.getValue());
                    List<Integer> new_schedule = merged.getKey();
                    boolean flag = true;
                    for(int l=0;l<new_schedule.size();l++){
                        List<Integer> temp = new ArrayList<>(new_schedule);
                        temp.remove(l);
                        if(!build.contains(temp)) {
                            flag = false;
                            break;
                        }
                    }
                    if(flag){
                        Worker new_worker = try_insert(schedules.get(i-1).get(j).getValue(), merged.getValue().get(0), curT);
                        if(new_worker != null){//System.out.println("i::"+new_worker.schedule);
                            schedules.get(i).add( new Pair<>(new_schedule, new_worker));
                            build.add(new_schedule);
                        }
                    }
                }
            }
        }
        return schedules;
    }

    private Pair<List<Integer>,List<Integer>> merge_and_diff(List<Integer> a, List<Integer> b){
        HashSet<Integer> setA = new HashSet<>(a);
        HashSet<Integer> setB = new HashSet<>(b);

        // Calculate union
        HashSet<Integer> union = new HashSet<>(setA);
        union.addAll(setB);

        // Calculate difference
        HashSet<Integer> difference = new HashSet<>(setB);
        difference.removeAll(setA);

        return new Pair<>(new ArrayList<>(union), new ArrayList<>(difference));
    }
}
