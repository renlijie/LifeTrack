package com.triptrack.util;

import android.database.Cursor;

public class Cursors {
  private Cursors() {}

  public static long getUtc(Cursor cursor) {
    return cursor.getLong(cursor.getColumnIndex(Constants.KEY_UTC));
  }

  public static float getAcc(Cursor cursor) {
    return cursor.getFloat(cursor.getColumnIndex(Constants.COL_ACC));
  }

  public static double getLat(Cursor cursor) {
    return cursor.getDouble(cursor.getColumnIndex(Constants.COL_LAT));
  }

  public static double getLng(Cursor cursor) {
    return cursor.getDouble(cursor.getColumnIndex(Constants.COL_LNG));
  }
}
