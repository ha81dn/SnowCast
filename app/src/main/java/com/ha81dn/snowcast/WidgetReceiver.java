package com.ha81dn.snowcast;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class WidgetReceiver extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals("com.ha81dn.snowcast.UPDATE")) {
            Log.w("onReceive", intent.getAction());

            ComponentName thisWidget = new ComponentName(context,
                    WidgetReceiver.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int widgetId : allWidgetIds) {
                // create some random data
                int number = (new Random().nextInt(100));

                RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                        R.xml.widget_layout);
                Log.w("WidgetExample", String.valueOf(number));
                // Set the text
                remoteViews.setTextViewText(R.id.update, String.valueOf(number));
                appWidgetManager.updateAppWidget(widgetId, remoteViews);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        ComponentName thisWidget = new ComponentName(context,
                WidgetReceiver.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.xml.widget_layout);
            Intent intent = new Intent(context, WidgetReceiver.class);
            intent.setAction("com.ha81dn.snowcast.UPDATE");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    public String httpGet(String urlString) {

        StringBuffer chaine = new StringBuffer("");
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.connect();

            InputStream inputStream = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = rd.readLine()) != null) {
                chaine.append(line);
            }

        } catch (Exception ignore) {
        }

        return chaine.toString();
    }


    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return httpGet(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            // Zuweisung an Widget
        }
    }


}