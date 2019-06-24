package com.unitn.safetrip;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

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

public class MapsActivity extends AppCompatActivity implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback {


    private GoogleMap mMap;
    Intent intent;
    String hostname = "placeholder";
    boolean serviceRunning = false;


    Handler mHandler = new Handler();
    List<KidsData> kidsData = new ArrayList<>();

    Polygon safeArea = null;

    Runnable mHandlerTask = new Runnable()
    {
        @Override
        public void run() {
            if (!hostname.matches("placeholder")) {
            new updateMap().execute();
            new updateSafeArea().execute();
            if (!serviceRunning) {
                intent.putExtra("hostname", hostname);
                serviceRunning = true;
                startService(intent);
            }
            }
            mHandler.postDelayed(mHandlerTask, 10000);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                Intent myIntent = new Intent(MapsActivity.this, Settings.class);
                myIntent.putExtra("hostname", hostname);
                MapsActivity.this.startActivity(myIntent);
                return true;
            case R.id.names:
                Intent myIntent2 = new Intent(MapsActivity.this, NamesList.class);
                myIntent2.putExtra("hostname", hostname);
                MapsActivity.this.startActivity(myIntent2);
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    private class updateMap extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            BufferedReader in = null;
            String data = null;

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                URI website = new URI("http://" + hostname + "/points/latest");
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

                for (int i=0; i<kidsData.size(); i++) kidsData.get(i).removeMarker();
                kidsData.clear();
                String title;

            // Extract data from json and store into ArrayList as class objects
                for(int i=0;i<jArray.length();i++){
                    JSONObject json_data = jArray.getJSONObject(i);
                    KidsData kidData = new KidsData();
                    kidData.id = json_data.getInt("id");
                    kidData.altitude = json_data.getInt("altitude");
                    kidData.serial = json_data.getString("serial");
                    JSONArray coords = json_data.getJSONArray("location");
                    kidData.lat = coords.getDouble(1);
                    kidData.lon = coords.getDouble(0);
                    kidData.timestamp = json_data.getString("timestamp");
                    kidData.name = json_data.getString("name");
                    kidsData.add(kidData);
                    if (kidData.name.matches("")) title = kidData.serial;
                    else title = kidData.name;
                    kidData.marker = mMap.addMarker(new MarkerOptions().position(new LatLng(kidData.lat,kidData.lon)).title(title));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class updateSafeArea extends AsyncTask<String, Void, String> {

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

            JSONObject settings = null;
            try {
                settings = new JSONObject(result);
                if (settings.getBoolean("safeAreaEnabled")) {
                    JSONObject temp = settings.getJSONObject("safeAreaGeography");
                    JSONArray coords = temp.getJSONArray("coordinates");
                    coords = coords.getJSONArray(0);
                    if (safeArea != null) safeArea.remove();
                    safeArea = mMap.addPolygon(new PolygonOptions()
                            .add(
                                    getLatLng(coords.getJSONArray(0)),
                                    getLatLng(coords.getJSONArray(1)),
                                    getLatLng(coords.getJSONArray(2)),
                                    getLatLng(coords.getJSONArray(3)),
                                    getLatLng(coords.getJSONArray(4)),
                                    getLatLng(coords.getJSONArray(5)),
                                    getLatLng(coords.getJSONArray(5)),
                                    getLatLng(coords.getJSONArray(6)),
                                    getLatLng(coords.getJSONArray(7)),
                                    getLatLng(coords.getJSONArray(8)),
                                    getLatLng(coords.getJSONArray(9)),
                                    getLatLng(coords.getJSONArray(10)),
                                    getLatLng(coords.getJSONArray(11))
                            ));
                }
                else {
                    if (safeArea != null)
                    safeArea.remove();
                }

            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }
    }

    private LatLng getLatLng(JSONArray coord) throws JSONException {
        return new LatLng(coord.getDouble(1),coord.getDouble(0));
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        intent = new Intent(MapsActivity.this, LocationUpdate.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("Set hostname");

        // Set up the input
        final EditText input = new EditText(MapsActivity.this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                hostname = input.getText().toString();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();





        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            Toast.makeText(this, "PLEASE ENABLE LOCATION IN SETTINGS", Toast.LENGTH_LONG).show();
        }
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mHandlerTask.run();
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }
    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mHandlerTask);
        stopService(intent);
        super.onDestroy();
    }



    private class KidsData {
        public double lat;
        public double lon;
        public int id;
        public String serial;
        public String timestamp;
        public int altitude;
        public String name;
        Marker marker;

        public void removeMarker() {
            marker.remove();
        }
    }
}
