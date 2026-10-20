#include "native-geo.h"

// Degree to Radian conversion
static inline double deg2rad(double deg) {
    return deg * (PI / 180.0);
}

/**
 * Calculates high-precision distance between two GPS coordinates
 * using the Haversine formula.
 */
double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
    double dLat = deg2rad(lat2 - lat1);
    double dLon = deg2rad(lon2 - lon1);

    double a = std::sin(dLat / 2.0) * std::sin(dLat / 2.0) +
               std::cos(deg2rad(lat1)) * std::cos(deg2rad(lat2)) *
               std::sin(dLon / 2.0) * std::sin(dLon / 2.0);

    double c = 2.0 * std::atan2(std::sqrt(a), std::sqrt(1.0 - a));
    return EARTH_RADIUS_METERS * c;
}

/**
 * Verifies if user is within the circular campus boundary radius.
 */
bool isPointInsideRadius(double currentLat, double currentLon, double centerLat, double centerLon, double radiusMeters) {
    double distance = calculateDistanceMeters(currentLat, currentLon, centerLat, centerLon);
    return distance <= radiusMeters;
}

/**
 * Ray Casting Algorithm for checking if a coordinate is inside
 * an arbitrary campus boundary polygon (e.g., specific college grounds).
 */
bool isPointInPolygon(const GeoPoint& point, const std::vector<GeoPoint>& polygon) {
    size_t n = polygon.size();
    if (n < 3) return false;

    bool inside = false;
    for (size_t i = 0, j = n - 1; i < n; j = i++) {
        if (((polygon[i].longitude > point.longitude) != (polygon[j].longitude > point.longitude)) &&
            (point.latitude < (polygon[j].latitude - polygon[i].latitude) *
                              (point.longitude - polygon[i].longitude) /
                              (polygon[j].longitude - polygon[i].longitude) + polygon[i].latitude)) {
            inside = !inside;
        }
    }
    return inside;
}

/**
 * Smart Battery-Optimized GPS Polling Interval:
 * Prevents phone battery drain by dynamically slowing down GPS polling
 * when user is far away from college, and increasing precision as they get closer.
 *
 * Distance > 10 km -> poll every 15 minutes (900 sec)
 * Distance 5 - 10 km -> poll every 5 minutes (300 sec)
 * Distance 1 - 5 km -> poll every 2 minutes (120 sec)
 * Distance 300m - 1 km -> poll every 30 seconds (30 sec)
 * Distance <= 300m -> poll every 10 seconds (10 sec)
 */
int calculateBatteryOptimizedInterval(double distanceToCampusMeters) {
    if (distanceToCampusMeters > 10000.0) {
        return 900; // 15 mins
    } else if (distanceToCampusMeters > 5000.0) {
        return 300; // 5 mins
    } else if (distanceToCampusMeters > 1000.0) {
        return 120; // 2 mins
    } else if (distanceToCampusMeters > 300.0) {
        return 30;  // 30 secs
    } else {
        return 10;  // 10 secs
    }
}

// JNI Bridge implementations
extern "C" {

JNIEXPORT jdouble JNICALL
Java_com_campussilencer_app_nativebridge_NativeGeoEngine_calculateDistance(
        JNIEnv* env, jobject thiz,
        jdouble lat1, jdouble lon1,
        jdouble lat2, jdouble lon2) {
    return (jdouble) calculateDistanceMeters(lat1, lon1, lat2, lon2);
}

JNIEXPORT jboolean JNICALL
Java_com_campussilencer_app_nativebridge_NativeGeoEngine_isInsideCampus(
        JNIEnv* env, jobject thiz,
        jdouble userLat, jdouble userLon,
        jdouble campusLat, jdouble campusLon,
        jdouble radiusMeters) {
    bool inside = isPointInsideRadius(userLat, userLon, campusLat, campusLon, radiusMeters);
    return inside ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_campussilencer_app_nativebridge_NativeGeoEngine_getBatteryOptimizedInterval(
        JNIEnv* env, jobject thiz,
        jdouble distanceMeters) {
    return (jint) calculateBatteryOptimizedInterval(distanceMeters);
}

}
