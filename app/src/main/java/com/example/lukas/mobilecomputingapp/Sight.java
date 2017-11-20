package com.example.lukas.mobilecomputingapp;

import android.location.Location;

/**
 * Created by Lukas on 20/11/2017.
 */

public class Sight {

    private String name;
    private String description;
    private String date;
    private String picturePath;
    private Location location;

    public Sight() {
    }

    public Sight(String name, String desc, String date, String picturePath, Location location) {
        this.name = name;
        this.description = desc;
        this.date = date;
        this.picturePath = picturePath;
        this.location = location;
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
