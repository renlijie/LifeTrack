package com.triptrack.util;

/**
 * constants used in TripTrack
 */
public final class Constants {

  /**
   * application-level tag
   */
  public static final String TAG = "TripTrackLog";

  /**
   * default sampling interval
   */
  public static final int DEFAULT_INTERVAL_MINS = 5;

  /**
   * name of the application's shared preferences file
   */
  public static final String PREFS_FILE = "TripTrackPref";

  /**
   * name of the exported csv file
   */
  public static final String HISTORY_FILE = "MyGeoFixHistory.dat";

  /**
   * key to check whether location sample is enabled
   */
  public static final String ENABLED = "enabled";

  /**
   * key to check the current sampling interval
   */
  public static final String INTERVAL_MINS = "interval";

  /**
   * keys for a geo fix's UTC, latitude, longitude and accuracy in DB
   */
  public static final String KEY_UTC = "utc";
  public static final String COL_LAT = "lat";
  public static final String COL_LNG = "lng";
  public static final String COL_ACC = "acc";
  public static final String COL_USER_MOVED = "moved";
  public static final String COL_LOCAL_DATE = "localdate";

  /**
   * maximum radius in meters of an acceptable fix in each category
   */
  public static final double GPS_MIN_ACCURACY_METERS = 100;
  public static final double WIFI_MIN_ACCURACY_METERS = 200;

  /**
   * GPS/network timeouts
   */
  public static final int GPS_TIMEOUT_SECS = 60;
  public static final int NETWORK_TIMEOUT_SECS = 30;

  /**
   * asynchronous UI control params
   */
  public static final int HANDLER_TOAST = 0;
  public static final int HANDLER_PROGRESSBAR_DISMISS = 2;
  public static final int HANDLER_PROGRESSBAR_SHOWMAX = 3;
  public static final int HANDLER_PROGRESSBAR_SETPROGRESS = 4;

  /**
   * about TripTrack
   */
  public static final String ABOUT =
      "Ever find yourself too lazy to keep records of where you have been on your amazing trip while you really should?\n\n" +

          "TripTrack is designed just for doing that " + "\u2014" + " automatically!\n\n" +

          "By periodically sampling your fine/coarse locations found by exploiting the GPS, WiFi and cell-tower signals, " +
          "this app keeps a record of your \"life track\" during the time it is enabled, power efficiently.\n\n" +

          "The highlight of this application include:\n\n" +

          "1) Automatically chooses the best accuracy available.\n\n" +

          "2) Makes use of very little system resources and is very power efficient (especially when GPS is turned off). " +
          "   Simply enable it and leave it on for days, months, or even years to create your own \"life track\"!\n\n" +

          "3) Supports user defined sampling frequency.\n\n" +

          "4) Includes an intuitive map interface for visualizing the recorded location fixes. It's like reading a diary!\n\n" +

          "5) Location fixes are deletable from the map interface.\n\n" +

          "6) Stores all your information only on your phone and does not upload your data at any time.\n\n" +

          "More features are still in development! (Let me know what kind of features you would like to be implemented!)\n\n" +

          "Enjoy!\n\n" +

          "Author: Lijie Ren \n" +
          "(lijie.project@gmail.com)";
}
