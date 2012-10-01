package com.triptrack;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

abstract class BaseLocationListener implements LocationListener {
  private static final String TAG = "BaseLocationListener";

  protected LocationSampler locationSampler;
  protected LocationManager locationManager;
  protected boolean isCompleted = false;

  public boolean isCompleted() {
    return isCompleted;
  }

  public BaseLocationListener(LocationSampler locationSampler,
                              LocationManager locationManager) {
    this.locationSampler = locationSampler;
    this.locationManager = locationManager;

    Handler timeout = new Handler();
    timeout.postDelayed(new LocationListenerTimeout(locationSampler,
                                                    locationManager,
                                                    this),
                        Constants.TIMEOUT_SECS * 1000);
    Log.d(Constants.TAG + ":" + TAG, "Added timeout: "
            + Constants.TIMEOUT_SECS);
  }

  abstract public double getMinAccuracy();

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {}

  @Override
  public void onProviderEnabled(String provider) {}

  @Override
  public void onProviderDisabled(String provider) {}
};
