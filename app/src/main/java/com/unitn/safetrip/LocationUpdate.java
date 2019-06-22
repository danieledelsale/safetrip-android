package com.unitn.safetrip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class LocationUpdate extends Service {

    public LocationUpdate() {
    }
    int mStartMode;       // indicates how to behave if the service is killed
    IBinder binder;      // interface for clients that bind
    boolean allowRebind; // indicates whether onRebind should be used

    Handler mHandler = new Handler();
    List<Alert> alertList = new ArrayList<>();

    Runnable mHandlerTask = new Runnable()
    {
        @Override
        public void run() {
            routine();
            mHandler.postDelayed(mHandlerTask, 10000);
        }
    };

    private void routine() {
        new fetchAlerts().execute();
    }


    @Override
    public void onCreate() {
        // The service is being created


    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        // The service is starting, due to a call to startService()
        Intent notificationIntent = new Intent(this, LocationUpdate.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, "DEFAULT")
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_description))
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(1337, notification);
        mHandlerTask.run();
        return mStartMode;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        mHandler.removeCallbacks(mHandlerTask);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("DEFAULT", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private class fetchAlerts extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            BufferedReader in = null;
            String data = null;

            try {
                HttpClient httpclient = new DefaultHttpClient();

                HttpGet request = new HttpGet();
                URI website = new URI("http://safetrip-api-staging.herokuapp.com/alerts");
                request.setURI(website);
                HttpResponse response = httpclient.execute(request);
                in = new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent()));

                // Pass data to onPostExecute method
                return (in.readLine());

            } catch (Exception e) {
                Log.e("log_tag", "Error in http connection " + e.toString());
                return "unsuccessful";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            JSONArray jArray = null;
            try {
                jArray = new JSONArray(result);
                // Extract data from json and store into ArrayList as class objects
                if (jArray.length() > alertList.size()) {
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject json_data = jArray.getJSONObject(i);
                        Alert alert = new Alert();
                        alert.id = json_data.getInt("id");
                        alert.serial = json_data.getString("serial");
                        alert.text = json_data.getString("text");
                        alert.read = json_data.getBoolean("read");
                        if (!alert.read)
                            alertList.add(alert);
                            alert.createNotification();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }
    private class Alert {
        int id;
        String text;
        String serial;
        boolean read;

        public void createNotification() {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(LocationUpdate.this, "DEFAULT")
                    .setContentTitle("New Alert")
                    .setContentText(this.text)
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                    .setPriority(NotificationCompat.PRIORITY_MAX);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(LocationUpdate.this);
            notificationManager.notify(this.id, builder.build());


        }
    }
}

