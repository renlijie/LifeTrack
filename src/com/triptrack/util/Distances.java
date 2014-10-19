package com.triptrack.util;


import android.location.Location;

public class Distances {
  private Distances() {}

  public static float getDistanceBetween(
      double lat, double lng, double preLat, double preLng) {
    float[] results = new float[1];
    Location.distanceBetween(lat, lng, preLat, preLng, results);
    return results[0];
  }
}
