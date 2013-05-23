package com.triptrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

class FixOverlay extends ItemizedOverlay<OverlayItem> {
    private static final String TAG = "FixOverlay";
    private static final int MAX_SIZE = 500;
    private static final int MIN_DIST = 5;

    private final Cursor c;

    private ArrayList<Element> points = new ArrayList<Element>();
    private HistoryMapActivity map;

    private boolean drawMarkers;

    private double maxLat = -90;
    private double minLat = 90;
    private double cenLat = 0;
    private double latSpan = 0;
    private double rightLng = 0;
    private double leftLng = 0;
    private double cenLng = 0;
    private double lngSpan = 0;

    class Element extends OverlayItem {
        public
        long utc;
        double lat;
        double lng;
        float acc;
        int green;

        Element(long utc, double lat, double lng, float acc, int green) {
            super(new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6)), "", "");
            this.utc = utc;
            this.lat = lat;
            this.lng = lng;
            this.acc = acc;
            this.green = green;
        }
    }

    public FixOverlay(Cursor rows, HistoryMapActivity map, boolean drawMarkers) {
        super(boundCenterBottom(map.getResources().getDrawable(R.drawable.marker)));
        this.drawMarkers = drawMarkers;
        this.map = map;
        c = rows;

        if (!c.moveToLast()) {
            populate();
            return;
        }

        ArrayList<Double> lngList = new ArrayList<Double>();
        int size = c.getCount();

        int index = 0;
        int green;
        double preLat = 0, preLng = 0;
        float[] results = new float[1];
        boolean firstPoint = true;

        while (true) {
            double lat = c.getDouble(c.getColumnIndex(Constants.KEY_LAT));
            double lng = c.getDouble(c.getColumnIndex(Constants.KEY_LNG));
            float acc = c.getFloat(c.getColumnIndex(Constants.KEY_ACC));
            long utc = c.getLong(c.getColumnIndex(Constants.KEY_UTC));

            if (firstPoint) {
                firstPoint = false;
            } else {
                Location.distanceBetween(lat, lng, preLat, preLng, results);
                if (results[0] <= acc) {
                    // Is a valid fix, but not drawing.
                    index++;
                    if (c.isFirst()) {
                        break;
                    }
                    c.moveToPrevious();
                    continue;
                }
            }
            preLat = lat;
            preLng = lng;

            if (lat > maxLat)
                maxLat = lat;
            if (lat < minLat)
                minLat = lat;

            green = (int) (255 * (double) index++ / size);
            points.add(new Element(utc, lat, lng, acc, green));
            lngList.add(lng);

            if (c.isFirst()) {
                break;
            }
            c.moveToPrevious();
        }

        Double[] lngs = lngList.toArray(new Double[lngList.size()]);
        Arrays.sort(lngs);

        int idx = 0;
        double diff = lngs[lngs.length - 1] - lngs[0];
        if (diff > 180)
            diff = 360 - diff;
        for (int i = 1; i < lngs.length; i++) {
            double d = lngs[i] - lngs[i - 1];
            if (d > 180)
                d = 360 - d;
            if (diff < d) {
                diff = d;
                idx = i;
            }
        }
        leftLng = lngs[idx];
        if (idx == 0) {
            rightLng = lngs[lngs.length - 1];
            cenLng = (rightLng + leftLng) / 2;
            lngSpan = rightLng - leftLng;
        } else {
            rightLng = lngs[idx - 1];
            cenLng = (leftLng + rightLng) / 2 - 180;
            lngSpan = (rightLng + 180) + (180 - leftLng);
        }

        cenLat = (minLat + maxLat) / 2;
        latSpan = maxLat - minLat;
        populate();
    }

    public int numFarAwayFixes() {
        return points.size();
    }

    public void close() {
        c.close();
    }

    @Override
    public int size() {
        if (drawMarkers) {
            int size = points.size();
            if (size <= MAX_SIZE)
                return size;
            else {
                drawMarkers = false;
                Toast.makeText(map, "Too many fixes (" + size
                        + ").\nWill not draw markers.", Toast.LENGTH_SHORT).show();
                return 0;
            }
        }
        else
            return 0;
    }

    public double getLatSpan() {
        return latSpan;
    }

    public double getLngSpan() {
        return lngSpan;
    }

    public double getCenLng() {
        return cenLng;
    }

    public double getCenLat() {
        return cenLat;
    }

    @Override
    public void draw(Canvas canvas, MapView mapv, boolean shadow) {
        if (shadow) {
            return;
        }
        super.draw(canvas, mapv, false);

        if (points == null) {
            return;
        }

        Path path = new Path();
        boolean firstPoint = true;
        Paint mPaint = new Paint();
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(4);

        int preX = 0, preY = 0;
        boolean farAway = true;
        double distance;
        for (Element point : points) {
            GeoPoint gFix = point.getPoint();

            Point pFix = new Point();
            Projection projection = mapv.getProjection();
            projection.toPixels(gFix, pFix);

            if (firstPoint) {
                path.moveTo(pFix.x, pFix.y);
                firstPoint = false;
                preX = pFix.x;
                preY = pFix.y;
            } else {
                distance = Math.sqrt(Math.pow((preX - pFix.x), 2)
                        + Math.pow((preY - pFix.y), 2));
                if (distance > MIN_DIST) {
                    farAway = true;
                    path.lineTo(pFix.x, pFix.y);
                    preX = pFix.x;
                    preY = pFix.y;
                } else {
                    farAway = false;
                }
            }

            if (farAway) {
                int rad = (int) (projection.metersToEquatorPixels(point.acc)
                        * (1 / Math.cos(Math.toRadians(point.lat))));
                mPaint.setARGB(128, 255 - point.green, point.green, 0);
                canvas.drawCircle(pFix.x, pFix.y, rad, mPaint);
            }
        }

        mPaint.setARGB(128, 0, 0, 255);
        canvas.drawPath(path, mPaint);
    }

    @Override
    protected OverlayItem createItem(int i) {
        return points.get(i);
    }

    @Override
    protected boolean onTap(int index) {
        final Element item = points.get(index);
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(item.utc);
        new AlertDialog.Builder(map)
                .setTitle(CalendarHelper.prettyDate(c) + " "
                        + String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":"
                        + String.format("%02d", c.get(Calendar.MINUTE)) + ":"
                        + String.format("%02d", c.get(Calendar.SECOND)))
                .setMessage("Press BACK to cancel. \n\n"
                        + "(Latitude, Longitude), Accuracy\n(" + String.format("%1$,.5f", item.lat)
                        + ", " + String.format("%1$,.5f", item.lng) + "), "
                        + String.format("%1$,.3f", item.acc))
                .setPositiveButton("Delete this fix", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AlertDialog.Builder(map)
                                .setTitle("Confirm").setMessage("DELETE?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        FixDataStore fixDataStore = new FixDataStore(map);
                                        fixDataStore.open();
                                        fixDataStore.deleteSingle(item.utc);
                                        fixDataStore.close();
                                        map.prepareDates(0, true);
                                        Toast.makeText(map, "deleted!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                })
                                .show();
                    }
                })
                .setNegativeButton("DELETE THIS DAY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AlertDialog.Builder(map)
                                .setTitle("Confirm").setMessage("DELETE?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FixDataStore fixDataStore = new FixDataStore(map);
                                        fixDataStore.open();
                                        fixDataStore.deleteDay(item.utc);
                                        fixDataStore.close();
                                        map.prepareDates(0, true);
                                        Toast.makeText(map, "deleted!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                })
                                .show();
                    }
                })
                .show();
        return true;
    }
}
