package com.triptrack;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

class GpsLocationListener extends BaseLocationListener {
  public static final String TAG = "GpsLocationListener";

  public GpsLocationListener(LocationSampler locationSampler,
                             LocationManager locationManager,
                             int timeoutSec) {
    super(locationSampler, locationManager, timeoutSec);
  }

  @Override
  public double getMinAccuracy() {
    return Constants.GPS_MIN_ACCURACY;
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
