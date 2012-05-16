package com.triptrack;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

public class RepeatingAlarm extends BroadcastReceiver {
    private static final String TAG = "RepeatingAlarm";

    void write(Context context, Location location) {
        FixDataStore fixDataStore = new FixDataStore(context);
        fixDataStore.open();
        fixDataStore.createFix(location);
        fixDataStore.close();       
    }
    
    @Override
    public void onReceive(final Context context, Intent intent) {
        final LocationManager locationManager =
            (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        
        final LocationListener cellLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null) {
                    Log.w(Constants.TAG + ":" + TAG, "location is null!");
                    return;
                }

                // TODO: add time out?
                locationManager.removeUpdates(this);
                Log.d(Constants.TAG + ":" + TAG, "CELL location " + location.getAccuracy());
                write(context, location);
            }

            @Override
            public void onStatusChanged(String provider, int status,
                Bundle extras) {
                // Log.v(Constants.TAG + ":" + TAG, "onStatusChanged: " +
                // provider);
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Log.v(Constants.TAG + ":" + TAG, "onProviderEnabled: "
                // + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                // TODO: status notice
                // Log.v(Constants.TAG + ":" + TAG, "onProviderDisabled: "
                // + provider);
            }
        };

        final LocationListener wifiLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null) {
                    Log.w(Constants.TAG + ":" + TAG, "location is null!");
                    return;
                }

                Log.d(Constants.TAG + ":" + TAG, "WIFI location " + location.getAccuracy());
                if (location.getAccuracy() > Constants.WIFI_ACC_THRESHOLD_METERS) {
                    Log.d(Constants.TAG + ":" + TAG, "WIFI ACC too low");
                    return;
                }
                
                locationManager.removeUpdates(this);
                write(context, location);
            }

            @Override
            public void onStatusChanged(String provider, int status,
                Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
        
        final LocationListener gpsLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null) {
                    Log.w(Constants.TAG + ":" + TAG, "location is null!");
                    return;
                }
                locationManager.removeUpdates(this);                
                Log.d(Constants.TAG + ":" + TAG, "GPS location " + location.getAccuracy());
                write(context, location);
            }

            @Override
            public void onStatusChanged(String provider, int status,
                Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
        
        try{
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, Constants.MIN_TIME_MILLIS,
                    Constants.MIN_DISTANCE_METERS, gpsLocationListener);
            } else {
                Log.w(Constants.TAG + ":" + TAG, "GPS not enabled.");
            }
        }catch(Exception ex){
            Log.w(Constants.TAG + ":" + TAG, "Exception: GPS not enabled.");
        }
        
        try{
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                ConnectivityManager connectivityMgr = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo wifiInfo = connectivityMgr
                        .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (wifiInfo.isAvailable() == true) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, Constants.MIN_TIME_MILLIS,
                        Constants.MIN_DISTANCE_METERS, wifiLocationListener);
                    
                } else {
                    Log.w(Constants.TAG + ":" + TAG, "WIFI not available.");
                    NetworkInfo mobileInfo = connectivityMgr
                        .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

                    if (mobileInfo.isAvailable() == true) { 
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, Constants.MIN_TIME_MILLIS,
                            Constants.MIN_DISTANCE_METERS, cellLocationListener);
                    } else {
                        Log.w(Constants.TAG + ":" + TAG, "CELL not available.");                      
                    }
                }
            } else {
                Log.w(Constants.TAG + ":" + TAG, "WIFI and CELL not available.");
            }
        }catch(Exception ex){
            Log.w(Constants.TAG + ":" + TAG, "Exception: WIFI and CELL not availiable.");
        }
    }
}