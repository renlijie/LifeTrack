package com.triptrack.location;

import com.triptrack.util.Constants;

import android.location.LocationManager;

class CellLocationListener extends TimeoutLocationListener {

  public CellLocationListener(
      LocationSampler locationSampler,
      LocationManager locationManager) {
    super(
        locationSampler,
        locationManager,
        Double.MAX_VALUE,
        Constants.NETWORK_TIMEOUT_SECS);
  }

  @Override
  public String getLocationSource() {
    return "Cell";
  }
}
