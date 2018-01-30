package com.example.lukas.mobilecomputingapp.Activites;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.lukas.mobilecomputingapp.Adapters.PlaceAdapter;
import com.example.lukas.mobilecomputingapp.CameraHandler;
import com.example.lukas.mobilecomputingapp.DatabaseHandler;
import com.example.lukas.mobilecomputingapp.Models.LatLng;
import com.example.lukas.mobilecomputingapp.Models.Sight;
import com.example.lukas.mobilecomputingapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NearbyResultActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemClickListener {
    public static final String PLACES_API_KEY = "AIzaSyB7PO0EOXLZu83TyPkbD8rC9HTKBxo4bro";
    public static final String SYGIC_API_KEY = "cVIf6jukcY1kZErWcCfgeaCHWb8L0Wet8Nfqywv9";

    private GoogleMap mMap;
    private ArrayList<Marker> markers = new ArrayList<>();
    private PlaceAdapter placeAdapter;

    private Sight initialSight;
    private com.google.android.gms.maps.model.LatLng gLatLng;
    private Location initLoc;
    private static final int DEFAULT_ZOOM = 14;

    private BottomNavigationView mBottomNavView;
    private CameraHandler camHandler = new CameraHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_result);

        mBottomNavView = (BottomNavigationView) findViewById(R.id.navigation);
        mBottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        MainActivity.setCheckable(mBottomNavView, false);

        initialSight = (Sight) getIntent().getSerializableExtra("sight");
        gLatLng = new com.google.android.gms.maps.model.LatLng(initialSight.getLocation().getLatitude(), initialSight.getLocation().getLongitude());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nearbyMap);
        mapFragment.getMapAsync(this);

        initLoc = new Location("initial location");
        initLoc.setLatitude(initialSight.getLocation().getLatitude());
        initLoc.setLongitude(initialSight.getLocation().getLongitude());

        placeAdapter = new PlaceAdapter(this, new ArrayList<Sight>(), initLoc);
        ListView mListView = (ListView) findViewById(R.id.nearbyListView);
        mListView.setAdapter(placeAdapter);
        mListView.setDrawingCacheEnabled(true);
        mListView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        mListView.setOnItemClickListener(this);

        queryAndSetNearbyPOI(initialSight.getLocation());
    }

    protected void onResume() {
        super.onResume();
        MainActivity.setCheckable(mBottomNavView, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("picPath", camHandler.getmCurrentPhotoPath());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (camHandler != null && savedInstanceState.containsKey("picPath")) {
            camHandler.setmCurrentPhotoPath(savedInstanceState.getString("picPath"));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (mMap != null) {
            Sight s = (Sight) placeAdapter.getItem(i);

            TextView tvDesc = (TextView) view.findViewById(R.id.placeDescTextView);
            if (tvDesc.getVisibility() == View.GONE && !tvDesc.getText().equals("")){
                tvDesc.setVisibility(View.VISIBLE);
            }else if(tvDesc.getVisibility() == View.VISIBLE){
                tvDesc.setVisibility(View.GONE);
            }


            com.google.android.gms.maps.model.LatLng latLng;
            latLng = new com.google.android.gms.maps.model.LatLng(s.getLocation().getLatitude(), s.getLocation().getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM + 3));
            for (Marker m : markers) {
                if (m.getPosition().latitude == latLng.latitude && m.getPosition().longitude == latLng.longitude) {
                    m.showInfoWindow();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.addMarker(new MarkerOptions()
                .position(gLatLng)
                .title(initialSight.getName()));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gLatLng, DEFAULT_ZOOM));
    }

    private void queryAndSetNearbyPOI(LatLng position) {
        /*
        String requestUri = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                + position.getLatitude() + "," + position.getLongitude()
                + "&radius=7000&types=art_gallery|museum|zoo|church|hindu_temple|mosque|synagogue" +
                "&key=" + PLACES_API_KEY;
        */
        /*
        String requestUri = "https://maps.googleapis.com/maps/api/place/textsearch/json?location="
                + position.getLatitude() + "," + position.getLongitude()
                + "&query=tourist+attraction" +
                "&key=AIzaSyB7PO0EOXLZu83TyPkbD8rC9HTKBxo4bro";
        */
        //New API - way better than Google Places
        String requestUri = "https://api.sygictravelapi.com/1.0/en/places/list?" +
                "area=" + position.getLatitude() + "," + position.getLongitude() + ",7000" +
                "&levels=poi" +
                "&categories=sightseeing|discovering|traveling" +
                "&limit=20";
        //need to URL encode the pipe character
        requestUri = requestUri.replace("|", "%7C");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(requestUri)
                .addHeader("x-api-key", SYGIC_API_KEY)
                .build();

        //LATENCY MEASUREMENT -- START
        final long startTime = System.nanoTime();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NearbySearchFailure", e.getLocalizedMessage());
                e.printStackTrace();
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("NearbySearchFailure", "Error in nearby search");
                    throw new IOException("Unexpected code " + response);
                } else {
                    try {
                        //LATENCY MEASUREMENT -- END
                        long elapsedTimeMS = (System.nanoTime() - startTime) / 1000000;
                        Log.i("LATENCY - NEARBY INFO", String.valueOf(elapsedTimeMS)+ "ms");

                        JSONObject resultJSON = new JSONObject(response.body().string()).getJSONObject("data");

                        final JSONArray results = resultJSON.getJSONArray("places");
                        final ArrayList<Sight> sights = new ArrayList<>();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject placeJSON = results.getJSONObject(i);
                            //Log.i("Name", placeJSON.getString("name"));
                            double lat = placeJSON.getJSONObject("location").getDouble("lat");
                            double lng = placeJSON.getJSONObject("location").getDouble("lng");

                            //Todo
                            String picRef = "";
                            if (placeJSON.has("thumbnail_url") && placeJSON.getString("thumbnail_url") != null) {
                                picRef = placeJSON.getString("thumbnail_url");
                            }
                            Sight s = new Sight(placeJSON.getString("name"), placeJSON.getString("perex"), "", picRef, new LatLng(lat, lng));
                            s.setAddress(placeJSON.getString("name_suffix"));
                            sights.add(s);
                        }
                        Collections.sort(sights, new Comparator<Sight>() {
                            @Override
                            public int compare(Sight s1, Sight s2) {
                                Location sight1Location = new Location("Sight1 Location");
                                sight1Location.setLatitude(s1.getLocation().getLatitude());
                                sight1Location.setLongitude(s1.getLocation().getLongitude());
                                Location sight2Location = new Location("Sight2 Location");
                                sight2Location.setLatitude(s2.getLocation().getLatitude());
                                sight2Location.setLongitude(s2.getLocation().getLongitude());

                                float distanceToSight1 = initLoc.distanceTo(sight1Location);
                                float distanceToSight2 = initLoc.distanceTo(sight2Location);

                                return Math.round(distanceToSight1 - distanceToSight2);
                            }
                        });

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                placeAdapter.addAll(sights);
                                for (Sight s : sights) {
                                    markers.add(mMap.addMarker(new MarkerOptions()
                                            .position(new com.google.android.gms.maps.model.LatLng(s.getLocation().getLatitude(), s.getLocation().getLongitude()))
                                            .title(s.getName())
                                    ));
                                }
                            }
                        });


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }
        });
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            MainActivity.setCheckable(mBottomNavView, true);
            switch (item.getItemId()) {
                case R.id.navigation_cam:
                    dispatchTakePictureIntent();
                    camHandler.galleryAddPic();
                    return true;

                case R.id.navigation_map:
                    DatabaseHandler db = new DatabaseHandler(NearbyResultActivity.this);

                    Intent mapsIntent = new Intent(NearbyResultActivity.this, LocationHistoryActivity.class);
                    mapsIntent.putExtra("sights", db.getAllSights());
                    mapsIntent.putExtra("center", initialSight.getLocation());

                    db.close();

                    startActivity(mapsIntent);
                    return true;

                case R.id.navigation_home:
                    startActivity(new Intent(NearbyResultActivity.this, MainActivity.class));
                    return true;
            }
            return false;
        }
    };

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = camHandler.createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CameraHandler.REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == CameraHandler.REQUEST_IMAGE_CAPTURE) && (resultCode == RESULT_OK)) {

            try {
                Bitmap pic = BitmapFactory.decodeFile(camHandler.getmCurrentPhotoPath());

                pic = Bitmap.createBitmap(pic);
                pic = Bitmap.createScaledBitmap(pic, pic.getWidth() / 2, pic.getHeight() / 2, false);

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                pic.compress(Bitmap.CompressFormat.JPEG, MainActivity.IMG_COMPRESSION_FACTOR, bytes);

                Log.d("PHOTO_SIZE", String.valueOf(bytes.size() / 1024));

                FileOutputStream fo = new FileOutputStream(new File(camHandler.getmCurrentPhotoPath()));
                fo.write(bytes.toByteArray());
                fo.close();

                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.putExtra("picPath", camHandler.getmCurrentPhotoPath());
                startActivity(mainIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
