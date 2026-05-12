package com.example.treasure_and_battle.utils;

import com.amap.api.maps.model.LatLng;

public class GeoUtils {

    public static double calculateDistance(LatLng a, LatLng b) {
        double r = 6371000;
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLon = Math.toRadians(b.longitude - a.longitude);
        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.latitude))
                * Math.cos(Math.toRadians(b.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    }
}
