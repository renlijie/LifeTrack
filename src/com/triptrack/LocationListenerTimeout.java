package com.triptrack;

import android.location.LocationManager;
import android.util.Log;

public class LocationListenerTimeout implements Runnable {
  private static final String TAG = "LocationListenerTimeout";

  private LocationSampler locationSampler;
  private LocationManager locationManager;
  private BaseLocationListener locationListener;

  public LocationListenerTimeout(LocationSampler locationSampler,
                                 LocationManager locationManager,
                                 BaseLocationListener locationListener) {
    this.locationSampler = locationSampler;
    this.locationManager = locationManager;
    this.locationListener = locationListener;
  }

  @Override
  public void run() {
    if (locationListener.isCompleted()) {
      Log.d(Constants.TAG + ":" + TAG,
            locationListener.getClass().getName() + " has already completed.");
      return;
    }
    Log.d(Constants.TAG + ":" + TAG,
          locationListener.getClass().getName() + " timeout.");
    locationManager.removeUpdates(locationListener);

    locationSampler.useNextListener(locationListener);
  }
}
