package com.example.lukas.mobilecomputingapp.Activites;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

//import com.google.android.gms.maps.model.LatLng;

import com.example.lukas.mobilecomputingapp.CameraHandler;
import com.example.lukas.mobilecomputingapp.DatabaseHandler;
import com.example.lukas.mobilecomputingapp.Models.Sight;
import com.example.lukas.mobilecomputingapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SinglePictureActivity extends AppCompatActivity {
    private CameraHandler camHandler = new CameraHandler(this);
    private BottomNavigationView mBottomNavView;

    private TextView mTitleText;
    private TextView mWikiText;
    private TextView mWikiUrlText;

    private Sight sight;

    DatabaseHandler db = new DatabaseHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_picture);

        Bundle b = getIntent().getExtras();
        sight = (Sight) b.getSerializable("Sight");

        String sightName = sight.getName();
        String picLocation = sight.getPicturePath();
        String wikiDescription = sight.getDescription();
        String wikiUrl = sight.getWikiUrl();

        mBottomNavView = (BottomNavigationView) findViewById(R.id.navigation);
        mBottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        MainActivity.setCheckable(mBottomNavView,false);

        ImageView mSightImg = (ImageView) findViewById(R.id.singleImgView);
        mWikiText = (TextView) findViewById(R.id.wikiTextView);
        mWikiUrlText = (TextView) findViewById(R.id.wikiLinkTextView);
        mTitleText = (TextView) findViewById(R.id.titleTextView);

        Button mMapBtn = (Button) findViewById(R.id.MapBtn);
        Button mShareBtn = (Button) findViewById(R.id.ShareBtn);
        Button mNearbyBtn = (Button) findViewById(R.id.NearbyBtn);

        mMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Sight> sights = new ArrayList<>();
                sights.add(sight);

                Intent mapsIntent = new Intent(SinglePictureActivity.this, LocationHistoryActivity.class);
                mapsIntent.putExtra("sights", sights);
                mapsIntent.putExtra("center",sight.getLocation());
                startActivity(mapsIntent);
            }
        });
        mShareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(sight.getPicturePath()));
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent,"Share pictures with your friends"));
            }
        });
        mNearbyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nearbyIntent = new Intent(SinglePictureActivity.this, NearbyResultActivity.class);
                nearbyIntent.putExtra("sight", sight);
                startActivity(nearbyIntent);
            }
        });

        mSightImg.setImageBitmap(BitmapFactory.decodeFile(picLocation));
        mTitleText.setText(sightName);
        mWikiText.setText("querying wikipiedia...");

        if (wikiDescription ==null || wikiDescription.isEmpty()){
            queryAndSetWikiInfo(sightName);
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                mWikiText.setText(Html.fromHtml(wikiDescription,Html.FROM_HTML_MODE_COMPACT));
            }else{
                mWikiText.setText(Html.fromHtml(wikiDescription));
            }
            if (wikiUrl !=null && !wikiUrl.isEmpty()){
                mWikiUrlText.setMovementMethod(LinkMovementMethod.getInstance());
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    mWikiUrlText.setText(Html.fromHtml(wikiUrl,Html.FROM_HTML_MODE_COMPACT));
                }else{
                    mWikiUrlText.setText(Html.fromHtml(wikiUrl));
                }
            }
        }
    }

    protected void onResume() {
        super.onResume();
        MainActivity.setCheckable(mBottomNavView,false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("picPath", camHandler.getmCurrentPhotoPath());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (camHandler!=null && savedInstanceState.containsKey("picPath")){
            camHandler.setmCurrentPhotoPath(savedInstanceState.getString("picPath"));
        }
    }

    private void queryAndSetWikiInfo(final String sightName){

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://en.wikipedia.org/w/api.php?format=json&action=query&list=search&srsearch=" + sightName)
                .build();

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
                    // use successful result
                    final String myResponse = response.body().string();

                    try {
                        JSONArray searchResults = new JSONObject(myResponse).getJSONObject("query").getJSONArray("search");

                        if (searchResults.length() > 0) {
                            final int pageID = searchResults.getJSONObject(0).getInt("pageid");

                            OkHttpClient client2 = new OkHttpClient();
                            Request request2 = new Request.Builder()
                                    //.url("https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&pageids=" + pageID)
                                    .url("https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro&pageids=" + pageID)
                                    .build();

                            client2.newCall(request2).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    e.printStackTrace();
                                    call.cancel();
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    if(!response.isSuccessful()){
                                        throw new IOException("Unexpected code " + response);
                                    }else{
                                        final String myResponse2 = response.body().string();

                                        try {
                                            JSONObject result = new JSONObject(myResponse2).getJSONObject("query").getJSONObject("pages").getJSONObject(String.valueOf(pageID));

                                            final String title = result.getString("title");
                                            final String intro = result.getString("extract");
                                            final String wikiUrlHtml = "<a href=http://en.wikipedia.org/wiki/" + title.replace(" ", "_") + ">visit wikipedia.org</href>";

                                            sight.setDescription(intro);
                                            sight.setWikiUrl(wikiUrlHtml);
                                            db.updateSight(sight);

                                            SinglePictureActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mTitleText.setText(title);
                                                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                                                        mWikiText.setText(Html.fromHtml(intro,Html.FROM_HTML_MODE_COMPACT));
                                                        mWikiUrlText.setMovementMethod(LinkMovementMethod.getInstance());
                                                        mWikiUrlText.setText(Html.fromHtml(wikiUrlHtml,Html.FROM_HTML_MODE_COMPACT));
                                                    }else{
                                                        mWikiText.setText(Html.fromHtml(intro));
                                                        mWikiUrlText.setMovementMethod(LinkMovementMethod.getInstance());
                                                        mWikiUrlText.setText(Html.fromHtml(wikiUrlHtml));
                                                    }
                                                }
                                            });

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });

                        }else{
                            Log.d("WIKI QUERY", "Nothing found for " + sightName);
                        }

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
            MainActivity.setCheckable(mBottomNavView,true);
            switch (item.getItemId()) {
                case R.id.navigation_cam:
                    dispatchTakePictureIntent();
                    camHandler.galleryAddPic();
                    return true;

                case R.id.navigation_map:
                    Intent mapsIntent = new Intent(SinglePictureActivity.this, LocationHistoryActivity.class);
                    mapsIntent.putExtra("sights", db.getAllSights());
                    mapsIntent.putExtra("center", sight.getLocation());
                    startActivity(mapsIntent);
                    return true;

                case R.id.navigation_home:
                    startActivity(new Intent(SinglePictureActivity.this,MainActivity.class));
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
