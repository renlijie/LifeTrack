package com.triptrack;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

abstract class BaseLocationListener implements LocationListener {
  public static final String TAG = "BaseLocationListener";

  protected LocationSampler locationSampler;
  protected LocationManager locationManager;

  public BaseLocationListener(LocationSampler locationSampler,
                              LocationManager locationManager) {
    this.locationSampler = locationSampler;
    this.locationManager = locationManager;
  }
  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {}

  @Override
  public void onProviderEnabled(String provider) {}

  @Override
  public void onProviderDisabled(String provider) {}
};
