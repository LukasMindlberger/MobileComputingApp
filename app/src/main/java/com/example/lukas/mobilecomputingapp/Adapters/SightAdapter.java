package com.example.lukas.mobilecomputingapp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.lukas.mobilecomputingapp.Activites.MainActivity;
import com.example.lukas.mobilecomputingapp.R;
import com.example.lukas.mobilecomputingapp.Models.Sight;
import com.example.lukas.mobilecomputingapp.Activites.SinglePictureActivity;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Lukas on 20/11/2017.
 */

public class SightAdapter extends RecyclerView.Adapter<SightAdapter.MyViewHolder> {

    private Context ctx;
    private List<Sight> sightList;

    private Geocoder gcd;
    private List<Address> addresses;

    public MyAdapterListener onClickListener;


    public SightAdapter(Context ctx, List<Sight> sights, MyAdapterListener myAdapterListener) {
        this.ctx = ctx;
        this.sightList = sights;
        this.onClickListener = myAdapterListener;
        this.gcd = new Geocoder(ctx, Locale.UK);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sight_card, parent, false);

        MyViewHolder mVH = new MyViewHolder(itemView);

        return mVH;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        //final Sight sight = sightList.get(position);
        holder.sight = sightList.get(position);

        holder.name.setText(sightList.get(position).getName());

        //Get city and country of location
        addresses = null;
        try {
            addresses = gcd.getFromLocation(sightList.get(position).getLocation().getLatitude(), sightList.get(position).getLocation().getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses.size() > 0) {
            if (addresses.get(0).getLocality() != null) {
                holder.description.setText(String.format("%s, %s", addresses.get(0).getLocality(), addresses.get(0).getCountryName()));
            } else {
                holder.description.setText(String.format("%s", addresses.get(0).getCountryName()));
            }

        } else {
            holder.description.setText(sightList.get(position).getLocation().toString());
        }

        try {
            Date d = MainActivity.FULL_TIME_FORMAT.parse(sightList.get(position).getDateString());
            holder.date.setText(new SimpleDateFormat("dd.MM.yyyy HH:mm").format(d));
        } catch (ParseException e) {
            e.printStackTrace();
            holder.date.setText(sightList.get(position).getDateString());
        }

        //holder.img.setImageBitmap(BitmapFactory.decodeFile(sightList.get(position).getPicturePath()));
        Picasso.with(ctx) //much better performance
                .load(new File(sightList.get(position).getPicturePath()))
                .fit()
                .centerCrop()
                .into(holder.img);

        holder.favBtn.setChecked(sightList.get(position).isFavorite());
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Sight sight;

        private TextView name, date, description;
        private ImageView img;
        private ToggleButton favBtn;

        private Button mapBtn, shareBtn, nearbyBtn;

        public MyViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            name = (TextView) view.findViewById(R.id.sight_name);
            date = (TextView) view.findViewById(R.id.sight_date);
            description = (TextView) view.findViewById(R.id.sight_description);
            img = (ImageView) view.findViewById(R.id.sight_image);

            mapBtn = (Button) view.findViewById(R.id.sight_mapBtn);
            shareBtn = (Button) view.findViewById(R.id.sight_shareBtn);
            nearbyBtn = (Button) view.findViewById(R.id.sight_nearbyBtn);

            favBtn = (ToggleButton) view.findViewById(R.id.sight_favBtn);

            mapBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickListener.mapBtnOnClick(view, getAdapterPosition());
                }
            });

            shareBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickListener.shareBtnOnClick(view, getAdapterPosition());
                }
            });

            favBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickListener.favBtnOnClick(view, getAdapterPosition());
                }
            });

            nearbyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickListener.nearbyBtnOnClick(view, getAdapterPosition());
                }
            });

        }

        @Override
        public void onClick(View v) {
            Intent singlePicIntent = new Intent(v.getContext(), SinglePictureActivity.class);
            singlePicIntent.putExtra("Sight", sight);
            v.getContext().startActivity(singlePicIntent);
        }
    }

    public interface MyAdapterListener {
        void favBtnOnClick(View v, int position);

        void mapBtnOnClick(View v, int position);

        void shareBtnOnClick(View v, int position);

        void nearbyBtnOnClick(View v, int position);
    }

    @Override
    public int getItemCount() {
        return sightList.size();
    }
}
