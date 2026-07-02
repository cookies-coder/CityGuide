package com.city.guide.utils;

public class Gcj02ToBd09Converter {
    private static final double x_pi = 3.14159265358979324 * 3000.0 / 180.0;

    /**
     * 高德坐标 (GCJ-02) -> 百度坐标 (BD-09)
     */
    public static double[] gcj02ToBd09(double lng, double lat) {
        double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * x_pi);
        double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * x_pi);
        double bd_lng = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return new double[]{bd_lng, bd_lat};
    }
}