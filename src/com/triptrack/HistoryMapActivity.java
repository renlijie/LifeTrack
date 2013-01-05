package com.triptrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.CalendarView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import java.util.Calendar;
import java.util.List;

public class HistoryMapActivity extends MapActivity {
    private static final String TAG = "HistoryMap";
    private static final String[] strDays = new String[]{"Sun.", "Mon.", "Tue.", "Wed.", "Thu.", "Fri.", "Sat."};

    private MapView mapView;
    private Button dateSettingsButton;
    private Button previousDayButton;
    private Button nextDayButton;
    private Button allDaysButton;
    private Button startDayButton;
    private Button endDayButton;
    private Button settingsButton;
    private Button backButton;
    private CalendarView calendarView;
    private List<Overlay> mapOverlays;
    private Calendar calendar;
    private boolean allDays;

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && (calendarView.getVisibility() == View.VISIBLE)) {
            calendarView.setVisibility(View.GONE);
            mapView.setVisibility(View.VISIBLE);
            startDayButton.setVisibility(View.GONE);
            endDayButton.setVisibility(View.GONE);
            allDaysButton.setVisibility(View.GONE);
            buttonsFade();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    static String printDate(Calendar calendar) {
        if (calendar == null) {
            return "ALL DAYS";
        }

        return calendar.get(Calendar.YEAR) + "/"
                + String.format("%02d", (calendar.get(Calendar.MONTH) + 1)) + "/"
                + String.format("%02d", calendar.get(Calendar.DATE)) + ", "
                + strDays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    }

    void prepareRows(int direction, final boolean firstTime, boolean move) {
        FixDataStore fixDataStore = new FixDataStore(this);
        fixDataStore.open();

        if (direction == 1) {
            Calendar cal;
            cal = fixDataStore.nextRecordDay(calendar);
            if (cal != null) {
                calendar = cal;
            }
        } else if (direction == -1) {
            Calendar cal;
            cal = fixDataStore.previousRecordDay(calendar);
            if (cal != null) {
                calendar = cal;
            }
        }

        Cursor c;
        if (allDays) {
            c = fixDataStore.fetchFixes(null);
        } else {
            c = fixDataStore.fetchFixes(calendar);
        }

        if (!firstTime) {
            FixOverlay fixOverlay = (FixOverlay) (mapOverlays.get(mapOverlays.size() - 1));
            fixOverlay.close(); // only close once
            mapOverlays.remove(mapOverlays.size() - 1);
        }
        FixOverlay f;
        if (allDays) {
            f = new FixOverlay(this, c, null, this);
            mapOverlays.add(f);
        } else {
            f = new FixOverlay(this, c, calendar, this);
            mapOverlays.add(f);
        }

        if (allDays) {
            dateSettingsButton.setText(printDate(null) + "\n" + f.numFarAwayFixes() + " out of "
                    + c.getCount() + " fixes.");
        } else {
            dateSettingsButton.setText(printDate(calendar) + "\n" + f.numFarAwayFixes() + " out of "
                    + c.getCount() + " fixes.");
        }
        fixDataStore.close();

        if (move) {
            double latSpanE6 = f.getLatSpan() * 1E6;
            double lngSpanE6 = f.getLngSpan() * 1E6;
            double cenLatE6 = f.getCenLat() * 1E6;
            double cenLngE6 = f.getCenLng() * 1E6;
            mapView.getController().zoomToSpan((int) (latSpanE6 * 1.5), (int) (lngSpanE6 * 1.1));
            GeoPoint gFix = new GeoPoint((int) (cenLatE6), (int) (cenLngE6));
            mapView.getController().animateTo(gFix);
        }
    }

    private void buttonsFade() {
        Animation buttonFadeOut = new AlphaAnimation(1.0f, 0.0f);

        buttonFadeOut.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation a) {
                previousDayButton.setVisibility(View.INVISIBLE);
                nextDayButton.setVisibility(View.INVISIBLE);
                dateSettingsButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation a) {
            }

            @Override
            public void onAnimationStart(Animation a) {
            }
        });

        buttonFadeOut.setStartOffset(2000);
        buttonFadeOut.setDuration(1000);

        previousDayButton.setAnimation(buttonFadeOut);
        nextDayButton.setAnimation(buttonFadeOut);
        dateSettingsButton.setAnimation(buttonFadeOut);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        mapView = (MapView) findViewById(R.id.mapview);

        previousDayButton = (Button) findViewById(R.id.previous_day);
        nextDayButton = (Button) findViewById(R.id.next_day);
        allDaysButton = (Button) findViewById(R.id.all_days);
        dateSettingsButton = (Button) findViewById(R.id.date_settings);
        startDayButton = (Button) findViewById(R.id.start_date);
        settingsButton = (Button) findViewById(R.id.settings);
        endDayButton = (Button) findViewById(R.id.end_date);
        backButton = (Button) findViewById(R.id.back);
        calendarView = (CalendarView) findViewById(R.id.calendar);

        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                HistoryMapActivity.this.startActivity(new Intent(HistoryMapActivity.this, ControlPanelActivity.class));
            }
        });

        dateSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                previousDayButton.clearAnimation();
                nextDayButton.clearAnimation();
                dateSettingsButton.clearAnimation();
                calendarView.setVisibility(View.VISIBLE);
                allDaysButton.setVisibility(View.VISIBLE);
                startDayButton.setVisibility(View.VISIBLE);
                endDayButton.setVisibility(View.VISIBLE);
                mapView.setVisibility(View.INVISIBLE);
                settingsButton.setVisibility(View.GONE);
                backButton.setVisibility(View.VISIBLE);
            }
        });

        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                calendarView.setVisibility(View.GONE);
                allDaysButton.setVisibility(View.GONE);
                startDayButton.setVisibility(View.GONE);
                endDayButton.setVisibility(View.GONE);
                mapView.setVisibility(View.VISIBLE);
                backButton.setVisibility(View.GONE);
                settingsButton.setVisibility(View.VISIBLE);
                buttonsFade();            }
        });

        mapOverlays = mapView.getOverlays();
        mapOverlays.add(new Overlay() {
            @Override
            public void draw(Canvas c, MapView mapView, boolean shadow) {
            }

            @Override
            public boolean onTap(GeoPoint p, MapView mapView) {
                buttonsFade();
                return false;
            }
        });

        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        allDays = false;
        prepareRows(0, true, true);

        previousDayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                allDays = false;
                prepareRows(-1, false, true);
                buttonsFade();
            }
        });

        nextDayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                allDays = false;
                prepareRows(1, false, true);
                buttonsFade();
            }
        });

        allDaysButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                allDays = true;
                calendarView.setVisibility(View.GONE);
                mapView.setVisibility(View.VISIBLE);
                startDayButton.setVisibility(View.GONE);
                endDayButton.setVisibility(View.GONE);
                allDaysButton.setVisibility(View.GONE);
                prepareRows(0, false, true);
                buttonsFade();
            }
        });

        buttonsFade();
    }
}
