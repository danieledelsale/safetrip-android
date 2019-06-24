package com.unitn.safetrip;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.Set;

public class Settings extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    String hostname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        super.onCreate(savedInstanceState);
        hostname = getIntent().getStringExtra("hostname");
        setContentView(R.layout.activity_settings);

        new getSettings().execute();


        final Button saveBtn = findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Switch safeAreaToggle = findViewById(R.id.safeAreaToggle);
                EditText safeAreaLong = findViewById(R.id.safeAreaCenterLong);
                EditText safeAreaLat = findViewById(R.id.safeAreaCenterLat);
                EditText safeAreaRange = findViewById(R.id.safeAreaRange);
                Switch safeGroupToggle = findViewById(R.id.safeGroupToggle);
                EditText safeGroupDistance = findViewById(R.id.safeGroupDistance);
                JSONObject settings = new JSONObject();
                try {
                    settings.put("safeAreaEnabled", safeAreaToggle.isChecked());
                    if (safeAreaToggle.isChecked() && !safeAreaLat.getText().toString().matches("") && !safeAreaLong.getText().toString().matches("") && !safeAreaRange.getText().toString().matches("")) {
                            GPSCoords center = new GPSCoords(Double.parseDouble(safeAreaLat.getText().toString()),Double.parseDouble(safeAreaLong.getText().toString()));
                            int distance = Integer.parseInt(safeAreaRange.getText().toString());
                            GPSGeoFence safeArea = new GPSGeoFence();
                        JSONObject safeAreaGeography = new JSONObject();
                        safeAreaGeography.put("type", "Polygon");
                        JSONArray coords = new JSONArray();
                        safeArea.point1 = getCoords(center,0,distance);
                        coords.put(loadJPoint(safeArea.point1));
                        safeArea.point2 = getCoords(center,30,distance);
                        coords.put(loadJPoint(safeArea.point2));
                        safeArea.point3 = getCoords(center,60,distance);
                        coords.put(loadJPoint(safeArea.point3));
                        safeArea.point4 = getCoords(center,90,distance);
                        coords.put(loadJPoint(safeArea.point4));
                        safeArea.point5 = getCoords(center,120,distance);
                        coords.put(loadJPoint(safeArea.point5));
                        safeArea.point6 = getCoords(center,150,distance);
                        coords.put(loadJPoint(safeArea.point6));
                        safeArea.point7 = getCoords(center,180,distance);
                        coords.put(loadJPoint(safeArea.point7));
                        safeArea.point8 = getCoords(center,210,distance);
                        coords.put(loadJPoint(safeArea.point8));
                        safeArea.point9 = getCoords(center,240,distance);
                        coords.put(loadJPoint(safeArea.point9));
                        safeArea.point10 = getCoords(center,270,distance);
                        coords.put(loadJPoint(safeArea.point10));
                        safeArea.point11 = getCoords(center,300,distance);
                        coords.put(loadJPoint(safeArea.point11));
                        safeArea.point12 = getCoords(center,330,distance);
                        coords.put(loadJPoint(safeArea.point12));
                        coords.put(loadJPoint(safeArea.point1));
                        JSONArray quickFix = new JSONArray();
                        quickFix.put(coords);
                        safeAreaGeography.put("coordinates", quickFix);

                        settings.put("safeAreaGeography", safeAreaGeography);
                    }
                    else if (safeAreaToggle.isChecked())
                    {
                        Toast.makeText(Settings.this, "Please fill all the fields", Toast.LENGTH_LONG).show();
                    }
                    settings.put("safeGroupEnabled", safeGroupToggle.isChecked());
                    if (!safeGroupDistance.getText().toString().matches("") && safeGroupToggle.isChecked())
                        settings.put("safeGroupDistance", Integer.parseInt(safeGroupDistance.getText().toString()));
                    else if (safeGroupToggle.isChecked())
                        Toast.makeText(Settings.this, "Please fill all the fields", Toast.LENGTH_LONG).show();
                    String request = settings.toString();
                    new sendSettings().execute(request);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button currLocBtn = findViewById(R.id.setOnMyLocBtn);
        currLocBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(Settings.this,"PLEASE ENABLE LOCATION PERMISSION IN SETTINGS", Toast.LENGTH_LONG).show();
                    return;
                }
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(Settings.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    EditText lat = findViewById(R.id.safeAreaCenterLat);
                                    EditText lon = findViewById(R.id.safeAreaCenterLong);
                                    lon.setText(String.valueOf(location.getLongitude()));
                                    lat.setText(String.valueOf(location.getLatitude()));
                                }
                            }
                        });
            }
        });


    }

    private JSONArray loadJPoint(GPSCoords point) {
        JSONArray jPoint = new JSONArray();
        try {
            jPoint.put(point.lon);
            jPoint.put(point.lat);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jPoint;
    }

    private class getSettings extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            BufferedReader in = null;
            String data = null;

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                URI website = new URI("http://" + hostname + "/options");
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
            try {
                JSONObject json_data = new JSONObject(result);
                Switch safeAreaToggle = findViewById(R.id.safeAreaToggle);
                if (json_data.getBoolean("safeAreaEnabled")) {
                    safeAreaToggle.setChecked(true);
                    EditText safeAreaLong = findViewById(R.id.safeAreaCenterLong);
                    EditText safeAreaLat = findViewById(R.id.safeAreaCenterLat);
                } else safeAreaToggle.setChecked(false);
                Switch safeGroupToggle = findViewById(R.id.safeGroupToggle);
                if (json_data.getBoolean("safeGroupEnabled")) {
                    safeGroupToggle.setChecked(true);
                    EditText safeGroupDistance = findViewById(R.id.safeGroupDistance);
                    safeGroupDistance.setText(String.valueOf(json_data.getInt("safeGroupDistance")));
                } else safeGroupToggle.setChecked(false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class sendSettings extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... request) {
            try {
                String url = "http://" + hostname + "/options";
                URL object = new URL(url);

                HttpURLConnection con = (HttpURLConnection) object.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod("PUT");


                OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());

                wr.write(request[0]);
                wr.flush();

                //display what returns the POST request

                StringBuilder sb = new StringBuilder();
                int HttpResult = con.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    System.out.println("" + sb.toString());
                } else {
                    System.out.println(con.getResponseMessage());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return "ok";
        }

        @Override
        protected void onPostExecute(String s) {
            finish();

        }
    }

    private class GPSCoords {
        public double lat;
        public double lon;

        public GPSCoords(double lat2, double lon2) {
            this.lat = lat2;
            this.lon = lon2;
        }
    }

    private GPSCoords getCoords(GPSCoords center, double angle, double distance) {
        double R = 6378.1; //radius of the Earth
        angle = Math.toRadians(angle); //convert angle in radians
        distance = distance/1000; //convert in km

        double lat1 = Math.toRadians(center.lat);
        double lon1 = Math.toRadians(center.lon);

        double lat2 = Math.asin(Math.sin(lat1)*Math.cos(distance/R)+Math.cos(lat1)*Math.sin(distance/R)*Math.cos(angle));
        double lon2 = lon1 + Math.atan2(Math.sin(angle)*Math.sin(distance/R)*Math.cos(lat1),Math.cos(distance/R)-Math.sin(lat1)*Math.sin(lat2));

        lat2 = Math.toDegrees(lat2);
        lon2 = Math.toDegrees(lon2);

        return new GPSCoords(lat2,lon2);

    }

    private class GPSGeoFence {
        public GPSCoords point1;
        public GPSCoords point2;
        public GPSCoords point3;
        public GPSCoords point4;
        public GPSCoords point5;
        public GPSCoords point6;
        public GPSCoords point7;
        public GPSCoords point8;
        public GPSCoords point9;
        public GPSCoords point10;
        public GPSCoords point11;
        public GPSCoords point12;
    }
}
