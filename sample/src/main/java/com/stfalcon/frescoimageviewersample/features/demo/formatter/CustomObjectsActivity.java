package com.stfalcon.frescoimageviewersample.features.demo.formatter;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import com.stfalcon.frescoimageviewer.ImageViewer;
import com.stfalcon.frescoimageviewersample.R;
import com.stfalcon.frescoimageviewersample.common.data.Demo;
import com.stfalcon.frescoimageviewersample.common.data.models.CustomImage;
import com.stfalcon.frescoimageviewersample.common.views.ImageOverlayView;
import com.stfalcon.frescoimageviewersample.features.demo.DemoActivity;
import com.stfalcon.frescoimageviewersample.features.demo.RecyclerViewAdapter;
import com.stfalcon.frescoimageviewersample.features.demo.SquareCardView;

import java.util.List;

/*
 * Created by troy379 on 06.03.17.
 */
public class CustomObjectsActivity extends DemoActivity implements RecyclerViewAdapter.RecyclerViewAdapterListener {

    private static final String KEY_IS_DIALOG_SHOWN = "IS_DIALOG_SHOWN";
    private static final String KEY_CURRENT_POSITION = "CURRENT_POSITION";

    private List<CustomImage> images;
    private ImageOverlayView overlayView;
    private int currentPosition;
    private boolean isDialogShown;
    protected boolean[] selections;
    private boolean selectionMode;
    String[] menuOptions;
    private android.support.v7.app.ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        images = Demo.getCustomImages();
//        AppUtils.showGotItSnackbar(findViewById(R.id.coordinator), R.string.custom_objects_hint);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Album");
            actionBar.setSubtitle("145 Photos．15 Nov 2016");
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_up_white_24dp);
        }

        selections = new boolean[posters.length];
        for (int i = 0; i < selections.length; i++) {
            selections[i] = false;
        }
    }

    private ImageViewer.Formatter<CustomImage> getCustomFormatter() {
        return new ImageViewer.Formatter<CustomImage>() {
            @Override
            public String format(CustomImage customImage) {
                return customImage.getUrl();
            }
        };
    }

    private ImageViewer.OnImageChangeListener getImageChangeListener() {
        return new ImageViewer.OnImageChangeListener() {
            @Override
            public void onImageChange(int position) {
                CustomImage image = images.get(position);
                overlayView.setShareText(image.getUrl());
                overlayView.setDescription(image.getDescription());
            }
        };
    }

    private ImageViewer.OnDismissListener getDismissListener() {
        return new ImageViewer.OnDismissListener() {
            @Override
            public void onDismiss() {
                isDialogShown = false;
            }
        };
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            isDialogShown = savedInstanceState.getBoolean(KEY_IS_DIALOG_SHOWN);
            currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION);
        }

        if (isDialogShown) {
            showPicker(currentPosition);
        }
    }

    @Override
    protected void showPicker(int startPosition) {
        isDialogShown = true;
        currentPosition = startPosition;
        overlayView = new ImageOverlayView(this);
        new ImageViewer.Builder<>(this, posters)
                .setOverlayView(overlayView)
                .setStartPosition(startPosition)
                .setImageChangeListener(getImageChangeListener())
                .setOnDismissListener(getDismissListener())
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_IS_DIALOG_SHOWN, isDialogShown);
        outState.putInt(KEY_CURRENT_POSITION, currentPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (!selectionMode) {
            inflater.inflate(R.menu.options_menu, menu);
            setupToolBar();
        } else {
            inflater.inflate(R.menu.menu, menu);
            setupToolBar();
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem actionViewItem = menu.findItem(R.id.miActionButton);
//        // Retrieve the action-view from menu
//        View v = MenuItemCompat.getActionView(actionViewItem);
//        // Find the button within action-view
//        Button b = (Button) v.findViewById(R.id.btnCustomAction);
        // Handle button click here
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!selectionMode) {
            return true;
        } else {
            setupSelectionMode(false);
            return false;

        }
    }
    private void setupToolBar() {

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(selectionMode) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }else{
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_up_white_24dp);
        }
    }

    private void setupSelectionMode(boolean b) {
        selectionMode = b;
        if (!selectionMode) {
            for (int i = 0; i < selections.length; i++) {
                selections[i] = false;
            }
        } else {
            //TODO show bottom menu
        }
        rcAdapter.notifyDataSetChanged();

        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.selectPhoto: {
                setupSelectionMode(!selectionMode);
            }
            break;
            case R.id.addPhotos:
                break;
            case R.id.addAlbumToPlaylist:
                break;
            case R.id.editAlbumName:
                break;
            case R.id.deleteAlbum:
                break;
            case android.R.id.home:
                if(!selectionMode) {
                    finish();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;

    }

    @Override
    public void onItemClick(View view, int position) {
        if (selectionMode) {
            selections[position] = !selections[position];
            SquareCardView squareCardView = (SquareCardView) view;
            if (squareCardView != null) {
                CheckBox checkBox = (CheckBox) squareCardView.findViewById(R.id.checkBox);
                if (selectionMode) {
                    checkBox.setChecked(selections[position]);
                }

            }
        } else {
            showPicker(position);
        }

    }

    @Override
    public void onItemLongClick(View view, int position) {
        // ...
        selectionMode = true;
        selections[position] = !selections[position];
        SquareCardView squareCardView = (SquareCardView) view;
        if (squareCardView != null) {
            CheckBox checkBox = (CheckBox) squareCardView.findViewById(R.id.checkBox);
            if (selectionMode) {
                checkBox.setChecked(selections[position]);
            }

        }
    }

    @Override
    public boolean isPhotoSelected(int position) {
        return selections[position];
    }

    @Override
    public boolean isPhotoSelectionMode() {
        return selectionMode;
    }


}
