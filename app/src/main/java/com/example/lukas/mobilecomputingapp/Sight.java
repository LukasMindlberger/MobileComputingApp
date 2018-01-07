package com.example.lukas.mobilecomputingapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import java.io.Serializable;


//import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Lukas on 20/11/2017.
 */

public class Sight implements Serializable{

    private String name;
    private String description;
    private String date;
    private LatLng location;

    private String picturePath;
    //private Bitmap picture;

    public Sight() {
    }

    public Sight(String name, String desc, String date, String picturePath, LatLng location) {
        this.name = name;
        this.description = desc;
        this.date = date;
        this.picturePath = picturePath;
        this.location = location;
/*
        if (picturePath!=null && !picturePath.isEmpty()){
            this.picture = BitmapFactory.decodeFile(picturePath);
        }
        */
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }
/*
    public Bitmap getPicture() {
        return picture;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }
*/
}
