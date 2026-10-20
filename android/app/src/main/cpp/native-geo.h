#ifndef NATIVE_GEO_H
#define NATIVE_GEO_H

#include <jni.h>
#include <cmath>
#include <vector>

#define EARTH_RADIUS_METERS 6371000.0
#define PI 3.14159265358979323846

struct GeoPoint {
    double latitude;
    double longitude;
};

// Core C++ calculation functions
double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2);
bool isPointInsideRadius(double currentLat, double currentLon, double centerLat, double centerLon, double radiusMeters);
bool isPointInPolygon(const GeoPoint& point, const std::vector<GeoPoint>& polygon);
int calculateBatteryOptimizedInterval(double distanceToCampusMeters);

#ifdef __cplusplus
extern "C" {
#endif

// JNI Bridge methods for Kotlin/Java
JNIEXPORT jdouble JNICALL
Java_com_campussilencer_app_nativebridge_NativeGeoEngine_calculateDistance(
        JNIEnv* env, jobject thiz,
        jdouble lat1, jdouble lon1,
        jdouble lat2, jdouble lon2);

JNIEXPORT jboolean JNICALL
Java_com_campussilencer_app_nativebridge_NativeGeoEngine_isInsideCampus(
        JNIEnv* env, jobject thiz,
        jdouble userLat, jdouble userLon,
        jdouble campusLat, jdouble campusLon,
        jdouble radiusMeters);

JNIEXPORT jint JNICALL
Java_com_campussilencer_app_nativebridge_NativeGeoEngine_getBatteryOptimizedInterval(
        JNIEnv* env, jobject thiz,
        jdouble distanceMeters);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_GEO_H
