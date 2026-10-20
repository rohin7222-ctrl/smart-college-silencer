package com.campussilencer.app.nativebridge

/**
 * NativeGeoEngine (Kotlin Layer -> C++ JNI)
 * Bridges Kotlin with the native C++ Haversine and Polygon calculations
 * for high performance and low battery consumption.
 */
object NativeGeoEngine {

    init {
        try {
            System.loadLibrary("campusgeo")
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("Native library 'campusgeo' failed to load: ${e.message}")
        }
    }

    /**
     * Calculates high-precision distance between two GPS coordinates in meters using C++ Haversine.
     */
    external fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double

    /**
     * Checks if user coordinate is inside circular campus radius using C++ native math.
     */
    external fun isInsideCampus(
        userLat: Double, userLon: Double,
        campusLat: Double, campusLon: Double,
        radiusMeters: Double
    ): Boolean

    /**
     * Returns optimal GPS polling interval in seconds depending on distance.
     * Prevents battery drain by slowing down polling when user is far away.
     */
    external fun getBatteryOptimizedInterval(distanceMeters: Double): Int
}
