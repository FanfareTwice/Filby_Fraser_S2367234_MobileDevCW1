package org.me.gcu.filby_fraser_s2367234.util;

import org.me.gcu.filby_fraser_s2367234.model.LocationPoint;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.util.List;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() { }

    // Haversine formula
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Nearest point of the situation; MAX_VALUE if it has no coordinates
    public static double distanceToSituationKm(double lat, double lon, Situation s) {
        List<LocationPoint> pts = s.getMappablePoints();
        double best = Double.MAX_VALUE;
        for (LocationPoint p : pts) {
            best = Math.min(best, distanceKm(lat, lon, p.getLatitude(), p.getLongitude()));
        }
        return best;
    }
}
