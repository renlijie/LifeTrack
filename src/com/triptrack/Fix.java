package com.triptrack;

import android.content.ContentValues;
import android.database.Cursor;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.triptrack.util.Constants;
import com.triptrack.util.Cursors;

public class Fix extends ClusterItem {
  private LatLng latLng;
  private long utc;
  private float acc;

  public Fix(long utc, double lat, double lng, float acc) {
    latLng = new LatLng(lat, lng);
    this.utc = utc;
    this.acc = acc;
  }

  @Override
  public LatLng getPosition() {
    return latLng;
  }

  public double getLat() {
    return latLng.latitude;
  }

  public double getLng() {
    return latLng.longitude;
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

  public static Fix fromContentValues(ContentValues values) {
    return new Fix(
        values.getAsLong(Constants.KEY_UTC),
        values.getAsDouble(Constants.COL_LAT),
        values.getAsDouble(Constants.COL_LNG),
        values.getAsFloat(Constants.COL_ACC));
  }

  public static Fix fromCursor(Cursor cursor) {
    return new Fix(
        Cursors.getUtc(cursor),
        Cursors.getLat(cursor),
        Cursors.getLng(cursor),
        Cursors.getAcc(cursor));
  }
}
