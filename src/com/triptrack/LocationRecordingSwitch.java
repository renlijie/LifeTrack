package com.triptrack;

import com.triptrack.location.LocationSampler;
import com.triptrack.util.Constants;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * A switch that controls the broadcast of periodic system alarms depending on
 * whether the app is enabled.
 */
public class LocationRecordingSwitch extends BroadcastReceiver {
  private static final String TAG = "LocationRecordingSwitch";
  private static final Long ONE_MINITUE_IN_MILLIS = 60000L;

  /**
   * Start broadcasting alarms periodically.
   */
  static public void startRecording(Context context, int intervalMins) {
    Intent intent = new Intent(context, LocationSampler.class);
    PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

    AlarmManager alarmManager = (AlarmManager) context
        .getSystemService(Context.ALARM_SERVICE);
    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
        SystemClock.elapsedRealtime(),
        ONE_MINITUE_IN_MILLIS * intervalMins,
        sender);
    Toast.makeText(context, R.string.repeating_scheduled, Toast.LENGTH_SHORT)
        .show();
  }

  /**
   * Stop the repeating alarms.
   */
  static public void stopRecording(Context context) {
    Intent intent = new Intent(context, LocationSampler.class);
    PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

    AlarmManager alarmManager = (AlarmManager) context
        .getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(sender);

    Toast.makeText(context, R.string.repeating_unscheduled, Toast.LENGTH_SHORT)
        .show();
  }

  /**
   * Upon rebooting the phone, maintain the alarm state as before the shutdown.
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    SharedPreferences sharedPreferences = context
        .getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
    boolean enabled = sharedPreferences.getBoolean(Constants.ENABLED, false);
    if (enabled) {
      int intervalMins = sharedPreferences
          .getInt(Constants.INTERVAL_MINS, Constants.DEFAULT_INTERVAL_MINS);
      startRecording(context, intervalMins);
    }
  }
}
