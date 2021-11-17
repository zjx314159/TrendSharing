package edu.ecnu.hwu;

import edu.ecnu.hwu.algorithms.BaseAlgorithm;
import edu.ecnu.hwu.algorithms.FESI;
import edu.ecnu.hwu.algorithms.GDP;
import edu.ecnu.hwu.algorithms.TBTARP;
import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;
import edu.ecnu.hwu.utils.DistanceUtils;
import edu.ecnu.hwu.utils.Global;
import edu.ecnu.hwu.utils.IOUtils;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NYCEvaluator {

    static List<Point> points;
    static List<Task> tasks;
    static List<Worker> workers;

    static int totalTask = 0;

    // metrics
    static long totalTardiness = 0;
    static long totalTravelTime = 0;
    static long totalLatency = 0;
    static long makeSpan = 0;
    static long totalRunningTime = 0;
    static long maxTardinessT = 0;
    static long maxTardinessW = 0;
    static long minTravelTime = Long.MAX_VALUE;
    static long maxTravelTime = 0;
    static int overtimeTask = 0;

    //parameters
    static int nT;
    static int nW;
    static double speed;
    static String algorithmName;
    static long batchPeriod;
    static double edtRatio;

    //input
    static String pointFile;
    static String taskFile;
    static String workerFile;

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 9) {
            System.err.println("Required 9 parameters, get " + args.length + ".");
            return;
        }
        Global.isEuler = true;
        nT = Integer.parseInt(args[0]);
        nW = Integer.parseInt(args[1]);
        pointFile = args[2];
        taskFile = args[3];
        workerFile = args[4];
        speed = stod(args[5]);
        algorithmName = args[6];
        batchPeriod = stol(args[7]);
        edtRatio = stod(args[8]);

        String outFile = "./nyc/" + algorithmName + "_" + batchPeriod + "_edt" + edtRatio + "_nW" + nW + "_nT" + nT + "_speed" + speed + ".txt";
        PrintStream out = new PrintStream(outFile);
        System.setOut(out);

        Validator.TestResult testResult = batchProcess(pointFile, taskFile, workerFile);
        if (!testResult.isValid) {
            System.err.printf("there exist some bugs: %s\n", testResult.message);
            return;
        }
        evaluate();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Finished at " + sdf.format(new Date(Long.parseLong(String.valueOf(System.currentTimeMillis())))));
        System.out.println("totalTask = " + totalTask);
        System.out.println("overtimeTask = " + overtimeTask);
        System.out.println("totalTardiness = " + totalTardiness);
        System.out.println("totalTravelTime = " + totalTravelTime);
        System.out.println("totalLatency = " + totalLatency);
        System.out.println("makeSpan = " + makeSpan);
        System.out.println("maxTardinessT = " + maxTardinessT);
        System.out.println("maxTardinessW = " + maxTardinessW);
        System.out.println("minTravelTime = " + minTravelTime);
        System.out.println("maxTravelTime = " + maxTravelTime);
        System.out.println("totalRunningTime = " + totalRunningTime);
    }

    private static Validator.TestResult batchProcess(String pointFile, String taskFile, String workerFile) {
        points = IOUtils.readNYCPoints(pointFile);
        tasks = IOUtils.readNYCTasks(taskFile, points, nT, speed, edtRatio);
        workers = IOUtils.readNYCWorkers(workerFile, nW, speed);
        nT = tasks.size();
        nW = workers.size();

        long curT = 0L;

        int start = 0, end = 0;//[start:end) determines the tasks in a batch
        long t0, t1;
        int round = 1;
        BaseAlgorithm algorithm = new TBTARP(tasks, workers, points);
        switch (algorithmName) {
            case "fesi":
                algorithm = new FESI(tasks, workers, points);
                break;
            case "gdp":
                algorithm = new GDP(tasks, workers, points);
                break;
            case "tbtarp":
                algorithm = new TBTARP(tasks, workers, points);
                break;
            default:
        }
        while (end < nT) {
            curT += batchPeriod;
            while (end < nT && tasks.get(end).releaseTime <= curT) end++;
            if (end == start) continue;
//            System.out.printf("==================== batch %d start(batchPeriod: %d)[%d:%d) ====================\n",
//                    round, batchPeriod, start, end);
            t0 = System.currentTimeMillis();

            algorithm.run(start, end, curT);

            t1 = System.currentTimeMillis();
//            System.out.printf("==================== batch %d end  (batchPeriod: %d)[%d:%d) ====================\n",
//                    round, batchPeriod, start, end);
            round++;
            totalRunningTime += t1 - t0;
            totalTask += end - start;
            start = end;
            algorithm.updateWorkerPos(curT);
        }
        Validator validator = new Validator(points, tasks, workers);
        return validator.testCorrectness();
//        return new Validator.TestResult("", true);
    }

    private static void evaluate() {
        for (Worker w : workers) {
            int fromId = w.schedule.get(0), toId;
            long travelTime = 0, tardiness = 0;
            for (int i = 1; i < w.schedule.size(); i++) {
                int tid = w.schedule.get(i) >> 1;
                Task t = tasks.get(tid);
                toId = (w.schedule.get(i) & 1) == 1 ? t.pid : t.did;
                if (isDelivery(w.schedule.get(i))) {
                    if (w.arrivalTime.get(i) > tasks.get(tid).expectedDeliveryTime) {
                        overtimeTask++;
                        long diff = w.arrivalTime.get(i) - t.expectedDeliveryTime;
                        tardiness += diff;
                        maxTardinessT = Math.max(maxTardinessT, diff);
                    }
                    totalLatency += w.arrivalTime.get(i) - t.releaseTime;
                }
                travelTime += DistanceUtils.timeConsuming(points.get(fromId), points.get(toId), w.speed);
                fromId = toId;
            }
            maxTardinessW = Math.max(maxTardinessW, tardiness);
            totalTardiness += tardiness;
            totalTravelTime += travelTime;
            makeSpan = Math.max(makeSpan, travelTime);
            maxTravelTime = Math.max(maxTravelTime, travelTime);
            minTravelTime = Math.min(minTravelTime, travelTime);
        }
    }

    public static boolean isPickup(int num) {
        return (num & 1) == 1;
    }

    public static boolean isDelivery(int num) {
        return (num & 1) == 0;
    }

    public static double stod(String str) {
        return Double.parseDouble(str);
    }

    public static long stol(String s) {
        return Long.parseLong(s);
    }
}
