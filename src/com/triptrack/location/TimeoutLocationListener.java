package com.triptrack.location;

import com.triptrack.util.Constants;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

abstract class TimeoutLocationListener implements LocationListener {
  private static final String TAG = "TimeoutLocationListener";

  private final LocationSampler locationSampler;
  private final LocationManager locationManager;
  private final double accuracyThreshold;
  private final int timeoutSecs;

  private boolean isCompleted;

  public TimeoutLocationListener(
      LocationSampler locationSampler,
      LocationManager locationManager,
      double accuracyThreshold,
      int timeoutSecs) {
    this.locationSampler = locationSampler;
    this.locationManager = locationManager;
    this.accuracyThreshold = accuracyThreshold;
    this.timeoutSecs = timeoutSecs;
    isCompleted = false;
  }

  public void startListening() {
    locationManager.requestLocationUpdates(
        LocationManager.NETWORK_PROVIDER, 0, 0, this);
    attachTimeout(timeoutSecs);
  }

  public void stopListening() {
    locationManager.removeUpdates(this);
  }

  private void attachTimeout(int timeoutSecs) {
    new Handler().postDelayed(locationSampler, timeoutSecs * 1000);
    Log.d(Constants.TAG + ":" + TAG, "Set timeout for " + getLocationSource()
        + ": " + timeoutSecs);
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location == null) {
      Log.w(Constants.TAG + ":" + TAG,
          "Location from " + getLocationSource() + " is null!");
      return;
    }
    if (location.getAccuracy() > accuracyThreshold) {
      Log.d(Constants.TAG + ":" + TAG,
          "Ignored " + getLocationSource() + " location with low accuracy: "
              + location.getAccuracy());
      return;
    }
    locationManager.removeUpdates(this);
    locationSampler.saveLocation(getLocationSource(), location);
    isCompleted = true;
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {}

  @Override
  public void onProviderEnabled(String provider) {}

  @Override
  public void onProviderDisabled(String provider) {}

  abstract String getLocationSource();
}
