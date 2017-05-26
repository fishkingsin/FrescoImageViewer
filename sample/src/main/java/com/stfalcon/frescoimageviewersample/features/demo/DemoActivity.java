package com.stfalcon.frescoimageviewersample.features.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import com.stfalcon.frescoimageviewersample.R;
import com.stfalcon.frescoimageviewersample.common.data.Demo;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Created by troy379 on 06.03.17.
 */
public abstract class DemoActivity extends AppCompatActivity implements RecyclerItemClickListener.OnItemClickListener , RecyclerViewAdapter.RecyclerViewAdapterListener {
    private static final String TAG = DemoActivity.class.getSimpleName();
    RecyclerView recyclerView;
    GridLayoutManager lLayout;


    protected String[] posters, descriptions;
    protected boolean[] selections;
    private boolean selectionMode;
    private RecyclerViewAdapter rcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);


    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.item1:
                selectionMode = !selectionMode;
                rcAdapter.notifyDataSetChanged();

                return true;
            case R.id.item2:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void showPicker(int startPosition) {

    }

    protected void init() {

        lLayout = new GridLayoutManager(DemoActivity.this, 3);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(lLayout);

        posters = Demo.getPosters();
        descriptions = Demo.getDescriptions();
        String jsonString = Demo.getFlickrs();
        List<String> rowListItem;
        rowListItem = new ArrayList<>(Arrays.asList(Demo.getFlickrs()));
        try {
            JSONArray array = new JSONArray(jsonString);
            ArrayList<String> previewUrls = new ArrayList();
            ArrayList<String> thumbnailUrls = new ArrayList();
            for(int i = 0 ; i < array.length() ; i++) {
                previewUrls.add(array.getJSONObject(i).getString("previewUrl"));
                thumbnailUrls.add(array.getJSONObject(i).getString("thumbnailUrl"));
            }
            rowListItem = thumbnailUrls;
            posters = previewUrls.toArray(new String[0]);
            descriptions = previewUrls.toArray(new String[0]);
            selections = new boolean[posters.length];
        } catch (JSONException e) {
            e.printStackTrace();

        }


        rcAdapter = new RecyclerViewAdapter(DemoActivity.this, rowListItem);
        recyclerView.setAdapter(rcAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(DemoActivity.this, recyclerView, this));

    }

    @Override
    public void onItemClick(View view, int position) {
        if(selectionMode){
            selections[position] = !selections[position];
            SquareCardView squareCardView = (SquareCardView) view;
            if(squareCardView != null){
                CheckBox checkBox = (CheckBox) squareCardView.findViewById(R.id.checkBox);
                if(selectionMode) {
                    checkBox.setChecked(selections[position]);
                }

            }
        }else {
            showPicker(position);
        }

    }

    @Override
    public void onItemLongClick(View view, int position) {
        // ...
    }
    @Override
    public boolean isPhotoSelected(int position){
        return selections[position];
    }
    @Override
    public boolean isPhotoSelectionMode(){
        return selectionMode;
    }
}
