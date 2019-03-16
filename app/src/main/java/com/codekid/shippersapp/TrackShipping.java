package com.codekid.shippersapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.codekid.shippersapp.Common.Common;
import com.codekid.shippersapp.Common.DirectionJSONParser;
import com.codekid.shippersapp.Model.Request;
import com.codekid.shippersapp.Remote.IGeoCoordinates;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import info.hoang8f.widget.FButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class TrackShipping extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    LocationRequest locationRequest;

    Location mLastLocation;

    SupportMapFragment mapFragment;

    Marker mMarker;
    Polyline polyline;

    IGeoCoordinates mService;

    FButton btn_call,btn_shipped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_shipping);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        btn_call = findViewById(R.id.btn_call);
        btn_shipped = findViewById(R.id.btn_shipped);

        btn_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:"+Common.currentRequest.getPhone()));
                if (ActivityCompat.checkSelfPermission(TrackShipping.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(TrackShipping.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivity(intent);
            }
        });

        btn_shipped.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shippedOrder();

            }
        });

        mService = Common.getGeoCodeService();

        buildLocationRequest();
        buildLocationCallback();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

    }

    private void shippedOrder() {
        // we will delete order in table
        //=>shippingOrders &
        //=>ShippingTable
        //Then Update status of order to shipped
        FirebaseDatabase.getInstance()
                .getReference(Common.SHIPPING_TABLE)
                .child(Common.currentShipper.getPhone())
                .child(Common.currentKey)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Update Status on Request Table
                        Map<String,Object> update_status = new HashMap<>();
                        update_status.put("status","03");

                        FirebaseDatabase.getInstance()
                                .getReference("Requests")
                                .child(Common.currentKey)
                                .updateChildren(update_status)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //Delete From ShippingOrder
                                        FirebaseDatabase.getInstance()
                                                .getReference(Common.SHIPPER_INFO_TABLE)
                                                .child(Common.currentKey)
                                                .removeValue()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(TrackShipping.this, "Shipped!!..", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                });

                                    }
                                });

                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        boolean isSuccess = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.uber_style));
        if (!isSuccess)
            Log.d("CODEERR","Map Style Failed To Load");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        mLastLocation = location;
                        LatLng ourLocation = new LatLng(location.getLatitude(),location.getLongitude());
                        mMarker = mMap.addMarker(new MarkerOptions().position(ourLocation).title("Your Location"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(ourLocation));
                        mMap.animateCamera(CameraUpdateFactory.zoomBy(16.0f));

                    }
                });
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                mLastLocation = locationResult.getLastLocation();

                if (mMarker !=null)
                    mMarker.setPosition(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude())); //place marker at our position on the map

                //Update Location To Firebase
                Common.updateShippingInfo(Common.currentKey,mLastLocation);

                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude())));

                mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f));

                drawRoute(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()),Common.currentRequest);

            }
        };

    }


    private void drawRoute(final LatLng ourLocation, Request request) {
        if (polyline !=null)
            polyline.remove();

        if (request.getAddress() !=null && !request.getAddress().isEmpty())
        {
            mService.getGeoCode(request.getAddress()).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());

                                String lat =((JSONArray)jsonObject.get("results"))
                                        .getJSONObject(0)
                                        .getJSONObject("geometry")
                                        .getJSONObject("location")
                                        .get("lat").toString();

                                String lng =((JSONArray)jsonObject.get("results"))
                                        .getJSONObject(0)
                                        .getJSONObject("geometry")
                                        .getJSONObject("location")
                                        .get("lng").toString();

                                LatLng orderLocation = new LatLng(Double.parseDouble(lat),Double.parseDouble(lng));

                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.box);

                                bitmap = Common.scaleBitmap(bitmap,70,70);

                                MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                        .title("Order of"+Common.currentRequest.getPhone())
                                        .position(orderLocation);

                                mMap.addMarker(marker);


                                //draw route
                                mService.getDirections(ourLocation.latitude+","+ourLocation.longitude,
                                        orderLocation.latitude+","+orderLocation.longitude)
                                        .enqueue(new Callback<String>() {
                                            @Override
                                            public void onResponse(Call<String> call, Response<String> response) {
                                                new ParserTask().execute(response.body().toString());
                                            }

                                            @Override
                                            public void onFailure(Call<String> call, Throwable t) {

                                            }
                                        });

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {

                        }
                    });
        }
        else
        {
            if (request.getLatLng() != null && !request.getAddress().isEmpty())
            {
                String[] latLng = request.getLatLng().split(",");
                LatLng orderLocation = new LatLng(Double.parseDouble(latLng[0]),Double.parseDouble(latLng[1]));

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.box);

                bitmap = Common.scaleBitmap(bitmap,70,70);

                MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        .title("Order of"+Common.currentRequest.getPhone())
                        .position(orderLocation);

                mMap.addMarker(marker);

                mService.getDirections(ourLocation.latitude+","+ourLocation.longitude,
                        orderLocation.latitude+","+orderLocation.longitude)
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                new ParserTask().execute(response.body().toString());
                            }

                            @Override
                            public void onFailure(Call<String> call, Throwable t) {

                            }
                        });

            }
        }

    }

    private class ParserTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>> {
        AlertDialog mDialog = new SpotsDialog(TrackShipping.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.show();
            mDialog.setMessage("Please Wait...");
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jObject;
            List<List<HashMap<String,String>>> routes =null;
            try{
                jObject = new JSONObject(strings[0]);
                DirectionJSONParser parser = new DirectionJSONParser();

                routes =parser.parse(jObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }


        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();

            ArrayList points = null;
            PolylineOptions lineOptions = null;

            for(int i=0;i<lists.size();i++)
            {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String,String>> path = lists.get(i);

                for(int j =0;j<path.size();j++)
                {
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    LatLng position = new LatLng(lat,lng);

                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);
            }
          polyline=  mMap.addPolyline(lineOptions);
        }
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
    }

    @Override
    protected void onStop() {
        if (fusedLocationProviderClient !=null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }
}
