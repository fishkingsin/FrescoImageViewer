package com.stfalcon.frescoimageviewersample.features.demo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.stfalcon.frescoimageviewersample.R;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolders> {
    private static final String TAG = RecyclerViewAdapter.class.getSimpleName();

    public interface RecyclerViewAdapterListener{
        boolean isPhotoSelected(int position);
        boolean isPhotoSelectionMode();
    }
    RecyclerViewAdapterListener listener;
    private List<String> itemList;
    private Context context;

    public RecyclerViewAdapter(Context context, List<String> itemList) {
        this.itemList = itemList;
        this.context = context;
        listener = (RecyclerViewAdapterListener) context;
    }

    @Override
    public RecyclerViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, null);
        RecyclerViewHolders rcv = new RecyclerViewHolders(layoutView);

        return rcv;
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolders holder, int position) {
        holder.simpleDraweeView.setImageURI(itemList.get(position));

        boolean selection = listener.isPhotoSelected(position);
        boolean selectionMode = listener.isPhotoSelectionMode();
        CheckBox checkBox = holder.checkBox;
        checkBox.setVisibility(selectionMode ? View.VISIBLE: View.INVISIBLE);
        checkBox.setChecked(selectionMode && selection);

    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }
}
