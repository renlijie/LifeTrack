package com.lifetrack;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class LocationSampler extends BroadcastReceiver {
  private static final String TAG = "LocationSampler";
  private Context context;

  private void writeToDb(Location location) {
    FixDataStore fixDataStore = new FixDataStore(context);
    fixDataStore.open();
    fixDataStore.createFix(location);
    fixDataStore.close();
  }

  public void setLocation(String source, Location location) {
    Log.d(Constants.TAG + ":" + TAG,
          "Location(" + source + "): " + location.getAccuracy());
    writeToDb(location);
  }

  public void useNextListener(BaseLocationListener locationListener) {
    if (locationListener instanceof GpsLocationListener) {
      Log.d(Constants.TAG + ":" + TAG, "Switch to WiFi listener.");
      LocationManager locationManager = (LocationManager) context
        .getSystemService(Context.LOCATION_SERVICE);
      if (!tryWifi(locationManager))
        if (!tryCell(locationManager))
          Log.d(Constants.TAG + ":" + TAG, "Give up.");
    } else if (locationListener instanceof WifiLocationListener) {
      Log.d(Constants.TAG + ":" + TAG, "Switch to Cell listener.");
      LocationManager locationManager = (LocationManager) context
        .getSystemService(Context.LOCATION_SERVICE);
      if (!tryCell(locationManager))
        Log.d(Constants.TAG + ":" + TAG, "Give up.");
    } else if (locationListener instanceof CellLocationListener) {
      Log.d(Constants.TAG + ":" + TAG, "No listener left. Give up.");
    } else {
      throw new IllegalArgumentException("Illegal listener type: "
        + locationListener.getClass().getName());
    }
  }

  private boolean tryGps(LocationManager locationManager) {
    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      Log.d(Constants.TAG + ":" + TAG, "GPS enabled.");
      // register a gps location listener
      GpsLocationListener gpsLocationListener =
        new GpsLocationListener(this,
                                locationManager,
                                Constants.GPS_TIMEOUT_SECS);
      locationManager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER, 0, 0, gpsLocationListener);
      return true;
    } else {
      Log.d(Constants.TAG + ":" + TAG, "GPS not enabled.");
      return false;
    }
  }

  private boolean tryWifi(LocationManager locationManager) {
    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      ConnectivityManager connectivityManager = (ConnectivityManager)
        context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo wifiInfo = connectivityManager
        .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

      if (wifiInfo.isAvailable() == true) {
        Log.d(Constants.TAG + ":" + TAG, "WiFi available.");
        // register a wifi location listener
        WifiLocationListener wifiLocationListener =
          new WifiLocationListener(this,
                                   locationManager,
                                   Constants.NETWORK_TIMEOUT_SECS);
        locationManager.requestLocationUpdates(
          LocationManager.NETWORK_PROVIDER, 0, 0, wifiLocationListener);
        return true;
      } else {
        Log.d(Constants.TAG + ":" + TAG, "WiFi not available.");
      }
    }
    return false;
  }

  private boolean tryCell(LocationManager locationManager) {
    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      ConnectivityManager connectivityManager = (ConnectivityManager)
        context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo mobileInfo = connectivityManager
        .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

      if (mobileInfo.isAvailable() == true) {
        Log.d(Constants.TAG + ":" + TAG, "Cell available.");
        // register a cell location listener
        CellLocationListener cellLocationListener =
          new CellLocationListener(this,
                                   locationManager,
                                   Constants.NETWORK_TIMEOUT_SECS);
        locationManager.requestLocationUpdates(
          LocationManager.NETWORK_PROVIDER, 0, 0, cellLocationListener);
        return true;
      } else {
        Log.d(Constants.TAG + ":" + TAG, "Cell not available.");
      }
    }
    return false;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    this.context = context;

    LocationManager locationManager = (LocationManager) context
      .getSystemService(Context.LOCATION_SERVICE);

    if (!tryGps(locationManager))
      if (!tryWifi(locationManager))
        if (!tryCell(locationManager))
          Log.d(Constants.TAG + ":" + TAG, "Give up.");
  }
}
