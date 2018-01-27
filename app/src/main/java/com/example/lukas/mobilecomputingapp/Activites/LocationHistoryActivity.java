package com.example.lukas.mobilecomputingapp.Activites;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.MenuItem;

import com.example.lukas.mobilecomputingapp.CameraHandler;
import com.example.lukas.mobilecomputingapp.Models.Sight;
import com.example.lukas.mobilecomputingapp.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class LocationHistoryActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = LocationHistoryActivity.class.getSimpleName();
    private GoogleMap mMap;

    private CameraHandler camHandler = new CameraHandler(this);
    private BottomNavigationView mBottomNavView;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 7;
    private final LatLng mDefaultLocation = new LatLng(48.306940, 14.285830);
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private ArrayList<Sight> sights;

    private ArrayList<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_history);

        mBottomNavView = (BottomNavigationView) findViewById(R.id.navigation);
        mBottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mBottomNavView.setSelectedItemId(R.id.navigation_map);

        sights = (ArrayList<Sight>) getIntent().getSerializableExtra("sights");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    protected void onResume() {
        super.onResume();
        mBottomNavView.setSelectedItemId(R.id.navigation_map);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Linz.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        for(Sight sight: sights){

            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            Bitmap bmp = Bitmap.createBitmap(400, 400, conf);
            Canvas canvas1 = new Canvas(bmp);

            // paint defines the text color, stroke width and size
            Paint color = new Paint();
            color.setTextSize(40);
            color.setColor(Color.BLACK);

            // modify canvas
            canvas1.drawBitmap(getRoundedCornerBitmap(decodeFile(sight.getPicturePath()),400), 0,0, color);
            canvas1.drawText(sight.getName(),10,300,color);

            markers.add(mMap.addMarker(new MarkerOptions()
                .position(new com.google.android.gms.maps.model.LatLng(sight.getLocation().getLatitude(),sight.getLocation().getLongitude()))
                .title(sight.getName())
                //.icon(BitmapDescriptorFactory.fromBitmap(bmp))
                ));
        }

        mMap.setOnInfoWindowClickListener(this);

        // Prompt the user for permission.
        getLocationPermission();
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
        // Check if we should center on a sight or the current location
        if (getIntent().hasExtra("center")){
            com.example.lukas.mobilecomputingapp.Models.LatLng extraLatLng = (com.example.lukas.mobilecomputingapp.Models.LatLng) getIntent().getSerializableExtra("center");
            LatLng latLng = new LatLng(extraLatLng.getLatitude(),extraLatLng.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM+3));
            for (Marker m:markers) {
                if(m.getPosition().latitude == latLng.latitude && m.getPosition().longitude == latLng.longitude){
                    m.showInfoWindow();
                }
            }
            getIntent().removeExtra("center");

        }else{
            if(mLastKnownLocation != null){
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(mLastKnownLocation.getLatitude(),
                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
            }else{
                mMap.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        }
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private Bitmap decodeFile(String imgPath)
    {
        Bitmap b = null;
        int max_size = 450;
        File f = new File(imgPath);
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();
            int scale = 1;
            if (o.outHeight > max_size || o.outWidth > max_size)
            {
                scale = (int) Math.pow(2, (int) Math.ceil(Math.log(max_size / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis,new Rect(50,50,50,50), o2);
            fis.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return b;
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
                            mLastKnownLocation = task.getResult();
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
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
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    @Override
    public void onInfoWindowClick(Marker marker) {
        for(Sight sight: sights){
            if (sight.getName().equals(marker.getTitle())){
                Intent singlePicIntent = new Intent(LocationHistoryActivity.this, SinglePictureActivity.class);
                singlePicIntent.putExtra("Sight", sight);
                startActivity(singlePicIntent);
            }
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
                    return true;

                case R.id.navigation_map:
                    //Do nothing, we're already on the map
                    return true;

                case R.id.navigation_home:
                    startActivity(new Intent(LocationHistoryActivity.this,MainActivity.class));
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == CameraHandler.REQUEST_IMAGE_CAPTURE) && (resultCode == RESULT_OK)) {

            try {
                Bitmap pic = BitmapFactory.decodeFile(camHandler.getmCurrentPhotoPath());

                pic = Bitmap.createBitmap(pic);
                pic = Bitmap.createScaledBitmap(pic, pic.getWidth() / 2, pic.getHeight() / 2, false);

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                pic.compress(Bitmap.CompressFormat.JPEG, 80, bytes);

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







}
