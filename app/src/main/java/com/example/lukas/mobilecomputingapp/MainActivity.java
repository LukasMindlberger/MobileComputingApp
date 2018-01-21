package com.example.lukas.mobilecomputingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.JsonWriter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
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

    private Toolbar mToolbar;
    private Spinner mSpinner;

    private TextView mTextMessage;
    private ImageView mImageView;
    private BottomNavigationView mBottomNavView;

    private RecyclerView recyclerView;
    private SightAdapter sightAdapter;

    String mCurrentPhotoPath;

    ArrayList<Sight> sights = new ArrayList<>();

    // Write a message to the database
    //FirebaseDatabase database = FirebaseDatabase.getInstance();
    //DatabaseReference locationRef = database.getReference("sights/" + uID);

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
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mBottomNavView = (BottomNavigationView) findViewById(R.id.navigation);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        sights.addAll(db.getAllSights());
        sightAdapter = new SightAdapter(sights);
        recyclerView = (RecyclerView) findViewById(R.id.sight_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(sightAdapter);
        registerReceiver(Updated,new IntentFilter("data_changed"));

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(MainActivity.this,
                R.layout.custom_spinner_item,
                getResources().getStringArray(R.array.sortModes));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(myAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(MainActivity.this, mSpinner.getSelectedItem().toString(),Toast.LENGTH_LONG).show();

                ArrayList<Sight> s = sortSightsList(mSpinner.getSelectedItem().toString(), sights);
                sights.clear();
                sights.addAll(s);

                Log.i("DATESORT", String.valueOf(s.size()));
                for (Sight s1:s) {
                    Log.i("SORT", s1.getName());
                }
                sightAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    private ArrayList<Sight> sortSightsList(String sort, ArrayList<Sight> sights) {
        ArrayList<Sight> sortedSights = new ArrayList<>();
        sortedSights.addAll(sights);

        switch (sort.toLowerCase()){
            case "recent":
                Log.i("DATESORT", String.valueOf(sights.size()));
                Collections.sort(sortedSights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight1, Sight sight2) {
                        if (sight1.getDateString() == null || sight2.getDateString() == null) {
                            Log.i("EXC", "0 exception");
                            return 0;
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                        try {
                            Date d1 = sdf.parse(sight1.getDateString());
                            Date d2 = sdf.parse(sight2.getDateString());
                            Log.i("sort", String.valueOf(d1.compareTo(d2)));
                            //most recent on top
                            return d2.compareTo(d1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                            Log.i("EXC", "parseexception");
                            return 0;
                        }
                    }
                });
                break;
            case "alphabetical":
                Collections.sort(sights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight, Sight t1) {
                        return 0;
                    }
                });
                break;
            case "favorites":
                Collections.sort(sights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight, Sight t1) {
                        return 0;
                    }
                });
                break;
            case "location":
                Collections.sort(sights, new Comparator<Sight>() {
                    @Override
                    public int compare(Sight sight, Sight t1) {
                        return 0;
                    }
                });
                break;
        }

        return sortedSights;
    }

    private final BroadcastReceiver Updated= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            sights.clear();
            sights.addAll(db.getAllSights());
            sightAdapter.notifyDataSetChanged();
        }
    };

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

                    Bundle extra = new Bundle();
                    //extra.putSerializable("sights",sights);
                    //mapsIntent.putExtra("s", extra);

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
                        final LatLng sightLocation = new LatLng(round(Double.parseDouble(sightLocationJSON.getString("latitude")),2), round(Double.parseDouble(sightLocationJSON.getString("longitude")),2));

                        Log.d("SIGHT NAME AND LOCATION", sightName + " @ " + sightLocation.toString());

                        Sight sight = new Sight(sightName,null,new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()),mCurrentPhotoPath,sightLocation);
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
                                Toast.makeText(MainActivity.this, "Could not find a matching sight or landmark!",
                                        Toast.LENGTH_LONG).show();

                                //TODO give user possibility to enter sight name themselves and take gps coordinates

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

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
