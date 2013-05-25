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
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.squareup.timessquare.CalendarPickerView;
import com.squareup.timessquare.CalendarPickerView.OnDateSelectedListener;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Activity the user sees when opening the app.
 * @author Lijie Ren
 *
 */

public class HistoryMapActivity extends MapActivity {
    private static final String TAG = "HistoryMap";

    // Map Panel
    private MapView mapView;
    private Button dateSettingsButton;
    private Button settingsButton;
    private Button previousDayButton;
    private Button nextDayButton;

    // Calendar Panel
    private CalendarPickerView calendarView;
    private ToggleButton markersButton;
    private Button unboundedDateButton;
    private Button drawButton;
    private ToggleButton firstDayButton;
    private ToggleButton lastDayButton;

    // Internal variables
    private FixDataStore fixDataStore = new FixDataStore(this);
    private List<Overlay> mapOverlays;
    private Span span = new Span();

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    // Override BACK key's behavior.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && (calendarView.getVisibility() == View.VISIBLE)) {
            showMapPanel();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        fixDataStore.close();
        super.onDestroy();
    }

    void prepareDates(int direction, boolean drawMarkers) {
        if (direction == 1) {
            fixDataStore.plusOneDay(span);
        } else if (direction == -1) {
            fixDataStore.minusOneDay(span);
        }
        drawFixes(span, drawMarkers);
    }

    void drawFixes(Calendar firstDay, Calendar lastDay, boolean drawMarkers) {
        Cursor c = fixDataStore.fetchFixes(firstDay, lastDay);
        FixOverlay fixOverlay = (FixOverlay) (mapOverlays.get(mapOverlays.size() - 1));
        if (fixOverlay != null)
            fixOverlay.close();
        mapOverlays.remove(mapOverlays.size() - 1);
        FixOverlay f = new FixOverlay(c, this, drawMarkers);
        mapOverlays.add(f);
        dateSettingsButton.setText(CalendarHelper.prettyInterval(firstDay, lastDay)
                + "\n" + f.numFarAwayFixes() + " out of "
                + c.getCount());
        zoomToFit(f);
    }

    void drawFixes(Span span, boolean drawMarkers) {
        drawFixes(span.getStartDay(), span.getEndDay(), drawMarkers);
    }

    void zoomToFit(FixOverlay f) {
        double latSpanE6 = f.getLatSpan() * 1E6;
        double lngSpanE6 = f.getLngSpan() * 1E6;
        double cenLatE6 = f.getCenLat() * 1E6;
        double cenLngE6 = f.getCenLng() * 1E6;
        mapView.getController().zoomToSpan((int) (latSpanE6 * 1.5), (int) (lngSpanE6 * 1.1));
        GeoPoint gFix = new GeoPoint((int) (cenLatE6), (int) (cenLngE6));
        mapView.getController().animateTo(gFix);
    }

    private void buttonsFade() {
        Animation buttonFadeOut = new AlphaAnimation(1.0f, 0.0f);

        buttonFadeOut.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation a) {
                settingsButton.setVisibility(View.INVISIBLE);
                previousDayButton.setVisibility(View.INVISIBLE);
                nextDayButton.setVisibility(View.INVISIBLE);
                markersButton.setVisibility(View.INVISIBLE);
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
        buttonFadeOut.setDuration(2000);

        settingsButton.setAnimation(buttonFadeOut);
        previousDayButton.setAnimation(buttonFadeOut);
        nextDayButton.setAnimation(buttonFadeOut);
        markersButton.setAnimation(buttonFadeOut);
        dateSettingsButton.setAnimation(buttonFadeOut);
    }

    private void showMapPanel() {
        calendarView.setVisibility(View.GONE);
        markersButton.setVisibility(View.GONE);
        unboundedDateButton.setVisibility(View.GONE);
        drawButton.setVisibility(View.GONE);
        firstDayButton.setVisibility(View.GONE);
        lastDayButton.setVisibility(View.GONE);

        mapView.setVisibility(View.VISIBLE);
        buttonsFade();
    }

    private void showCalendarPanel() {
        dateSettingsButton.clearAnimation();
        settingsButton.clearAnimation();
        previousDayButton.clearAnimation();
        nextDayButton.clearAnimation();
        markersButton.clearAnimation();

        mapView.setVisibility(View.INVISIBLE);
        settingsButton.setVisibility(View.GONE);

        calendarView.setVisibility(View.VISIBLE);
        unboundedDateButton.setVisibility(View.VISIBLE);
        drawButton.setVisibility(View.VISIBLE);
        firstDayButton.setVisibility(View.VISIBLE);
        lastDayButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.triptrack.R.layout.history_map_activity);

        fixDataStore.open();

        mapView = (MapView) findViewById(R.id.mapview);
        dateSettingsButton = (Button) findViewById(R.id.date_settings);
        settingsButton = (Button) findViewById(R.id.settings);
        previousDayButton = (Button) findViewById(R.id.previous_day);
        nextDayButton = (Button) findViewById(R.id.next_day);
        markersButton = (ToggleButton) findViewById(R.id.draw_markers);

        calendarView = (CalendarPickerView) findViewById(R.id.calendar);
        calendarView.init(fixDataStore.earliestRecordDay().getTime(), new Date());

        markersButton.setChecked(false);
        unboundedDateButton = (Button) findViewById(R.id.unbounded_date);
        drawButton = (Button) findViewById(R.id.draw);
        firstDayButton = (ToggleButton) findViewById(R.id.first_day);
        firstDayButton.setChecked(true);
        lastDayButton = (ToggleButton) findViewById(R.id.last_day);
        lastDayButton.setChecked(false);

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
        // Add a dummy overlay for removal later.
        mapOverlays.add(null);

        prepareDates(0, markersButton.isChecked());

        buttonsFade();

        dateSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showCalendarPanel();
            }
        });

        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                HistoryMapActivity.this.startActivity(
                        new Intent(HistoryMapActivity.this, ControlPanelActivity.class));
            }
        });

        markersButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareDates(0, markersButton.isChecked());
                buttonsFade();
            }
        });

        previousDayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareDates(-1, markersButton.isChecked());
                buttonsFade();
            }
        });

        nextDayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareDates(1, markersButton.isChecked());
                buttonsFade();
            }
        });


        calendarView.setOnDateSelectedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(Date date) {
                if (firstDayButton.isChecked()) {
                    span.setStartDay(date);
                    lastDayButton.setChecked(true);
                    firstDayButton.setChecked(false);
                    Toast.makeText(HistoryMapActivity.this,
                            CalendarHelper.prettyInterval(span), Toast.LENGTH_SHORT).show();
                } else {
                    span.setEndDay(date);
                    firstDayButton.setChecked(true);
                    lastDayButton.setChecked(false);
                    Toast.makeText(HistoryMapActivity.this,
                            CalendarHelper.prettyInterval(span), Toast.LENGTH_SHORT).show();
                }
            }
        });

        drawButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showMapPanel();
                drawFixes(span, markersButton.isChecked());
            }
        });

        unboundedDateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firstDayButton.isChecked()) {
                    span.setStartDay(fixDataStore.earliestRecordDay());
                    lastDayButton.setChecked(true);
                    firstDayButton.setChecked(false);
                    Toast.makeText(HistoryMapActivity.this,
                            CalendarHelper.prettyInterval(span), Toast.LENGTH_SHORT).show();
                }
                else {
                    span.setEndDay(Calendar.getInstance());
                    lastDayButton.setChecked(false);
                    firstDayButton.setChecked(true);
                    Toast.makeText(HistoryMapActivity.this,
                            CalendarHelper.prettyInterval(span), Toast.LENGTH_SHORT).show();
                }
            }
        });

        firstDayButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                lastDayButton.setChecked(!firstDayButton.isChecked());
            }
        });

        lastDayButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                firstDayButton.setChecked(!lastDayButton.isChecked());
            }
        });
    }
}
