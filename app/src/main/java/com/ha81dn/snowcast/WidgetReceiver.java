package com.ha81dn.snowcast;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            Log.w("onReceive", intent.getAction());

            AsyncTask<String, Void, String> getter = new HttpAsyncTask();
            ((HttpAsyncTask) getter).context = context;
            ((HttpAsyncTask) getter).appWidgetManager = appWidgetManager;
            ((HttpAsyncTask) getter).index = "idx1";
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.xml.widget_layout);
            remoteViews.setTextViewText(R.id.update, context.getString(R.string.data_fetch_1));

            ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int widgetId : allWidgetIds) {
                appWidgetManager.updateAppWidget(widgetId, remoteViews);
            }
            getter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "http://kachelmannwetter.com/de/vorhersage/#idx/lighttrend");
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.xml.widget_layout);
            Intent intent = new Intent(context, WidgetReceiver.class);
            intent.setAction("com.ha81dn.snowcast.UPDATE");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        public AppWidgetManager appWidgetManager;
        public Context context;
        public String location;
        public String index;
        boolean flag = false;

        @Override
        protected String doInBackground(String... urls) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            StringBuilder chaine = new StringBuilder("");
            String location = sharedPref.getString(index, "").trim();
            if (location.equals("")) return "";

            try {
                URL url = new URL(urls[0].replace("#idx", location));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.setConnectTimeout(60000);
                connection.connect();

                InputStream inputStream = connection.getInputStream();

                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                int i = 0;
                while ((line = rd.readLine()) != null) {
                    i++;
                    chaine.append(line);
                    if (i % 10 == 0) publishProgress();
                }
            } catch (Exception ignore) {
            }
            return chaine.toString();
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.xml.widget_layout);
            if (flag) {
                remoteViews.setTextViewText(R.id.update, context.getString(R.string.data_fetch_1));
                flag = false;
            } else {
                remoteViews.setTextViewText(R.id.update, context.getString(R.string.data_fetch_0));
                flag = true;
            }

            ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int widgetId : allWidgetIds) {
                appWidgetManager.updateAppWidget(widgetId, remoteViews);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
            String time, day, sky, city = "", temp, precip, windname, winddir, wMid, wMax;
            int snowPos, pos, len = result.length();

            if (!result.equals("")) {
                snowPos = result.indexOf("chnee");
                if (snowPos >= 0)
                    city = getInnerText(result, 0, "<title>Wettervorhersage Light für ", " |", false);
                while (snowPos >= 0) {
                    try {
                        do {
                            sky = getInnerText(result, snowPos, "<p>", "</p>", true);
                            pos = nextOccPos(result, snowPos, "<h4>", true);
                            if (pos == -1) break;
                            time = getInnerText(result, pos, ">", ":", false);
                            pos = nextOccPos(result, pos, "\">Temperatur<", false);
                            if (pos == len) break;
                            pos = nextOccPos(result, pos + 10, "\">", false);
                            if (pos == len) break;
                            temp = getInnerText(result, pos, ">", "<", false).replace(".", ",");
                            pos = nextOccPos(result, pos, "\">Niederschlag<", false);
                            if (pos == len) break;
                            pos = nextOccPos(result, pos + 10, "\">", false);
                            if (pos == len) break;
                            precip = getInnerText(result, pos, ">", "<", false).replace(".", ",");
                            pos = nextOccPos(result, pos, " Bft", false);
                            wMax = getInnerText(result, pos, "/ ", " B", true);
                            pos = nextOccPos(result, pos + 10, " Bft", false);
                            wMid = getInnerText(result, pos, "/ ", " B", true);
                            pos = nextOccPos(result, pos, "wi-from-", false);
                            if (pos == len) break;
                            winddir = getInnerText(result, pos, "-", " wind-daytable", false);
                            pos = nextOccPos(result, snowPos, "\"day_", true);
                            if (pos == -1) break;
                            time = getInnerText(result, pos, "_", "\"", false) + time;
                            if (time.length() != 8) break;
                            pos = nextOccPos(result, pos, ",", false);
                            if (pos == len) break;
                            day = getInnerText(result, pos, " ", ",", true);
                            windname = translateBft(context, Integer.parseInt(wMid), Integer.parseInt(wMax));
                            winddir = translateWDir(context, winddir);
                            DatabaseHandler.store(db, location, time, sky, city, day, temp, precip, windname, winddir);
                        } while (false);
                    } catch (Exception ignore) {
                    }

                    snowPos = result.indexOf("chnee", snowPos + 5);
                }

                db.close();
            }

            if (index.equals("idx5")) {
                showForecast(context, appWidgetManager, appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetReceiver.class)));
            } else {
                AsyncTask<String, Void, String> getter = new HttpAsyncTask();
                ((HttpAsyncTask) getter).context = context;
                ((HttpAsyncTask) getter).appWidgetManager = appWidgetManager;
                ((HttpAsyncTask) getter).index = "idx" + Integer.toString(Integer.parseInt(index.substring(3, 4)) + 1);
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.xml.widget_layout);
                remoteViews.setTextViewText(R.id.update, context.getString(R.string.data_fetch_1));

                ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
                int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                for (int widgetId : allWidgetIds) {
                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
                }
                getter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "http://kachelmannwetter.com/de/vorhersage/#idx/lighttrend");
            }


            // Demo-Code
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.xml.widget_layout);
            remoteViews.setTextViewText(R.id.update, getNonbreakingText(result.substring(0, 500)));

            ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int widgetId : allWidgetIds) {
                appWidgetManager.updateAppWidget(widgetId, remoteViews);
            }
        }

        private void showForecast(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.xml.widget_layout);
            // Zuweiserei: erst in Variable holen, einmal setTextViewText und dann alle durchrasseln

            for (int widgetId : appWidgetIds) {
                remoteViews.setTextViewText(R.id.update, "");
                appWidgetManager.updateAppWidget(widgetId, remoteViews);
            }
        }

        private String translateWDir(Context context, String wDir) {
            return context.getResources().getString(context.getResources().getIdentifier("wdir_" + wDir, "string", context.getPackageName()));
        }

        private String translateBft(Context context, int bftMid, int bftMax) {
            if (bftMax < 0 || bftMax > 12) bftMax = bftMid;
            if (bftMid < 0 || bftMid > 12) bftMid = bftMax;
            if (bftMid < 0 || bftMid > 12)
                return context.getString(R.string.bft_unknown);
            else {
                if (bftMid == bftMax)
                    return context.getResources().getString(context.getResources().getIdentifier("bft_" + Integer.toString(bftMid), "string", context.getPackageName()));
                else
                    return context.getString(R.string.bft_range, context.getResources().getString(context.getResources().getIdentifier("bft_" + Integer.toString(bftMid), "string", context.getPackageName())), context.getResources().getString(context.getResources().getIdentifier("bft_" + Integer.toString(bftMax), "string", context.getPackageName())));
            }
        }

        private String getNonbreakingText(String text) {
            StringBuilder builder = new StringBuilder(text.length() * 2);
            for (int i = 0; i < text.length(); i++) {
                builder.append(text.charAt(i));
                builder.append("\u200B");
            }
            return builder.toString();
        }

        private int nextOccPos(String page, int pos, String stopper, boolean rtl) {
            int nextPos, lenS;
            char firstS;
            lenS = stopper.length();
            firstS = stopper.charAt(0);
            if (rtl) {
                for (nextPos = pos; nextPos >= 0; nextPos--) {
                    if (page.charAt(nextPos) == firstS) {
                        if (page.substring(nextPos, nextPos + lenS).equals(stopper)) break;
                    }
                }
            } else {
                for (nextPos = pos; nextPos < page.length(); nextPos++) {
                    if (page.charAt(nextPos) == firstS) {
                        if (page.substring(nextPos, nextPos + lenS).equals(stopper)) break;
                    }
                }
            }
            return nextPos;
        }

        private String getInnerText(String page, int pos, String leftStopper, String rightStopper, boolean rtl) {
            int leftPos, rightPos, lenLS, lenRS;
            char firstL, firstR;
            lenLS = leftStopper.length();
            lenRS = rightStopper.length();
            firstL = leftStopper.charAt(0);
            firstR = rightStopper.charAt(0);
            if (rtl) {
                for (leftPos = pos; leftPos >= 0; leftPos--) {
                    if (page.charAt(leftPos) == firstL) {
                        if (page.substring(leftPos, leftPos + lenLS).equals(leftStopper)) break;
                    }
                }
                if (leftPos == -1) return "";
                for (rightPos = leftPos; rightPos < page.length(); rightPos++) {
                    if (page.charAt(rightPos) == firstR) {
                        if (page.substring(rightPos, rightPos + lenRS).equals(rightStopper)) break;
                    }
                }
                if (rightPos == page.length()) return "";
            } else {
                for (rightPos = pos; rightPos < page.length(); rightPos++) {
                    if (page.charAt(rightPos) == firstR) {
                        if (page.substring(rightPos, rightPos + lenRS).equals(rightStopper)) break;
                    }
                }
                if (rightPos == page.length()) return "";
                for (leftPos = rightPos; leftPos >= 0; leftPos--) {
                    if (page.charAt(leftPos) == firstL) {
                        if (page.substring(leftPos, leftPos + lenLS).equals(leftStopper)) break;
                    }
                }
                if (leftPos == -1) return "";
            }
            return page.substring(leftPos + lenLS, rightPos);
        }

    }
}