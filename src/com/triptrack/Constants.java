package com.triptrack;

public final class Constants {

  // ------------------- in use-----------------------

  /**
   * application-level tag
   */
  static final String TAG = "TripTrackLog";

  /**
   * default sampling interval
   */
  static final int DEFAULT_INTERVAL_MINS = 10;

  /**
   * name of the application's shared preferences file
   */
  static final String PREFS_NAME = "TrackingPref";

  /**
   * name of the exported csv file.
   */
  static final String HISTORY_FILE = "FixHistory.dat";

  /**
   * key to check whether location sample is enabled.
   */
  static final String ENABLED = "Enabled";

  /**
   * key to check the current sampling interval.
   */
  static final String INTERVAL_MINS = "intervalMins";

  /**
   * keys for a fix's UTC, latitude, longitude and accuracy in db.
   */
  static final String KEY_UTC = "utc";
  static final String KEY_LAT = "lat";
  static final String KEY_LNG = "lng";
  static final String KEY_ACC = "acc";

  /**
   * the maximum radius in meters of an acceptable fix in each category.
   */
  static final double GPS_MIN_ACCURACY = 100;
  static final double WIFI_MIN_ACCURACY = 200;
  static final double CELL_MIN_ACCURACY = 1000000;

  /**
   * GPS/network timeout
   */
  static final int GPS_TIMEOUT_SECS = 60;
  static final int NETWORK_TIMEOUT_SECS = 30;

  /**
   * asynchronous UI control params.
   */
  static final int HANDLER_TOAST = 0;
  static final int HANDLER_PROGRESSBAR_SHOW = 1;
  static final int HANDLER_PROGRESSBAR_DISMISS = 2;
  static final int HANDLER_PROGRESSBAR_SETMAX = 3;
  static final int HANDLER_PROGRESSBAR_SETPROGRESS = 4;

  /**
   * about page
   */
  static final String ABOUT =
    "Ever find yourself too lazy to keep records of where you have been on your amazing trip while you really should?\n\n" +

    "TripTrack is designed just for doing that " + "\u2014" + " automatically!\n\n" +

    "By periodically sampling your fine/coarse locations found by exploiting the GPS, WiFi and cell-tower signals, " +
    "this app keeps a record of your \"trip track\" during the time it is enabled, power efficiently.\n\n" +

    "The highlight of this application include:\n\n" +

    "1) Automatically chooses the best accuracy available.\n\n" +

    "2) Makes use of very little system resources and is very power efficient (especially when GPS is turned off). " +
    "   Simply enable it and leave it on for days, months, or even years to create your long term \"life track\"!\n\n" +

    "3) Supports user defined sampling frequency.\n\n" +

    "4) Includes an intuitive map interface for visualizing the recorded location fixes. It's like reading a diary!\n\n" +

    "5) Location fixes are deletable from the map interface.\n\n" +

    "6) Stores all your information only on your phone and does not upload your data at any time.\n\n" +

    "More features are still in development! (Let me know what kind of features you would like to be implemented!)\n\n" +

    "Enjoy!\n\n" +

    "Author: Lijie Ren \n" +
    "(lijie.project@gmail.com)";

  // ----------------- not in use---------------------

}
