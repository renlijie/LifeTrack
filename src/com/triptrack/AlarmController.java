package com.triptrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * Controller controls the broadcast of alarms depending on whether the app is
 * enabled. When the phone reboots, start the alarm automatically if it was on
 * before the reboot.
 * @author Lijie Ren
 * 
 */
public class AlarmController extends BroadcastReceiver {
    private static final String TAG = "AlarmController";

    /**
     * When the phone reboots, start the alarm automatically if it was on before
     * the reboot.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final SharedPreferences settings =
            context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);

        final boolean enabled = settings.getBoolean(Constants.ENABLED, false);
        if (enabled) {
            final int intervalMins =
                settings.getInt(Constants.INTERVAL_MINS,
                    Constants.DEFAULT_INTERVAL_MINS);
            startAlarm(context, intervalMins);
        }
    }

    /**
     * Start broadcasting alarms periodically. Notify the user about this.
     * 
     * @param context
     * @param intervalMins
     */
    static public void startAlarm(Context context, int intervalMins) {
        final Intent intent = new Intent(context, RepeatingAlarm.class);
        final PendingIntent sender =
            PendingIntent.getBroadcast(context, 0, intent, 0);

        final AlarmManager am =
            (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock
            .elapsedRealtime(), 60000L * intervalMins, sender);

        Toast.makeText(context, R.string.repeating_scheduled,
            Toast.LENGTH_SHORT).show();
    }

    /**
     * Stop the repeating alarm and notify the user about this.
     * 
     * @param context
     */
    static public void stopAlarm(Context context) {
        final Intent intent = new Intent(context, RepeatingAlarm.class);
        final PendingIntent sender =
            PendingIntent.getBroadcast(context, 0, intent, 0);

        final AlarmManager am =
            (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);

        Toast.makeText(context, R.string.repeating_unscheduled,
            Toast.LENGTH_SHORT).show();
    }
}
