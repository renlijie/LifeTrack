package com.triptrack;

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

  @Override
  public void onReceive(Context context, Intent intent) {
    this.context = context;

    LocationManager locationManager = (LocationManager) context
      .getSystemService(Context.LOCATION_SERVICE);
    GpsLocationListener gpsLocationListener =
      new GpsLocationListener(this, locationManager);
    WifiLocationListener wifiLocationListener =
      new WifiLocationListener(this, locationManager);
    CellLocationListener cellLocationListener =
      new CellLocationListener(this, locationManager);

    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      Log.d(Constants.TAG + ":" + TAG, "GPS enabled.");
      // register a gps location listener
      locationManager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER, 0, 0, gpsLocationListener);
    } else {
      Log.d(Constants.TAG + ":" + TAG, "GPS not enabled.");
    }

    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      ConnectivityManager connectivityManager = (ConnectivityManager)
        context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo wifiInfo = connectivityManager
        .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

      if (wifiInfo.isAvailable() == true) {
        Log.d(Constants.TAG + ":" + TAG, "WiFi available.");
        // register a wifi location listener
        locationManager.requestLocationUpdates(
          LocationManager.NETWORK_PROVIDER, 0, 0, wifiLocationListener);
      } else {
        Log.d(Constants.TAG + ":" + TAG, "WiFi not available.");
        NetworkInfo mobileInfo = connectivityManager
          .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileInfo.isAvailable() == true) {
          Log.d(Constants.TAG + ":" + TAG, "Cell available.");
          // register a cell location listener
          locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 0, 0, cellLocationListener);
        } else {
          Log.d(Constants.TAG + ":" + TAG, "Cell not available.");
        }
      }
    } else {
      Log.w(Constants.TAG + ":" + TAG, "WiFi and Cell not available.");
    }
  }
}
