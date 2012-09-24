package com.triptrack;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FixDataStore {
    private static final String TAG = "FixDataStore";

    private static final String DATABASE_NAME = "triptrack";
    private static final String DATABASE_TABLE = "fixes";
    private static final int DATABASE_VERSION = 4;

    private final Context mCtx;

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_CREATE = "create table " +
        DATABASE_TABLE + " (" + Constants.KEY_UTC + " long primary key, " +
        Constants.KEY_LAT + " double, " + Constants.KEY_LNG + " double, " +
        Constants.KEY_ACC + " single);";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void
            onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(Constants.TAG + ":" + TAG,
                "Upgrading database from version " + oldVersion + " to " +
                    newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    public FixDataStore(Context ctx) {
        this.mCtx = ctx;
    }

    public FixDataStore open() {
        mDbHelper = new DatabaseHelper(mCtx);
        int waitMillis = 100;
        while (true) {
            try {
                mDb = mDbHelper.getWritableDatabase();
                break;
            } catch (SQLException e) {
                Log.w(Constants.TAG + ":" + TAG,
                    "Database not available. Wait for " +
                        Integer.toString(waitMillis) + " ms.");
            }
            try {
                Thread.sleep(waitMillis);
                if (waitMillis < 5000) {
                    waitMillis *= 2;
                }
            } catch (InterruptedException e) {
            }
        }
        // Log.d(Constants.TAG + ":" + TAG, "Database opened successfully.");
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public void exportToFile(File file, Handler mHandler) {
        Cursor c = fetchFixes(0);
        if (c.moveToFirst()) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                fos.write((c.getCount() + "\n").getBytes());
            } catch (IOException e) {
                mHandler.sendMessage(Message.obtain(mHandler,
                    Constants.HANDLER_TOAST, 0, 0, "Opening " + file +
                        " failed. Do you have access?"));
                Log.w(Constants.TAG + ":" + TAG, "Opening " + file +
                    " failed.");
                return;
            }

            int size = 0;
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_PROGRESSBAR_SETMAX, c.getCount(), 0, null));
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_PROGRESSBAR_SHOW, 0, 0, null));

            while (true) {
                String r =
                    Long.toString(c.getLong((c
                        .getColumnIndex(Constants.KEY_UTC)))) +
                        "," +
                        Double.toString(c.getDouble(c
                            .getColumnIndex(Constants.KEY_LAT))) +
                        "," +
                        Double.toString(c.getDouble(c
                            .getColumnIndex(Constants.KEY_LNG))) +
                        "," +
                        Float.toString(c.getFloat(c
                            .getColumnIndex(Constants.KEY_ACC))) + "\n";
                try {
                    fos.write(r.getBytes());
                } catch (IOException e) {
                    mHandler.sendMessage(Message.obtain(mHandler,
                        Constants.HANDLER_TOAST, 0, 0, "Writing to " + file +
                            " failed."));
                    Log.w(Constants.TAG + ":" + TAG, "Writing " + r +
                        " failed.");
                }
                mHandler
                    .sendMessage(Message.obtain(mHandler,
                        Constants.HANDLER_PROGRESSBAR_SETPROGRESS, size++, 0,
                        null));
                if (c.isLast()) {
                    break;
                }
                c.moveToNext();
            }

            try {
                fos.close();
            } catch (IOException e) {
                mHandler.sendMessage(Message.obtain(mHandler,
                    Constants.HANDLER_TOAST, 0, 0, "Closing " + file +
                        " failed."));
                Log.w(Constants.TAG + ":" + TAG, "Closing " + file +
                    " failed.");
            }
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_TOAST, 0, 0,
                    mCtx.getString(R.string.export_finished)));
            Log.d(Constants.TAG + ":" + TAG,
                mCtx.getString(R.string.export_finished));
        } else {
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_TOAST, 0, 0,
                    mCtx.getString(R.string.export_nothing)));
        }
        c.close();
    }

    public void importFromFile(File file, Handler mHandler) {
        // A flag to show if any error happened during importing.
        boolean noError = true;

        // Open a reader.
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (IOException e) {
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_TOAST, 0, 0, "Opening " + file +
                    " failed. Does it exist?"));
            Log.w(Constants.TAG + ":" + TAG, "Opening " + file + " failed.");
            return;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(
            new DataInputStream(fis)));

        // Read the first line as the total number of fixes.
        String str;
        int size;
        try {
            str = br.readLine();
        } catch (IOException e) {
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_TOAST, 0, 0, "Reading " + file + " failed."));
            Log.w(Constants.TAG + ":" + TAG, "Reading " + file + " failed.");
            return;
        }
        try {
            size = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            // If the first line cannot be parsed as an int, create a new reader
            // to read from the beginning of the file, in case the first line is
            // actually a fix.
            try {
                br.close();
                fis = new FileInputStream(file);
            } catch (IOException ex) {
                mHandler.sendMessage(Message.obtain(mHandler,
                    Constants.HANDLER_TOAST, 0, 0, "Opening " + file +
                        " failed. Does it exist?"));
                Log.w(Constants.TAG + ":" + TAG, "Opening " + file +
                    " failed.");
                return;
            }
            br = new BufferedReader(new InputStreamReader(new DataInputStream(
                    fis)));

            // Assign a fake size.
            size = Integer.MAX_VALUE;

            // Notify the user and set the flag. Continue.
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_TOAST, 0, 0,
                "Reading size failed. But continue."));
            Log.w(Constants.TAG + ":" + TAG, "Reading size failed." + str);
            noError = false;
        }

        // Show the progress bar.
        mHandler.sendMessage(Message.obtain(mHandler,
            Constants.HANDLER_PROGRESSBAR_SETMAX, size, 0,
            null));
        mHandler.sendMessage(Message.obtain(mHandler,
            Constants.HANDLER_PROGRESSBAR_SHOW, 0, 0, null));

        long utc;
        double lat, lng;
        float acc;

        // The number of fix currently being imported.
        size = 0;

        while (true) {
            size++;

            // Read the current line. Should contain a fix.
            try {
                str = br.readLine();
            } catch (IOException e) {
                mHandler.sendMessage(Message.obtain(mHandler,
                    Constants.HANDLER_TOAST, 0, 0, "Reading " + file +
                        " failed."));
                Log.w(Constants.TAG + ":" + TAG, "Reading " + file +
                    " failed.");
                return;
            }
            if (str == null) {
                break;
            }

            // Parse the string as a fix.
            String[] entry = str.split(",");
            if (entry.length < 4) {
                Log.e(Constants.TAG + ":" + TAG, str + " is not valid data!");
                noError = false;
                continue;
            }

            // Validate the data.
            try {
                utc = Long.parseLong(entry[0]);
                lat = Double.parseDouble(entry[1]);
                lng = Double.parseDouble(entry[2]);
                acc = Float.parseFloat(entry[3]);
            } catch (NumberFormatException e) {
                noError = false;
                Log.w(Constants.TAG + ":" + TAG, "Reading fix #" + size +
                    " failed.");
                continue;
            }
            if (utc < 0 || lat < -90 || lat > 90 || lng > 180 || lng < -180 ||
                acc < 0) {
                noError = false;
                Log.w(Constants.TAG + ":" + TAG, "Not valid: fix #" + size +
                    ".");
                continue;
            }

            // Add to database. If the fix is already in the database, an
            // SQLException will occur. Catch it.
            try {
                createFix(utc, lat, lng, acc);
                mHandler.sendMessage(Message.obtain(mHandler,
                    Constants.HANDLER_PROGRESSBAR_SETPROGRESS, size, 0,
                    null));
            } catch (SQLException e) {
                noError = false;
                Log.w(Constants.TAG + ":" + TAG, "SQLException at fix #" + size
                    +".");
            }
        }

        // Notify the user if error has occurred during the process.
        if (noError) {
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_TOAST, 0, 0,
                mCtx.getString(R.string.import_finished)));
            Log.d(Constants.TAG + ":" + TAG,
                mCtx.getString(R.string.import_finished));
        } else {
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_TOAST, 0, 0,
                mCtx.getString(R.string.import_finished_with_error)));
            Log.d(Constants.TAG + ":" + TAG,
                mCtx.getString(R.string.import_finished_with_error));
        }

        // Close the reader.
        try {
            br.close();
        } catch (IOException e) {
            mHandler.sendMessage(Message.obtain(mHandler,
                Constants.HANDLER_TOAST, 0, 0, "Closing " + file + " failed."));
            Log.w(Constants.TAG + ":" + TAG, "Closing " + file + " failed.");
        }
    }

    public void clearHistory() {
        mDb.execSQL("DELETE FROM " + DATABASE_TABLE);
    }

    public void delete(long utc) {
        mDb.delete(DATABASE_TABLE,
            Constants.KEY_UTC + "=" + Long.toString(utc), null);
    }

    public void deleteDay(long utc) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(utc);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Cursor c = fetchFixes(cal);
        c.moveToFirst();
        do {
            delete(c.getLong(c.getColumnIndex(Constants.KEY_UTC)));
        } while (c.moveToNext());
        c.close();
    }

    // TODO: encrypt
    public long createFix(Location location) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(Constants.KEY_UTC, location.getTime());
        initialValues.put(Constants.KEY_LAT, location.getLatitude());
        initialValues.put(Constants.KEY_LNG, location.getLongitude());
        initialValues.put(Constants.KEY_ACC, location.getAccuracy());
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     *
     * @param time
     * @param latitude
     * @param longitude
     * @param accuracy
     * @return
     * @throws SQLException when utc already exists. Caught in import().
     */
    public long createFix(long time, double latitude, double longitude,
        float accuracy) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(Constants.KEY_UTC, time);
        initialValues.put(Constants.KEY_LAT, latitude);
        initialValues.put(Constants.KEY_LNG, longitude);
        initialValues.put(Constants.KEY_ACC, accuracy);
        return mDb.insertOrThrow(DATABASE_TABLE, null, initialValues);
    }

    // TODO: decrypt
    public Cursor fetchFixes(int size) {
        if (size <= 0) {
            return mDb.query(DATABASE_TABLE, new String[] { Constants.KEY_UTC,
                Constants.KEY_LAT, Constants.KEY_LNG, Constants.KEY_ACC },
                null, null, null, null, Constants.KEY_UTC + " DESC", null);
        }
        return mDb.query(DATABASE_TABLE, new String[] { Constants.KEY_UTC,
            Constants.KEY_LAT, Constants.KEY_LNG, Constants.KEY_ACC }, null,
            null, null, null, Constants.KEY_UTC + " DESC", Integer
                .toString(size));
    }

    /**
     *  Return all fixes within 24 hours of the second that calendar points to.
     * @param calendar
     * @return
     */
    public Cursor fetchFixes(Calendar calendar) {
        if (calendar == null) {
            return mDb.query(DATABASE_TABLE, new String[] { Constants.KEY_UTC,
                Constants.KEY_LAT, Constants.KEY_LNG, Constants.KEY_ACC },
                null, null, null, null, Constants.KEY_UTC + " DESC", null);
        }

        long startMillis = calendar.getTimeInMillis();
        long endMillis = startMillis + 24L * 3600 * 1000 - 1;

        return mDb.query(DATABASE_TABLE, new String[] { Constants.KEY_UTC,
            Constants.KEY_LAT, Constants.KEY_LNG, Constants.KEY_ACC },
            Constants.KEY_UTC + " BETWEEN ? AND ?", new String[] {
                Long.toString(startMillis), Long.toString(endMillis) }, null,
            null, Constants.KEY_UTC + " DESC", null);
    }

    public Calendar previousRecordDay(Calendar calendar) {
        Cursor c =
            mDb.query(DATABASE_TABLE, new String[] { Constants.KEY_UTC,
                Constants.KEY_LAT, Constants.KEY_LNG, Constants.KEY_ACC },
                Constants.KEY_UTC + " < ?", new String[] { Long
                    .toString(calendar.getTimeInMillis()) }, null, null,
                Constants.KEY_UTC + " DESC", "1");
        if (c.getCount() == 0) {
            c.close();
            return null;
        } else {
            c.moveToFirst();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(c.getLong(c.getColumnIndex(Constants.KEY_UTC)));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            c.close();
            return cal;
        }
    }

    public Calendar nextRecordDay(Calendar calendar) {
        Cursor c =
            mDb.query(
                DATABASE_TABLE,
                new String[] { Constants.KEY_UTC, Constants.KEY_LAT,
                    Constants.KEY_LNG, Constants.KEY_ACC },
                Constants.KEY_UTC + " >= ?",
                new String[] { Long
                    .toString(calendar.getTimeInMillis() + 24L * 3600 * 1000) },
                null, null, Constants.KEY_UTC + " ASC", "1");
        if (c.getCount() == 0) {
            c.close();
            return null;
        } else {
            c.moveToFirst();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(c.getLong(c.getColumnIndex(Constants.KEY_UTC)));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            c.close();
            return cal;
        }
    }
}
