package edu.ecnu.hwu.utils;

import edu.ecnu.hwu.pojos.Point;

public class DistanceUtils {
    public static double eulerDistance(Point p1, Point p2) {
        return Math.sqrt((p1.getLng() - p2.getLng()) * (p1.getLng() - p2.getLng()) + (p1.getLat() - p2.getLat()) * (p1.getLat() - p2.getLat()));
    }

    private static final double RADIUS = 6367000.0;

    private static final double COEFFICIENT = 1.4;

    public static long timeConsuming(double distance, double speed) {
        return (long) Math.ceil(distance / speed);
    }

    public static long timeConsuming(Point from, Point to, double speed) {
        return (long) Math.ceil(getDistance(from, to) / speed);
    }

    public static double getDistance(Point from, Point to) {
        return Global.isEuler ? eulerDistance(from, to) : greatCircleDistance(from.getLng(), from.getLat(), to.getLng(), to.getLat()) * COEFFICIENT;
    }

    private static double greatCircleDistance(double lng1, double lat1, double lng2, double lat2) {
        double deltaLng = lng2 - lng1;
        double deltaLat = lat2 - lat1;
        double b = (lat1 + lat2) / 2.0;
        double x = Math.toRadians(deltaLng) * RADIUS * Math.cos(Math.toRadians(b));
        double y = RADIUS * Math.toRadians(deltaLat);
        return Math.sqrt(x * x + y * y);
    }
}
