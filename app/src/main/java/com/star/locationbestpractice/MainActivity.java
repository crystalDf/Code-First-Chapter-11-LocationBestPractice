package com.star.locationbestpractice;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final int SHOW_LOCATION = 0;

    private TextView mPositionTextView;
    private LocationManager mLocationManager;

    private LocationListener mLocationListener;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPositionTextView = (TextView) findViewById(R.id.position_text_view);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                showLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SHOW_LOCATION:
                        mPositionTextView.setText((CharSequence) msg.obj);
                        break;
                    default:
                        break;
                }
            }
        };

        String provider;

        List<String> providerList = mLocationManager.getProviders(true);
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            Toast.makeText(this, "No location provider to use", Toast.LENGTH_LONG).show();
            return;
        }

        Location location = mLocationManager.getLastKnownLocation(provider);

        if (location != null) {
            showLocation(location);
        }

        mLocationManager.requestLocationUpdates(provider, 5000, 1, mLocationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    private void showLocation(final Location location) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder
                        .append(getResources().getString(R.string.server_url))
                        .append("json?")
                        .append("latlng=")
                        .append(location.getLatitude())
                        .append(",")
                        .append(location.getLongitude())
                        .append("&sensor=false");
//                stringBuilder
//                        .append(getResources().getString(R.string.server_url))
//                        .append(location.getLatitude())
//                        .append(",")
//                        .append(location.getLongitude());

                InputStream inputStream = null;

                try {
                    URL url = new URL(stringBuilder.toString());

                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                    httpURLConnection.setRequestProperty("Accept-Language", "zh-CN");

                    inputStream = httpURLConnection.getInputStream();

                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(inputStream));

                    StringBuilder sb = new StringBuilder();

                    String line = null;

                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }

                    String response = sb.toString();

                    String address = getAddress(response);

                    Message message = new Message();
                    message.what = SHOW_LOCATION;
                    message.obj = address;

                    mHandler.sendMessage(message);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private String getAddress(String string) {
        try {
            JSONObject jsonObject = new JSONObject(string);

            JSONArray jsonArray = jsonObject.getJSONArray("results");

            JSONObject addressJsonObject = jsonArray.getJSONObject(0);

            return addressJsonObject.getString("formatted_address");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "No address";
    }

}
