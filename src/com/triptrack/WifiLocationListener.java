package com.triptrack;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

class WifiLocationListener extends BaseLocationListener {
  private static final String TAG = "WifiLocationListener";

  public WifiLocationListener(LocationSampler locationSampler,
                              LocationManager locationManager,
                              int timeoutSec) {
    super(locationSampler, locationManager, timeoutSec);
  }

  @Override
  public double getMinAccuracy() {
    return Constants.WIFI_MIN_ACCURACY;
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location == null) {
      Log.w(Constants.TAG + ":" + TAG, "location is null!");
      return;
    }
    if (location.getAccuracy() > Constants.WIFI_ACC_THRESHOLD_METERS) {
      Log.d(Constants.TAG + ":" + TAG,
            "WiFi accuracy too low: " + location.getAccuracy());
      return;
    }
    locationManager.removeUpdates(this);

    locationSampler.setLocation(TAG, location);
    isCompleted = true;
  }
}
