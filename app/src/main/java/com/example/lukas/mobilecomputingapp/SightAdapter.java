package com.example.lukas.mobilecomputingapp;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.List;

/**
 * Created by Lukas on 20/11/2017.
 */

public class SightAdapter extends RecyclerView.Adapter<SightAdapter.MyViewHolder> {

    private List<Sight> sightList;

    public SightAdapter(List<Sight> sights) {
        this.sightList = sights;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sight_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Sight sight = sightList.get(position);
        holder.name.setText(sight.getName());
        holder.date.setText(sight.getDate());
        holder.description.setText(sight.getLocation().toString());

        holder.img.setImageBitmap(sight.getPicture());

        holder.favBtn.setChecked(false);

        holder.PicPath = sight.getPicturePath();
        holder.PicName = sight.getName();

    }

    @Override
    public int getItemCount() {
        return sightList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private String PicPath;
        private String PicName;

        private TextView name, date, description;
        private ImageView img;
        private ToggleButton favBtn;

        public MyViewHolder(View view) {
            super(view);

            view.setOnClickListener(this);

            name = (TextView) view.findViewById(R.id.sight_name);
            date = (TextView) view.findViewById(R.id.sight_date);
            description = (TextView) view.findViewById(R.id.sight_description);

            img = (ImageView) view.findViewById(R.id.sight_image);

            favBtn = (ToggleButton) view.findViewById(R.id.sight_favBtn);
        }

        @Override
        public void onClick(View v) {
            Intent singlePicIntent = new Intent(v.getContext(), SinglePictureActivity.class);

            singlePicIntent.putExtra("SightName", PicName);
            singlePicIntent.putExtra("PictureLocation", PicPath);

            v.getContext().startActivity(singlePicIntent);
        }
    }
}
