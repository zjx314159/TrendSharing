package edu.ecnu.hwu.utils;


import edu.ecnu.hwu.pojos.Point;
import edu.ecnu.hwu.pojos.Task;
import edu.ecnu.hwu.pojos.Worker;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IOUtils {
    private static final String SEPARATOR = ",";

    public static void readElemeTasks(String file, List<Task> tasks, List<Point> points, Map<Point, Integer> point2id) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line = br.readLine();//skip header: [release_time,expected_delivery_time,src_lng,src_lat,dst_lng,dst_lat]
            int tid = 0;
            while ((line = br.readLine()) != null) {
                String[] attrs = line.split(SEPARATOR);
                double srcLng = stod(attrs[2]), srcLat = stod(attrs[3]);
                double dstLng = stod(attrs[4]), dstLat = stod(attrs[5]);
                Point p = new Point(srcLng, srcLat);
                Point d = new Point(dstLng, dstLat);
                double distance = DistanceUtils.getDistance(p, d);
                if (distance < 1.0) continue;//outlier
                if (!point2id.containsKey(p)) {
                    point2id.put(p, points.size());
                    points.add(p);
                }
                if (!point2id.containsKey(d)) {
                    point2id.put(d, points.size());
                    points.add(d);
                }
                tasks.add(new Task(tid++, point2id.get(p), point2id.get(d), stol(attrs[0]), stol(attrs[1]), 1, distance));
            }
            System.out.printf("number of tasks: %d\n", tasks.size());
            System.out.printf("corresponding number of points: %d\n", points.size());
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("Error initializing stream");
        }
    }

    public static void readElemeWorkers(String file, List<Worker> workers, List<Point> points, Map<Point, Integer> point2id) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line = br.readLine();//skip header: [cap,speed,lng,lat]
            int wid = 0;
            while ((line = br.readLine()) != null) {
                String[] attrs = line.split(SEPARATOR);
                int cap = stoi(attrs[0]);
                double speed = stod(attrs[1]), lng = stod(attrs[2]), lat = stod(attrs[3]);
                Point l = new Point(lng, lat);
                if (!point2id.containsKey(l)) {
                    point2id.put(l, points.size());
                    points.add(l);
                }
                List<Integer> schedule = new ArrayList<>(Collections.singletonList(point2id.get(l)));
                List<Long> arrivalTime = new ArrayList<>(Collections.singletonList(0L));
                List<Integer> picked = new ArrayList<>(Collections.singletonList(0));
                workers.add(new Worker(wid++, 0, cap, speed, schedule, arrivalTime, picked));
            }
            System.out.printf("number of workers: %d\n", workers.size());
            System.out.printf("total number of points: %d\n", points.size());
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("Error initializing stream");
        }
    }

    public static void readElemeWorkersWithNumberLimit(String file, List<Worker> workers, List<Point> points, Map<Point, Integer> point2id, int nW) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line = br.readLine();//skip header: [cap,speed,lng,lat]
            int wid = 0;
            while ((line = br.readLine()) != null) {
                if(wid == nW) break;
                String[] attrs = line.split(SEPARATOR);
                int cap = stoi(attrs[0]);
                double speed = stod(attrs[1]), lng = stod(attrs[2]), lat = stod(attrs[3]);
                Point l = new Point(lng, lat);
                if (!point2id.containsKey(l)) {
                    point2id.put(l, points.size());
                    points.add(l);
                }
                List<Integer> schedule = new ArrayList<>(Collections.singletonList(point2id.get(l)));
                List<Long> arrivalTime = new ArrayList<>(Collections.singletonList(0L));
                List<Integer> picked = new ArrayList<>(Collections.singletonList(0));
                workers.add(new Worker(wid++, 0, cap, speed, schedule, arrivalTime, picked));
            }
            System.out.printf("number of workers: %d\n", workers.size());
            System.out.printf("total number of points: %d\n", points.size());
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("Error initializing stream");
        }
    }

    public static List<Point> readNYCPoints(String pointFile) {
        List<Point> points = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(pointFile);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] coord = line.split(SEPARATOR);
                double lat = stod(coord[0]), lng = stod(coord[1]);
                points.add(new Point(lng, lat));
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("Error initializing stream");
        }
        System.out.println("points.size() = " + points.size());
        return points;
    }

    public static List<Task> readNYCTasks(String file, List<Point> points, int nT, double speed, double edtRatio) {
        List<Task> tasks = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line = br.readLine();//skip first line
            System.out.printf("number of tasks = %s\n", line);
            int tid = 0;
            while ((line = br.readLine()) != null) {
                String[] attrs = line.split(" ");
                long releaseTime = stol(attrs[0]);
                int pid = stoi(attrs[1]), did = stoi(attrs[2]), weight = stoi(attrs[3]);
                double distance = DistanceUtils.getDistance(points.get(pid), points.get(did));
                if (distance < 1.0) continue;//outlier
                tasks.add(new Task(tid++, pid, did, releaseTime, releaseTime + (long) (Math.ceil(distance / speed * edtRatio)), weight, distance));
                if (tid == nT) break;
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("Error initializing stream");
        }
        System.out.println("tasks.size() = " + tasks.size());
        return tasks;
    }

    public static List<Worker> readNYCWorkers(String file, int nW, double speed) {
        List<Worker> workers = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line = br.readLine();//skip first line
            int wid = 0;
            while ((line = br.readLine()) != null) {
                String[] attrs = line.split(" ");
                int lid = stoi(attrs[0]), cap = stoi(attrs[1]);
                List<Integer> schedule = new ArrayList<>(Collections.singletonList(lid));
                List<Long> arrivalTime = new ArrayList<>(Collections.singletonList(0L));
                List<Integer> picked = new ArrayList<>(Collections.singletonList(0));
                workers.add(new Worker(wid++, 0, cap, speed, schedule, arrivalTime, picked));
                if(wid == nW) break;
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("Error initializing stream");
        }
        System.out.println("workers.size() = " + workers.size());
        return workers;
    }

    public static double stod(String str) {
        return Double.parseDouble(str);
    }

    public static int stoi(String str) {
        return Integer.parseInt(str);
    }

    public static long stol(String str) {
        return Long.parseLong(str);
    }
}
