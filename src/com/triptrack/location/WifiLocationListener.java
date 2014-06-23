package com.triptrack.location;

import com.triptrack.util.Constants;

import android.location.LocationManager;

class WifiLocationListener extends TimeoutLocationListener {

  public WifiLocationListener(
      LocationSampler locationSampler,
      LocationManager locationManager) {
    super(
        locationSampler,
        locationManager,
        Constants.WIFI_MIN_ACCURACY_METERS,
        Constants.NETWORK_TIMEOUT_SECS);
  }

  @Override
  public String getLocationSource() {
    return "WiFi";
  }
}
