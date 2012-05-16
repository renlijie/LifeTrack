package com.triptrack;

public final class Constants {
    public static final String TAG = "TripTrackLog";

    public static final int DEFAULT_INTERVAL_MINS = 10;
    public static final int TIMEOUT_SECS = 60;
    public static final long MIN_TIME_MILLIS = 0;
    public static final float MIN_DISTANCE_METERS = 0;
    public static final int WIFI_ACC_THRESHOLD_METERS = 200;
    
    public static final String PREFS_NAME = "TrackingPref";
    public static final String HISTORY_FILE = "FixHistory.dat";
    
    public static final String ENABLED = "Enabled";
    public static final String INTERVAL_MINS = "intervalMins";

    public static final String KEY_UTC = "utc";
    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";
    public static final String KEY_ACC = "acc";

    static final int HANDLER_TOAST = 0;
    static final int HANDLER_PROGRESSBAR_SHOW = 1;
    static final int HANDLER_PROGRESSBAR_DISMISS = 2;
    static final int HANDLER_PROGRESSBAR_SETMAX = 3;
    static final int HANDLER_PROGRESSBAR_SETPROGRESS = 4;

    public static final String USAGE =
        "Ever find yourself too lazy to keep records of where you have been on your amazing trip while you really should?\n\n"
            + "TripTrack is designed just for doing that "
            + "\u2014"
            + " automatically!\n\n"
            + "By periodically sampling your fine/coarse locations found by exploiting the GPS, WiFi and cell-tower signals, "
            + "this app keeps a record of your \"trip track\" during the time it is enabled, power efficiently.\n\n"
            + "The highlight of this application include:\n\n"
            + "1) Automatically chooses the best accuracy available.\n\n"
            + "2) Makes use of very little system resources and is very power efficient (especially when GPS is turned off). "
            + "Simply enable it and leave it on for days, months, or even years to create your long term \"life track\"!\n\n"
            + "3) Supports user defined sampling frequency.\n\n"
            + "4) Includes an intuitive map interface for visualizing the recorded location fixes. It's like reading a diary!\n\n"
            + "5) Location fixes are deletable from the map interface.\n\n"
            + "6) Stores all your information only on your phone and does not upload your data at any time.\n\n"
           
            + "More features are still in development! (Let me know what kind of features you would like to be implemented!)\n\nEnjoy!\n\n"
            + "Author: Lijie Ren \n(lijie.project@gmail.com)";
}
