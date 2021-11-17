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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElemeEvaluator {

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
    static String taskDirStr;
    static String workerDirStr;
    static String algorithmName;
    static long batchPeriod;
    static int nW;

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 5) {
            System.err.println("Required 5 parameters, get " + args.length + ".");
            return;
        }
        Global.isEuler = false;
        taskDirStr = args[0];
        workerDirStr = args[1];
        algorithmName = args[2];
        batchPeriod = stol(args[3]);
        nW = stoi(args[4]);

        String outFile = "./eleme/" + algorithmName + "_" + batchPeriod + "_nW" + nW + ".txt";
        PrintStream out = new PrintStream(outFile);
        System.setOut(out);

        File taskDir = new File(taskDirStr);
        File workerDir = new File(workerDirStr);
        File[] taskFiles = taskDir.listFiles();
        File[] workerFiles = workerDir.listFiles();
        assert taskFiles != null;
        assert workerFiles != null;
        assert taskFiles.length == workerFiles.length;
        Map<String, String> taskMapping = new HashMap<>();
        Map<String, String> workerMapping = new HashMap<>();
        Pattern pattern = Pattern.compile(".*_(.*).csv");
        for (int i = 0; i < taskFiles.length; i++) {
            File taskFile = taskFiles[i], workerFile = workerFiles[i];
            Matcher matcher = pattern.matcher(taskFile.getName());
            if (matcher.find()) {
                taskMapping.put(matcher.group(1), taskFile.getAbsolutePath());
            }
            matcher = pattern.matcher(workerFile.getName());
            if (matcher.find()) {
                workerMapping.put(matcher.group(1), workerFile.getAbsolutePath());
            }
        }
        int i = 0;
        for (String key : taskMapping.keySet()) {
            String taskFile = taskMapping.get(key), workerFile = workerMapping.get(key);
            System.out.printf("==================== %dth dataset start ====================\n", i);
            System.out.println("taskFile = " + taskFile);
            System.out.println("workerFile = " + workerFile);
            Validator.TestResult testResult = batchProcess(taskFile, workerFile);
            if (!testResult.isValid) {
                System.err.printf("there exist some bugs: %s\n", testResult.message);
                return;
            }
            evaluate();
            i++;
        }
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

    private static Validator.TestResult batchProcess(String taskFile, String workerFile) {
        points = new ArrayList<>();
        tasks = new ArrayList<>();
        workers = new ArrayList<>();
        Map<Point, Integer> point2id = new HashMap<>();
        IOUtils.readElemeTasks(taskFile, tasks, points, point2id);
        IOUtils.readElemeWorkersWithNumberLimit(workerFile, workers, points, point2id, nW);
        long curT = 0L;

        int start = 0, end = 0;//[start:end) determines the tasks in a batch
        long t0, t1;
        int round = 1;
        int nT = tasks.size();
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
            totalTask += end - start;
            totalRunningTime += t1 - t0;
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

    public static long stol(String s) {
        return Long.parseLong(s);
    }

    public static int stoi(String s) {
        return Integer.parseInt(s);
    }
}
