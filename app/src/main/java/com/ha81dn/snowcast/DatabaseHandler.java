package com.ha81dn.snowcast;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import java.text.SimpleDateFormat;
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

    public static void store(SQLiteDatabase db, String location, String time, String sky, String city, String day, String temp, String precip, String windname, String winddir) {
        Cursor c;
        ContentValues vals = new ContentValues();
        boolean update = false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHH", Locale.getDefault());
        Calendar now = Calendar.getInstance();

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
        if (update)
            db.update("forecast", vals, "location = ? and time = ?", new String[]{location, time});
        else
            db.insert("forecast", null, vals);

        c = db.rawQuery("delete from forecast where time < ? or time > 99123123", new String[]{sdf.format(now.getTime())});
        if (c != null) {
            c.moveToFirst();
            c.close();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists forecast (location text, time integer, city text, day text, sky text, temp text, precip text, windname text, winddir text, primary key (location, time))");
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

    public Spannable retrieve(SQLiteDatabase db, String location) {
        Cursor c;
        SpannableStringBuilder list = new SpannableStringBuilder();
        int start;

        //sky || ' in ' || city || ' am ' || day || ' ab ' || ltrim(substr(time, 7, 2), '0') || ' Uhr mit ' || precip || ' bei ' || temp || ' und ' || windname || ' aus ' || winddir

        c = db.rawQuery("select sky, city, day, ltrim(substr(time, 7, 2), '0'), precip, temp, windname, winddir where location = ? order by time", new String[]{location});
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    if (list.length() != 0) list.append("\n");
                    start = list.length();
                    list.append(c.getString(0));
                    list.setSpan(new ForegroundColorSpan(0xFFFF0000), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(" in ");
                    start = list.length();
                    list.append(c.getString(1));
                    list.setSpan(new ForegroundColorSpan(0xFFFF0000), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(" am ");
                    start = list.length();
                    list.append(c.getString(2));
                    list.setSpan(new ForegroundColorSpan(0xFFFF0000), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(" ab ");
                    start = list.length();
                    list.append(c.getString(3));
                    list.setSpan(new ForegroundColorSpan(0xFFFF0000), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(" Uhr mit ");
                    start = list.length();
                    list.append(c.getString(4));
                    list.setSpan(new ForegroundColorSpan(0xFFFF0000), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(" bei ");
                    start = list.length();
                    list.append(c.getString(5));
                    list.setSpan(new ForegroundColorSpan(0xFFFF0000), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(" und ");
                    start = list.length();
                    list.append(c.getString(6));
                    list.setSpan(new ForegroundColorSpan(0xFFFF0000), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    list.append(" aus ");
                    start = list.length();
                    list.append(c.getString(7));
                    list.setSpan(new ForegroundColorSpan(0xFFFF0000), start, list.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } while (c.moveToNext());
            }
            c.close();
        }

        return list;
    }
}