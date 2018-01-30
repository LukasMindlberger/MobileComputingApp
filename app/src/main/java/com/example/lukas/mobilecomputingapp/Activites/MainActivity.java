package com.example.lukas.mobilecomputingapp.Activites;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.maps.model.LatLng;
import com.example.lukas.mobilecomputingapp.Adapters.SightAdapter;
import com.example.lukas.mobilecomputingapp.CameraHandler;
import com.example.lukas.mobilecomputingapp.DatabaseHandler;
import com.example.lukas.mobilecomputingapp.Models.LatLng;
import com.example.lukas.mobilecomputingapp.Models.Sight;
import com.example.lukas.mobilecomputingapp.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public static final int IMG_COMPRESSION_FACTOR = 95;
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final SimpleDateFormat FULL_TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private boolean mLocationPermissionGranted;
    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;

    private Toolbar mToolbar;
    private Spinner mSpinner;
    private BottomNavigationView mBottomNavView;

    private RecyclerView recyclerView;
    private SightAdapter sightAdapter;

    CameraHandler camHandler = new CameraHandler(this);
    ArrayList<Sight> sights = new ArrayList<>();
    DatabaseHandler db = new DatabaseHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        //setSupportActionBar(myToolbar);

        //mImageView = (ImageView) findViewById(R.id.image);
        //mTextMessage = (TextView) findViewById(R.id.message);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("sightseer");
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mBottomNavView = (BottomNavigationView) findViewById(R.id.navigation);
        mBottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        sights.addAll(db.getAllSights());

        SightAdapter.MyAdapterListener onClickListener = new SightAdapter.MyAdapterListener() {
            @Override
            public void favBtnOnClick(View v, int position) {
                Sight s = sights.get(position);
                s.setFavorite(!s.isFavorite());
                db.updateSight(s);
            }

            @Override
            public void mapBtnOnClick(View v, int position) {
                Intent mapsIntent = new Intent(MainActivity.this, LocationHistoryActivity.class);
                mapsIntent.putExtra("sights", sights);
                mapsIntent.putExtra("center", sights.get(position).getLocation());
                startActivity(mapsIntent);
            }

            @Override
            public void shareBtnOnClick(View v, int position) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(sights.get(position).getPicturePath()));
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, "Share pictures with your friends"));
            }

            @Override
            public void nearbyBtnOnClick(View v, int position) {
                Intent nearbyIntent = new Intent(MainActivity.this, NearbyResultActivity.class);
                nearbyIntent.putExtra("sight", sights.get(position));
                startActivity(nearbyIntent);
            }
        };

        sightAdapter = new SightAdapter(this, sights, onClickListener);
        recyclerView = (RecyclerView) findViewById(R.id.sight_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(30);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(sightAdapter);

        ArrayAdapter<String> myAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.custom_spinner_item,
                getResources().getStringArray(R.array.sortModes));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(myAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayList<Sight> s = sortSightsList(mSpinner.getSelectedItem().toString(), sights);
                sights.clear();
                sights.addAll(s);
                sightAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //Do nothing
            }
        });

        registerReceiver(Updated, new IntentFilter("data_changed"));

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLocationPermission();
        getDeviceLocation();
    }

    protected void onResume() {
        super.onResume();
        mBottomNavView.setSelectedItemId(R.id.navigation_home);
        registerReceiver(Updated, new IntentFilter("data_changed"));
        if(getIntent().hasExtra("picPath")){
            camHandler.setmCurrentPhotoPath(getIntent().getStringExtra("picPath"));
            getIntent().removeExtra("picPath");
            detectLandmarksHTTP();
        }
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(Updated);
    }

    protected void onRestart() {
        super.onRestart();
        mBottomNavView.setSelectedItemId(R.id.navigation_home);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("picPath", camHandler.getmCurrentPhotoPath());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mBottomNavView.setSelectedItemId(R.id.navigation_home);
        if (camHandler!=null && savedInstanceState.containsKey("picPath")){
            camHandler.setmCurrentPhotoPath(savedInstanceState.getString("picPath"));
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_cam:
                    dispatchTakePictureIntent();
                    camHandler.galleryAddPic();
                    mBottomNavView.setSelectedItemId(R.id.navigation_home);
                    return true;

                case R.id.navigation_map:
                    //mTextMessage.setText("Map");
                    Intent mapsIntent = new Intent(MainActivity.this, LocationHistoryActivity.class);
                    mapsIntent.putExtra("sights", sights);
                    startActivity(mapsIntent);

                    mBottomNavView.setSelectedItemId(R.id.navigation_home);
                    return true;

                case R.id.navigation_home:
                    //mTextMessage.setText("Home");
                    return true;
            }
            return false;
        }
    };

    //handles all my sorting needs
    private ArrayList<Sight> sortSightsList(String sort, ArrayList<Sight> sights) {
        ArrayList<Sight> sortedSights = new ArrayList<>();
        sortedSights.addAll(sights);

        switch (sort.toLowerCase().trim()) {

            case "recent":
                Collections.sort(sortedSights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight1, Sight sight2) {
                        if (sight1.getDateString() == null || sight2.getDateString() == null) {
                            Log.i("RECENT_SORT", "null exception");
                            return 0;
                        }

                        try {
                            Date d1 = FULL_TIME_FORMAT.parse(sight1.getDateString());
                            Date d2 = FULL_TIME_FORMAT.parse(sight2.getDateString());
                            //most recent on top
                            return d2.compareTo(d1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                            return 0;
                        }
                    }
                });
                break;

            case "oldest":
                Collections.sort(sortedSights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight1, Sight sight2) {
                        if (sight1.getDateString() == null || sight2.getDateString() == null) {
                            Log.i("OLDEST_SORT", "null exception");
                            return 0;
                        }

                        try {
                            Date d1 = FULL_TIME_FORMAT.parse(sight1.getDateString());
                            Date d2 = FULL_TIME_FORMAT.parse(sight2.getDateString());
                            //most recent on top
                            return d1.compareTo(d2);
                        } catch (ParseException e) {
                            e.printStackTrace();
                            return 0;
                        }
                    }
                });
                break;

            case "alphabetical":
                Collections.sort(sortedSights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight1, Sight sight2) {
                        if (sight1.getName() == null || sight2.getName() == null ||
                                sight1.getName().isEmpty() || sight2.getName().isEmpty()) {
                            Log.i("ALPHABETICAL_SORT", "null or empty exception");
                            return 0;
                        }
                        return sight1.getName().compareToIgnoreCase(sight2.getName());
                    }
                });
                break;

            case "distance":
                Collections.sort(sortedSights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight1, Sight sight2) {
                        if (mLastKnownLocation == null || sight1.getLocation() == null || sight2.getLocation() == null) {
                            Log.i("LOCATION_SORT", "null exception");
                            return 0;
                        }

                        Location sight1Location = new Location("Sight1 Location");
                        sight1Location.setLatitude(sight1.getLocation().getLatitude());
                        sight1Location.setLongitude(sight1.getLocation().getLongitude());

                        Location sight2Location = new Location("Sight2 Location");
                        sight2Location.setLatitude(sight2.getLocation().getLatitude());
                        sight2Location.setLongitude(sight2.getLocation().getLongitude());

                        float distanceToSight1 = mLastKnownLocation.distanceTo(sight1Location);
                        float distanceToSight2 = mLastKnownLocation.distanceTo(sight2Location);

                        return Math.round(distanceToSight1 - distanceToSight2);
                    }
                });
                break;

            case "favorites":
                Collections.sort(sortedSights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight1, Sight sight2) {
                        return Boolean.compare(sight2.isFavorite(), sight1.isFavorite());
                    }
                });
                break;
        }

        return sortedSights;
    }

    //Decides what to do when data was updated
    private final BroadcastReceiver Updated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            ArrayList<Sight> s = sortSightsList(mSpinner.getSelectedItem().toString(), db.getAllSights());
            sights.clear();
            sights.addAll(s);
            sightAdapter.notifyDataSetChanged();
        }
    };

    private void detectLandmarksHTTP() {
        try {
            try {
                run();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void run() throws IOException, JSONException {

        InputStream inputStream = new FileInputStream(camHandler.getmCurrentPhotoPath());//You can get an inputStream using any IO API
        byte[] bytes;
        byte[] buffer = new byte[16384];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bytes = output.toByteArray();

        String encodedImg = Base64.encodeToString(bytes, Base64.DEFAULT);
        //encodedImg = "placeholder for encoding";
        //System.out.println(encodedImg);

        //JSON Request Body basteln
        JSONObject img = new JSONObject();
        img.put("content", encodedImg);
        JSONObject obj = new JSONObject();
        obj.put("type", "LANDMARK_DETECTION");

        JSONArray features = new JSONArray();
        features.put(obj);

        JSONObject dataObj = new JSONObject();
        dataObj.put("image", img);
        dataObj.put("features", features);

        JSONArray arr = new JSONArray();
        arr.put(dataObj);

        JSONObject requests = new JSONObject();
        requests.put("requests", arr);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, requests.toString());
        Request request = new Request.Builder()
                .url("https://vision.googleapis.com/v1/images:annotate?key=AIzaSyDAYYDLWz9Nr9aqVmvZylkFhfUi1dI8a80")
                .post(body)
                .build();

        //LATENCY MEASUREMENT -- START
        final long startTime = System.nanoTime();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    //LATENCY MEASUREMENT -- END
                    long elapsedTimeMS = (System.nanoTime() - startTime) / 1000000;
                    Log.i("LATENCY - CLOUD VISION", String.valueOf(elapsedTimeMS)+ "ms");

                    // use successful result
                    final String myResponse = response.body().string();
                    try {
                        JSONObject result = new JSONObject(myResponse);

                        JSONObject responses = result.getJSONArray("responses").getJSONObject(0).getJSONArray("landmarkAnnotations").getJSONObject(0);
                        JSONObject sightLocationJSON = responses.getJSONArray("locations").getJSONObject(0).getJSONObject("latLng");

                        final String sightName = responses.getString("description");
                        final LatLng sightLocation = new LatLng(round(Double.parseDouble(sightLocationJSON.getString("latitude")), 2), round(Double.parseDouble(sightLocationJSON.getString("longitude")), 2));

                        Log.d("SIGHT NAME AND LOCATION", sightName + " @ " + sightLocation.toString());

                        Sight sight = new Sight(sightName, null, FULL_TIME_FORMAT.format(new Date()), camHandler.getmCurrentPhotoPath(), sightLocation);
                        //sights.add(sight);

                        db.addSight(sight);

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //mTextMessage.setText(sightName + " @ " + sightLocation.toString());
                                //System.out.println(myResponse);
                                Toast.makeText(MainActivity.this, "Success! :)",
                                        Toast.LENGTH_LONG).show();
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                queryUserForSightNameAndSaveSight();
                            }
                        });
                    }
                }
            }
        });
    }

    private void queryUserForSightNameAndSaveSight() {

        final AlertDialog.Builder getSightNameBuilder = new AlertDialog.Builder(this);
        getSightNameBuilder.setTitle("Enter the sight's name:");

        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        getSightNameBuilder.setView(input);

        getSightNameBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Adding manually entered sight at current position
                String sightName = input.getText().toString();
                LatLng sightLocation = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                Sight sight = new Sight(sightName, null, FULL_TIME_FORMAT.format(new Date()), camHandler.getmCurrentPhotoPath(), sightLocation);
                db.addSight(sight);
            }
        });
        getSightNameBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog.Builder decideBuilder = new AlertDialog.Builder(this);
        decideBuilder.setTitle("Unfortunately no matches :(");
        decideBuilder.setMessage("Do you want to manually enter a name?");
        decideBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                getSightNameBuilder.show();
            }
        });
        decideBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        decideBuilder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if ((requestCode == CameraHandler.REQUEST_IMAGE_CAPTURE) && (resultCode == RESULT_OK)) {

            try {
                Bitmap pic = BitmapFactory.decodeFile(camHandler.getmCurrentPhotoPath());

                pic = Bitmap.createBitmap(pic);
                pic = Bitmap.createScaledBitmap(pic, pic.getWidth() / 2, pic.getHeight() / 2, false);

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                pic.compress(Bitmap.CompressFormat.JPEG, IMG_COMPRESSION_FACTOR, bytes);

                Log.d("PHOTO_SIZE", String.valueOf(bytes.size() / 1024));

                FileOutputStream fo = new FileOutputStream(new File(camHandler.getmCurrentPhotoPath()));
                fo.write(bytes.toByteArray());
                fo.close();

                detectLandmarksHTTP();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

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
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {

                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            Log.d("LOCATION_FINDER", task.getResult().toString());
                        } else {
                            Log.d("LOCATION_FINDER", "Current location is null.");
                            Log.e("LOCATION_FINDER", "Exception: %s", task.getException());
                        }
                    }
                });

            } else {
                // Add a marker in Linz and move camera there
                Log.i("LOCATION_FINDER", "Location permission not granted");
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public static void setCheckable(BottomNavigationView view, boolean checkable) {
        final Menu menu = view.getMenu();
        for(int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setCheckable(checkable);
        }
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
