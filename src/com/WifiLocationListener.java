package com.triptrack;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

class WifiLocationListener extends BaseLocationListener {
  public static final String TAG = "WifiLocationListener";

  public WifiLocationListener(LocationSampler locationSampler,
                              LocationManager locationManager) {
    super(locationSampler, locationManager);
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location == null) {
      Log.w(Constants.TAG + ":" + TAG, "location is null!");
      return;
    }
    locationManager.removeUpdates(this);
    Log.d(Constants.TAG + ":" + TAG, "Location: " + location.getAccuracy());

    locationSampler.setLocation(location);
  }
}
