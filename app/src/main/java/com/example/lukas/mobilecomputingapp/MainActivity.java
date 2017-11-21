package com.example.lukas.mobilecomputingapp;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.JsonWriter;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private TextView mTextMessage;
    private ImageView mImageView;
    private BottomNavigationView mBottomNavView;

    private RecyclerView recyclerView;
    private SightAdapter sightAdapter;

    String mCurrentPhotoPath;
    List<Sight> sights = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mImageView = (ImageView) findViewById(R.id.image);
        //mTextMessage = (TextView) findViewById(R.id.message);
        mBottomNavView = (BottomNavigationView) findViewById(R.id.navigation);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //sights.add(new Sight("TestName1", "TestDesc1", "10.10.2011", "/storage/emulated/0/Android/data/com.example.lukas.mobilecomputingapp/files/Pictures/JPEG_20171121_195428_810594566.jpg", new LatLng(49, 49)));

        sightAdapter = new SightAdapter(sights);

        recyclerView = (RecyclerView) findViewById(R.id.sight_list);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(sightAdapter);
    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_cam:
                    dispatchTakePictureIntent();
                    galleryAddPic();
                    mBottomNavView.setSelectedItemId(R.id.navigation_home);
                    return true;

                case R.id.navigation_map:
                    //mTextMessage.setText("Map");
                    Intent mapsIntent = new Intent(MainActivity.this, locationHistoryActivity.class);
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


        InputStream inputStream = new FileInputStream(mCurrentPhotoPath);//You can get an inputStream using any IO API
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

        //System.out.println(requests.toString());

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, requests.toString());
        Request request = new Request.Builder()
                //Todo read best practices on API key handling
                .url("https://vision.googleapis.com/v1/images:annotate?key=AIzaSyDAYYDLWz9Nr9aqVmvZylkFhfUi1dI8a80")
                .post(body)
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
                        JSONObject result = new JSONObject(myResponse);

                        JSONObject responses = result.getJSONArray("responses").getJSONObject(0).getJSONArray("landmarkAnnotations").getJSONObject(0);
                        JSONObject sightLocationJSON = responses.getJSONArray("locations").getJSONObject(0).getJSONObject("latLng");

                        final String sightName = responses.getString("description");
                        final LatLng sightLocation = new LatLng(Double.parseDouble(sightLocationJSON.getString("latitude")), Double.parseDouble(sightLocationJSON.getString("longitude")));

                        Log.d("SIGHT NAME AND LOCATION", sightName + " @ " + sightLocation.toString());


                        Sight sight = new Sight(sightName,null,new SimpleDateFormat("dd.MM.yyyy").format(new Date()),mCurrentPhotoPath,sightLocation);
                        sights.add(sight);

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //mTextMessage.setText(sightName + " @ " + sightLocation.toString());
                                //System.out.println(myResponse);
                                sightAdapter.notifyDataSetChanged();
                                Toast.makeText(MainActivity.this, "Success! :)",
                                        Toast.LENGTH_LONG).show();
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Could not find a matching sight or landmark!",
                                        Toast.LENGTH_LONG).show();
                                //mTextMessage.setText("Could not find sight!");
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE) && (resultCode == RESULT_OK)) {
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            //mImageView.setImageBitmap(imageBitmap);

            try {
                Bitmap pic = BitmapFactory.decodeFile(mCurrentPhotoPath);

                pic = Bitmap.createBitmap(pic);
                pic = Bitmap.createScaledBitmap(pic, pic.getWidth() / 2, pic.getHeight() / 2, false);

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                pic.compress(Bitmap.CompressFormat.JPEG, 80, bytes);

                Log.d("SIZE", String.valueOf(bytes.size() / 1024));

                File f = new File(mCurrentPhotoPath);
                f.createNewFile();
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
                fo.close();

                Log.d("Path", mCurrentPhotoPath);

                /*
                mImageView.setImageURI(Uri.parse(mCurrentPhotoPath));
                if (f.exists()) {
                    String size = Long.toString(f.length() / 1024) + " KB";
                    mTextMessage.setText(size);
                } else {
                    mTextMessage.setText("Tough luck");
                }
                */

                //detectLandmarks(mCurrentPhotoPath, System.out);
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
                photoFile = createImageFile();
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

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
}
