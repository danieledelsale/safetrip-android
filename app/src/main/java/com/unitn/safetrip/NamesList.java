package com.unitn.safetrip;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.ArrayList;
import java.util.List;

public class NamesList extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    List<DevInfo> devList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_names_list);

        new getList().execute();

        Button btnSend = findViewById(R.id.button);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONArray devices = new JSONArray();
                for (int i = 0; i<devList.size(); i++) {
                    JSONObject dev = new JSONObject();
                    try {
                        dev.put("serial", devList.get(i).serial);
                        dev.put("name", devList.get(i).devName);
                        devices.put(dev);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                new sendList().execute(devices.toString());
            }

        });


    }


    private class getList extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            BufferedReader in = null;
            String data = null;

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                URI website = new URI("http://safetrip-api-staging.herokuapp.com/devices");
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
                JSONArray json_data = new JSONArray(result);
                for (int i=0; i<json_data.length(); i++) {
                    JSONObject device = json_data.getJSONObject(i);
                    devList.add(new DevInfo(device.getString("serial"), device.getString("name")));
                }
                    recyclerView = findViewById(R.id.devList);

                    // use this setting to improve performance if you know that changes
                    // in content do not change the layout size of the RecyclerView
                    recyclerView.setHasFixedSize(true);
                    // use a linear layout manager
                    layoutManager = new LinearLayoutManager(NamesList.this);
                    recyclerView.setLayoutManager(layoutManager);
                    mAdapter = new MyAdapter(devList);
                    recyclerView.setAdapter(mAdapter);


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class sendList extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... request) {
            try {
                String url = "http://safetrip-api-staging.herokuapp.com/devices";
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

    private class DevInfo {
        String serial;
        String devName;

        DevInfo(String serial, String devName) {
            this.serial = serial;
            this.devName = devName;
        }
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        private List<DevInfo> mDataset;
        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class MyViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView textView;
            public MyViewHolder(TextView v) {
                super(v);
                textView = v;
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<DevInfo> myDataset) {
            mDataset = myDataset;
        }


        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
            // create a new view
            TextView v = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_view, parent, false);
            MyViewHolder vh = new MyViewHolder(v);
            return vh;
        }
        
        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            
            holder.textView.setText("Serial: " + mDataset.get(position).serial + " - DisplayedName: " + mDataset.get(position).devName);
            holder.textView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(NamesList.this);
                    builder.setTitle("Set display name");

                    // Set up the input
                    final EditText input = new EditText(NamesList.this);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            devList.set(position, new DevInfo(devList.get(position).serial,input.getText().toString()));
                            mAdapter.notifyItemChanged(position);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();

                }
            });

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }


    }


}
