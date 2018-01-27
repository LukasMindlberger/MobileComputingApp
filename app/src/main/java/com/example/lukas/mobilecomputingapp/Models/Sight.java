package com.example.lukas.mobilecomputingapp.Models;

import java.io.Serializable;


//import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Lukas on 20/11/2017.
 */

public class Sight implements Serializable{

    private int id;

    private String name;
    private String description;
    private String dateString;
    private LatLng location;
    private String picturePath;
    private boolean isFavorite;
    private String wikiUrl;
    private String address;

    public Sight(String name, String desc, String dateString, String picturePath, LatLng location) {
        this.name = name;
        this.description = desc;
        this.dateString = dateString;
        this.picturePath = picturePath;
        this.location = location;
        this.isFavorite = false;
    }

    public Sight() {

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

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String date) {
        this.dateString = date;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getWikiUrl() {
        return wikiUrl;
    }

    public void setWikiUrl(String wikiUrl) {
        this.wikiUrl = wikiUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
