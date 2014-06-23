package com.triptrack.location;

import com.triptrack.util.Constants;

import android.location.LocationManager;

class GpsLocationListener extends TimeoutLocationListener {

  public GpsLocationListener(
      LocationSampler locationSampler,
      LocationManager locationManager) {
    super(
        locationSampler,
        locationManager,
        Constants.GPS_MIN_ACCURACY_METERS,
        Constants.GPS_TIMEOUT_SECS);
  }

  @Override
  public String getLocationSource() {
    return "GPS";
  }
}
