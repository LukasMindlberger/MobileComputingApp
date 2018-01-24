package com.example.lukas.mobilecomputingapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.lukas.mobilecomputingapp.MainActivity.JSON;

public class SinglePictureActivity extends AppCompatActivity {

    private BottomNavigationView mBottomNavView;

    private TextView mTitleText;
    private TextView mWikiText;
    private ImageView mSightImg;

    private Button mShareBtn;
    private Button mMapBtn;
    private Button mSimilarBtn;

    private String picLocation;
    private String sightName;

    private String wikiDescription;

    private Sight sight;

    DatabaseHandler db = new DatabaseHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_picture);

        Bundle b = getIntent().getExtras();
        sight = (Sight) b.getSerializable("Sight");

        sightName = sight.getName();
        picLocation = sight.getPicturePath();
        wikiDescription = sight.getDescription();

        //mBottomNavView = (BottomNavigationView) findViewById(R.id.navigation);
        //BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mSightImg = (ImageView) findViewById(R.id.singleImgView);
        mWikiText = (TextView) findViewById(R.id.wikiTextView);
        mTitleText = (TextView) findViewById(R.id.titleTextView);

        mMapBtn = (Button) findViewById(R.id.MapBtn);
        mShareBtn = (Button) findViewById(R.id.ShareBtn);
        mSimilarBtn = (Button) findViewById(R.id.SimilarBtn);

        mMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Sight> sights = new ArrayList<>();
                sights.add(sight);

                Intent mapsIntent = new Intent(SinglePictureActivity.this, locationHistoryActivity.class);
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
        mSimilarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mSightImg.setImageBitmap(BitmapFactory.decodeFile(picLocation));
        mTitleText.setText(sightName);
        mWikiText.setText("querying wikipiedia...");

        if (wikiDescription==null || wikiDescription.isEmpty()){
            getWikiInfo(sightName);
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                mWikiText.setText(Html.fromHtml(wikiDescription,Html.FROM_HTML_MODE_COMPACT));
            }else{
                mWikiText.setText(Html.fromHtml(wikiDescription));
            }
        }
    }

    private void getWikiInfo(final String sightName){

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

                                            sight.setDescription(intro);
                                            db.updateSight(sight);

                                            Log.d("TITLE", title);
                                            Log.d("INTRO", intro);

                                            SinglePictureActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    mTitleText.setText(title);

                                                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                                                        mWikiText.setText(Html.fromHtml(intro,Html.FROM_HTML_MODE_COMPACT));
                                                    }else{
                                                        mWikiText.setText(Html.fromHtml(intro));
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
            switch (item.getItemId()) {
                case R.id.navigation_cam:
                    mBottomNavView.setSelectedItemId(R.id.navigation_home);
                    return true;

                case R.id.navigation_map:
                    //mTextMessage.setText("Map");
                    Intent mapsIntent = new Intent(SinglePictureActivity.this, locationHistoryActivity.class);
                    startActivity(mapsIntent);

                    mBottomNavView.setSelectedItemId(R.id.navigation_home);
                    return true;

                case R.id.navigation_home:
                    //mTextMessage.setText("Home");
                    startActivity(new Intent(SinglePictureActivity.this,MainActivity.class));
                    return true;
            }
            return false;
        }
    };
}
