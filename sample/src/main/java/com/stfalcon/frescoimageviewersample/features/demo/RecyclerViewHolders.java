package com.stfalcon.frescoimageviewersample.features.demo;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.stfalcon.frescoimageviewersample.R;

public class RecyclerViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener{

    boolean selected;
    SimpleDraweeView simpleDraweeView;
    public RecyclerViewHolders(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        simpleDraweeView = (SimpleDraweeView) itemView.findViewById(R.id.image);
    }

    @Override
    public void onClick(View view) {
        Toast.makeText(view.getContext(), "Clicked Country Position = " + getPosition(), Toast.LENGTH_SHORT).show();
    }
}