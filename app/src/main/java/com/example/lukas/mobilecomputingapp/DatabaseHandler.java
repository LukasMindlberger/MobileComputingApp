package com.example.lukas.mobilecomputingapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lukas on 21/01/2018.
 */

public class DatabaseHandler extends SQLiteOpenHelper {

    private Context ctx;

    private static final int DATABASE_VERSION = 2;

    // Contacts table name
    private static final String TABLE_SIGHTS = "sights";

    private static final String DATABASE_NAME = "sightsManager";

    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_WIKI_DESC = "wiki_desc";
    private static final String KEY_DATE_STR = "date";
    private static final String KEY_LAT = "latitude";
    private static final String KEY_LONG = "longitude";
    private static final String KEY_PIC_PATH = "picture_path";
    private static final String KEY_IS_FAV = "favorite";

    private static final String KEY_DATE = "date";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.ctx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_SIGHTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_WIKI_DESC + " TEXT,"
                + KEY_DATE_STR + " TEXT,"
                + KEY_LAT + " DOUBLE,"
                + KEY_LONG + " DOUBLE,"
                + KEY_PIC_PATH + " TEXT,"
                + KEY_IS_FAV + " INTEGER"
                + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SIGHTS);

        // Create tables again
        onCreate(db);
    }

    //Add Sight
    public void addSight(Sight s) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, s.getName());
        values.put(KEY_WIKI_DESC, s.getDescription());
        values.put(KEY_DATE_STR, s.getDateString());
        values.put(KEY_LAT, s.getLocation().getLatitude());
        values.put(KEY_LONG, s.getLocation().getLongitude());
        values.put(KEY_PIC_PATH, s.getPicturePath());
        values.put(KEY_IS_FAV, s.isFavorite());

        // Inserting Row
        db.insert(TABLE_SIGHTS, null, values);
        // Closing database connection
        db.close();

        Intent i = new Intent("data_changed");
        ctx.sendBroadcast(i);
    }

    public ArrayList<Sight> getAllSights() {

        ArrayList<Sight> sightList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_SIGHTS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Sight s = new Sight();

                s.setId(Integer.parseInt(cursor.getString(0)));
                s.setName(cursor.getString(1));
                s.setDescription(cursor.getString(2));
                s.setDateString(cursor.getString(3));
                s.setLocation(new LatLng(cursor.getDouble(4), cursor.getDouble(5)));
                s.setPicturePath(cursor.getString(6));
                s.setFavorite(cursor.getInt(7)>0);

                sightList.add(s);
            } while (cursor.moveToNext());
        }

        return sightList;
    }

    public int getSightsCount(){
        String countQuery = "SELECT  * FROM " + TABLE_SIGHTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    };

    public int updateSight(Sight s) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, s.getName());
        values.put(KEY_WIKI_DESC, s.getDescription());
        values.put(KEY_DATE_STR, s.getDateString());
        values.put(KEY_LAT, s.getLocation().getLatitude());
        values.put(KEY_LONG, s.getLocation().getLongitude());
        values.put(KEY_PIC_PATH, s.getPicturePath());
        values.put(KEY_IS_FAV, s.isFavorite());

        int retVal = db.update(TABLE_SIGHTS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(s.getId())});
        db.close();

        Intent i = new Intent("data_changed");
        ctx.sendBroadcast(i);

        return retVal;
    }

    public void deleteSight(Sight s) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SIGHTS, KEY_ID + " = ?",
                new String[]{String.valueOf(s.getId())});
        db.close();
        Intent i = new Intent("data_changed");
        ctx.sendBroadcast(i);
    }

    public void deleteAll(){
        String deleteQuery = "DELETE FROM " + TABLE_SIGHTS;
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(deleteQuery);
        db.close();
        Intent i = new Intent("data_changed");
        ctx.sendBroadcast(i);
    }

}
