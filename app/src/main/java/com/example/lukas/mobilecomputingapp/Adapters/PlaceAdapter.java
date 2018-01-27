package com.example.lukas.mobilecomputingapp.Adapters;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.lukas.mobilecomputingapp.Activites.MainActivity;
import com.example.lukas.mobilecomputingapp.Activites.NearbyResultActivity;
import com.example.lukas.mobilecomputingapp.R;
import com.example.lukas.mobilecomputingapp.Models.Sight;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by Lukas on 25/01/2018.
 */

public class PlaceAdapter extends ArrayAdapter<Sight> {
    private Location initLoc;
    private Context ctx;

    public PlaceAdapter(Context context, ArrayList<Sight> sights, Location initLoc) {
        super(context, 0, sights);
        this.initLoc = initLoc;
        this.ctx = context;
    }

    /*private view holder class*/
    private class ViewHolder {
        TextView tvName, tvDist, tvAddress;
        ImageView imgSightImg;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        String distanceString = null;
        Sight sight = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place, parent, false);
            holder = new ViewHolder();

            holder.imgSightImg = (ImageView) convertView.findViewById(R.id.placePicImgView);
            holder.tvName = (TextView) convertView.findViewById(R.id.placeNameTextView);
            holder.tvDist = (TextView) convertView.findViewById(R.id.placeDistTextView);
            holder.tvAddress = (TextView) convertView.findViewById(R.id.placeAddressTextView);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvName.setText(sight.getName());
        holder.tvAddress.setText(sight.getAddress());

        Location sightLocation = new Location("Sight1 Location");
        sightLocation.setLatitude(sight.getLocation().getLatitude());
        sightLocation.setLongitude(sight.getLocation().getLongitude());
        int distance = Math.round(initLoc.distanceTo(sightLocation));
        if (distance > 1000) {
            distanceString = MainActivity.round((double) (distance) / 1000, 1) + "km";
        } else {
            distanceString = distance + "m";
        }
        holder.tvDist.setText(distanceString);


        if (sight.getPicturePath() != null && !sight.getPicturePath().isEmpty()) {
            //Log.i("PhotoRef", "https://maps.googleapis.com/maps/api/place/photo?maxwidth=100&photoreference=" + sight.getPicturePath() + "&key=" + NearbyResultActivity.PLACES_API_KEY);
            Picasso.with(ctx)
                    .load("https://maps.googleapis.com/maps/api/place/photo?maxwidth=250&photoreference=" + sight.getPicturePath() +
                            "&key=" + NearbyResultActivity.PLACES_API_KEY)
                    //.fit()
                    //.centerCrop()
                    .into(holder.imgSightImg);
        }

        return convertView;
    }


}
