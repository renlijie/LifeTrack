package com.triptrack.location;

import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class LocationSampler extends BroadcastReceiver implements Runnable {
  private static final String TAG = "LocationSampler";

  private TimeoutLocationListener currentLocationListener;
  private LocationManager locationManager;
  private Context context;

  private void writeToDb(Location location) {
    GeoFixDataStore geoFixDataStore = new GeoFixDataStore(context);
    geoFixDataStore.open();
    try {
      geoFixDataStore.insertGeoFixOrThrow(
          location.getTime(),
          location.getLatitude(),
          location.getLongitude(),
          location.getAccuracy());
    } catch (SQLException e ) {
      Log.e(Constants.TAG + ":" + TAG, "Duplicate UTC: " + location.getTime());
    }
    geoFixDataStore.close();
  }

  public void saveLocation(String source, Location location) {
    Log.d(Constants.TAG + ":" + TAG,
        source + " accuracy: " + location.getAccuracy());
    writeToDb(location);
  }

  public void startWithNextListener() {
    if (currentLocationListener instanceof GpsLocationListener) {
      Log.d(Constants.TAG + ":" + TAG, "Switching to WiFi.");
      startWithWifi();
      return;
    }

    if (currentLocationListener instanceof WifiLocationListener) {
      Log.d(Constants.TAG + ":" + TAG, "Switching to Cell.");
      startWithCell();
      return;
    }

    if (currentLocationListener instanceof CellLocationListener) {
      Log.d(Constants.TAG + ":" + TAG, "No listener left. Giving up.");
      return;
    }

    throw new IllegalArgumentException("Unknown listener type: "
        + currentLocationListener.getClass().getName());
  }

  private void startWithGps() {
    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      Log.d(Constants.TAG + ":" + TAG, "GPS is available.");
      currentLocationListener = new GpsLocationListener(
          this,
          locationManager);
      currentLocationListener.startListening();
    } else {
      Log.d(Constants.TAG + ":" + TAG,
          "GPS is not available. Switching to WiFi.");
      startWithWifi();
    }
  }

  private void startWithWifi() {
    if (isWifiAvailable()) {
      Log.d(Constants.TAG + ":" + TAG, "WiFi is available.");
      currentLocationListener = new WifiLocationListener(
          this,
          locationManager);
      currentLocationListener.startListening();
    } else {
      Log.d(Constants.TAG + ":" + TAG,
          "WiFi is not available. Switching to Cell.");
      startWithCell();
    }
  }

  private void startWithCell() {
    if (isCellAvailable()) {
      Log.d(Constants.TAG + ":" + TAG, "Cell is available.");
      currentLocationListener = new CellLocationListener(
          this,
          locationManager);
      currentLocationListener.startListening();
    } else {
      Log.d(Constants.TAG + ":" + TAG, "Cell is not available. Giving up.");
    }
  }

  private boolean isWifiAvailable() {
    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      ConnectivityManager connectivityManager = (ConnectivityManager)
          context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo wifiInfo = connectivityManager
          .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      return wifiInfo.isAvailable();
    }
    return false;
  }


  private boolean isCellAvailable() {
    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      ConnectivityManager connectivityManager = (ConnectivityManager)
          context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo mobileInfo = connectivityManager
          .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
      return mobileInfo.isAvailable();
    }
    return false;
  }

  /**
   * Upon receiving an alarm from the system, start listening to user location.
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    this.context = context;
    locationManager = (LocationManager) context
        .getSystemService(Context.LOCATION_SERVICE);

    startWithGps();
  }

  /**
   * If the current location listener timed out, use the next listener.
   */
  @Override
  public void run() {
    if (currentLocationListener.isCompleted()) {
      Log.d(Constants.TAG + ":" + TAG,
          currentLocationListener.getClass().getName()
              + " has already completed.");
      return;
    }
    Log.d(Constants.TAG + ":" + TAG,
        currentLocationListener.getClass().getName() + " timed out.");
    currentLocationListener.stopListening();

    startWithNextListener();
  }
}
