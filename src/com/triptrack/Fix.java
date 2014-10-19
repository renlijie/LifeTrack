package com.triptrack;

import com.triptrack.util.Constants;

import android.content.ContentValues;

import com.google.android.gms.maps.model.LatLng;

public class Fix {
  private LatLng latLng;
  private long utc;
  private float acc;

  public Fix(ContentValues values) {
    latLng = new LatLng(
        values.getAsDouble(Constants.COL_LAT),
        values.getAsDouble(Constants.COL_LNG));
    utc = values.getAsLong(Constants.KEY_UTC);
    acc = values.getAsFloat(Constants.COL_ACC);
  }

  public Fix(long utc, double lat, double lng, float acc, int freshness) {
    latLng = new LatLng(lat, lng);
    this.utc = utc;
    this.acc = acc;
  }

  public Fix(long utc, double lat, double lng, float acc) {
    latLng = new LatLng(lat, lng);
    this.utc = utc;
    this.acc = acc;
  }

  public double getLat() {
    return latLng.latitude;
  }

  public double getLng() {
    return latLng.longitude;
  }

  public LatLng getLatLng() {
    return latLng;
  }

  public float getAcc() {
    return acc;
  }

  public long getUtc() {
    return utc;
  }

  public ContentValues toContentValues() {
    ContentValues values = new ContentValues();
    values.put(Constants.KEY_UTC, utc);
    values.put(Constants.COL_LAT, latLng.latitude);
    values.put(Constants.COL_LNG, latLng.longitude);
    values.put(Constants.COL_ACC, acc);
    return values;
  }
}
