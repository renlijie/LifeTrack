package com.triptrack;

import android.location.LocationListener;
import android.location.LocationManager;

class CellLocationListener extends BaseLocationListener {
  public static final String TAG = "CellLocationListener";

  public CellLocationListener(LocationSampler locationSampler,
                              LocationManager locationManager) {
    super(locationSampler, locationManager);
  }
}
