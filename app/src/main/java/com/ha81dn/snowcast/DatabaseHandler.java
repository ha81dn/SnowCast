package com.ha81dn.snowcast;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static DatabaseHandler mInstance = null;

    private DatabaseHandler(Context context) {
        super(context, "maindb", null, 1);
    }

    public static DatabaseHandler getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) mInstance = new DatabaseHandler(context.getApplicationContext());
        return mInstance;
    }

    public static void store(SQLiteDatabase db, String location, String time, String sky, String city, String day, String temp, String precip, String windname, String winddir, String stamp) {
        Cursor c;
        ContentValues vals = new ContentValues();
        boolean update = false;

        c = db.rawQuery("select count(*) from forecast where location = ? and time = ?", new String[]{location, time});
        if (c != null) {
            if (c.moveToFirst()) update = c.getInt(0) == 1;
            c.close();
        }

        vals.put("location", location);
        vals.put("time", time);
        vals.put("city", city);
        vals.put("day", day);
        vals.put("sky", sky);
        vals.put("temp", temp);
        vals.put("precip", precip);
        vals.put("windname", windname);
        vals.put("winddir", winddir);
        vals.put("stamp", stamp);
        if (update)
            db.update("forecast", vals, "location = ? and time = ?", new String[]{location, time});
        else
            db.insert("forecast", null, vals);
    }

    public static void discardObsolete(SQLiteDatabase db) {
        Cursor c;
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHH", Locale.getDefault());
        Calendar now = Calendar.getInstance();

        c = db.rawQuery("delete from forecast where time < ? or time > 99123123", new String[]{sdf.format(now.getTime())});
        if (c != null) {
            c.moveToFirst();
            c.close();
        }
    }

    public static void discardObsolete(SQLiteDatabase db, String location, String stamp) {
        Cursor c;

        c = db.rawQuery("delete from forecast where location = ? and stamp != ?", new String[]{location, stamp});
        if (c != null) {
            c.moveToFirst();
            c.close();
        }
    }

    public static SpannableStringBuilder retrieve(SQLiteDatabase db, SharedPreferences sharedPref, Context context) {
        Cursor c;
        SpannableStringBuilder list = new SpannableStringBuilder();
        boolean loopFlag, noSnow;
        int start;
        String location, day, tmp;
        char[] chars;

        for (int i = 1; i <= 5; i++) {
            location = sharedPref.getString("idx" + i, "");
            if (!location.equals("")) {
                c = db.rawQuery("select city, day, ltrim(substr(time, 7, 2), '0')+0, precip, temp, winddir from forecast where location = ? order by time", new String[]{location});
                if (c != null) {
                    if (c.moveToFirst()) {
                        if (list.length() != 0) list.append("\n");
                        start = list.length();
                        list.append(getZeroSpacedText(c.getString(0)));
                        list.setSpan(new ForegroundColorSpan(0xFFFFFF22), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        list.append(getZeroSpacedText(": "));
                        loopFlag = false;
                        noSnow = true;
                        day = "";
                        do {
                            try {
                                start = Math.round(Float.parseFloat(c.getString(3).replace("mm", "").replace(",", ".")) - 0.5f);
                                chars = new char[start <= 0 ? 0 : start - 1];
                                Arrays.fill(chars, '❄');
                            } catch (Exception ignore) {
                                chars = new char[0];
                                chars[0] = '?';
                            }
                            if (chars.length >= 1) {
                                noSnow = false;
                                if (loopFlag) list.append(getZeroSpacedText(", "));
                                tmp = c.getString(1);
                                if (!day.equals(tmp)) {
                                    day = tmp;
                                    start = list.length();
                                    list.append(getZeroSpacedText(day));
                                    list.setSpan(new ForegroundColorSpan(0xFFFFFFA0), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    list.append(getZeroSpacedText(" "));
                                }
                                list.append(getZeroSpacedText(c.getString(2) + "h "));
                                start = list.length();
                                list.append(getZeroSpacedText(new String(chars)));
                                list.setSpan(new ForegroundColorSpan(0xFFFFFFA0), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                if (c.getString(4).startsWith("-"))
                                    list.setSpan(new UnderlineSpan(), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                list.append(getZeroSpacedText(" "));
                                list.append(getZeroSpacedText(c.getString(5)));
                                loopFlag = true;
                            }
                        } while (c.moveToNext());
                        if (noSnow)
                            list.append(getZeroSpacedText(context.getString(R.string.no_snow_expected)));
                    }
                    c.close();
                }
            }
        }

        //sky || ' in ' || city || ' am ' || day || ' ab ' || ltrim(substr(time, 7, 2), '0') || ' Uhr mit ' || precip || ' bei ' || temp || ' und ' || windname || ' aus ' || winddir

        /*
        c = db.rawQuery("select sky, city, day, ltrim(substr(time, 7, 2), '0')+0, precip, temp, windname, winddir from forecast order by time", null);
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    if (list.length() != 0) list.append(" +++ ");
                    start = list.length();
                    list.append(getZeroSpacedText(c.getString(0)));
                    list.setSpan(new ForegroundColorSpan(0xFFFFFF22), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(getZeroSpacedText(" in "));
                    start = list.length();
                    list.append(getZeroSpacedText(c.getString(1)));
                    list.setSpan(new ForegroundColorSpan(0xFFFFFF22), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(getZeroSpacedText(" am "));
                    start = list.length();
                    list.append(getZeroSpacedText(c.getString(2)));
                    list.setSpan(new ForegroundColorSpan(0xFFFFFFA0), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(getZeroSpacedText(" ab "));
                    start = list.length();
                    list.append(getZeroSpacedText(c.getString(3) + " Uhr"));
                    list.setSpan(new ForegroundColorSpan(0xFFFFFFA0), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(getZeroSpacedText(" mit "));
                    start = list.length();
                    list.append(getZeroSpacedText(c.getString(4)));
                    list.setSpan(new ForegroundColorSpan(0xFFFFFFA0), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(getZeroSpacedText(" bei "));
                    start = list.length();
                    list.append(getZeroSpacedText(c.getString(5)));
                    list.setSpan(new ForegroundColorSpan(0xFFFFFFA0), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(getZeroSpacedText(" und "));
                    start = list.length();
                    list.append(getZeroSpacedText(c.getString(6)));
                    list.setSpan(new ForegroundColorSpan(0xFFFFFFA0), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(getZeroSpacedText(" aus "));
                    start = list.length();
                    list.append(getZeroSpacedText(c.getString(7)));
                    list.setSpan(new ForegroundColorSpan(0xFFFFFFA0), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } while (c.moveToNext());
            }
            c.close();
        }
        */
        return list;
    }

    private static String getZeroSpacedText(String text) {
        StringBuilder builder = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            builder.append(text.charAt(i));
            builder.append("\u200B");
        }
        return builder.toString();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists forecast (location text, time integer, city text, day text, sky text, temp text, precip text, windname text, winddir text, stamp integer, primary key (location, time))");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("drop table if exists logfile");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("drop table if exists logfile");
        onCreate(db);
    }
}