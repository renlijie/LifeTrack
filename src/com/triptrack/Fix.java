package com.triptrack;

import com.google.android.gms.maps.model.LatLng;

public class Fix {
  public LatLng latLng;
  public long utc;
  public float acc;
  public int freshness;

  public Fix(long utc, double lat, double lng, float acc, int freshness) {
    latLng = new LatLng(lat, lng);
    this.utc = utc;
    this.acc = acc;
    this.freshness = freshness;
  }
}
