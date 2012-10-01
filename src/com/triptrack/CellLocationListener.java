package com.triptrack;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

class CellLocationListener extends BaseLocationListener {
  public static final String TAG = "CellLocationListener";

  public CellLocationListener(LocationSampler locationSampler,
                              LocationManager locationManager) {
    super(locationSampler, locationManager);
  }

  @Override
  public double getMinAccuracy() {
    return Constants.CELL_MIN_ACCURACY;
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location == null) {
      Log.w(Constants.TAG + ":" + TAG, "location is null!");
      return;
    }
    locationManager.removeUpdates(this);
    locationSampler.setLocation(TAG, location);
    isCompleted = true;
  }
}
