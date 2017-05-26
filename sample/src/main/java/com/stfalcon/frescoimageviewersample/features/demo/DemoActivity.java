package com.stfalcon.frescoimageviewersample.features.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

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
public abstract class DemoActivity extends AppCompatActivity {
    private static final String TAG = DemoActivity.class.getSimpleName();
    RecyclerView recyclerView;
    GridLayoutManager lLayout;
//    private static final int[] ids = new int[]{
//            R.id.firstImage, R.id.secondImage,
//            R.id.thirdImage, R.id.fourthImage,
//            R.id.fifthImage, R.id.sixthImage,
//            R.id.seventhImage, R.id.eighthImage,
//            R.id.ninethImage
//    };

    protected String[] posters, descriptions;
    private boolean selected;

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
                selected = !selected;
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
        } catch (JSONException e) {
            e.printStackTrace();

        }


        RecyclerViewAdapter rcAdapter = new RecyclerViewAdapter(DemoActivity.this, rowListItem);
        recyclerView.setAdapter(rcAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(DemoActivity.this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                showPicker(position);

            }

            @Override
            public void onItemLongClick(View view, int position) {
                // ...
            }
        }));
//
//        for (int i = 0; i < ids.length; i++) {
//            SimpleDraweeView drawee = (SimpleDraweeView) findViewById(ids[i]);
//            initDrawee(drawee, i);
//        }
    }
//
//    private void initDrawee(SimpleDraweeView drawee, final int startPosition) {
//        drawee.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showPicker(startPosition);
//            }
//        });
//        drawee.setImageURI(posters[startPosition]);
//    }
}
