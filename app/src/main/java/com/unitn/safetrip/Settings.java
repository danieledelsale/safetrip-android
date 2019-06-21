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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        new getSettings().execute();


        final Button saveBtn = (Button) findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Switch safeAreaToggle = findViewById(R.id.safeAreaToggle);
                EditText safeAreaLong = findViewById(R.id.safeAreaCenterLong);
                EditText safeAreaLat = findViewById(R.id.safeAreaCenterLat);
                Switch safeGroupToggle = findViewById(R.id.safeGroupToggle);
                EditText safeGroupDistance = findViewById(R.id.safeGroupDistance);
                JSONObject settings = new JSONObject();
                try {
                    settings.put("safeAreaEnabled", safeAreaToggle.isChecked());
                    settings.put("safeGroupEnabled", safeGroupToggle.isChecked());
                    if (!safeGroupDistance.getText().toString().matches(""))
                        settings.put("safeGroupDistance", Integer.parseInt(safeGroupDistance.getText().toString()));
                    String request = settings.toString();
                    Log.e("SENT JSON", request);
                    new sendSettings().execute(request);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button currLocBtn = (Button) findViewById(R.id.setOnMyLocBtn);
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

    private class getSettings extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            BufferedReader in = null;
            String data = null;

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                URI website = new URI("http://safetrip-api-staging.herokuapp.com/options");
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
            Toast.makeText(Settings.this, result, Toast.LENGTH_LONG).show();
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

    private class sendSettings extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... request) {
            try {
                String url = "http://safetrip-api-staging.herokuapp.com/options";
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


            return null;
        }
    }
}
