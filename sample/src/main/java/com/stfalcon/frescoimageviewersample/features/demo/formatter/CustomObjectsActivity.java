package com.stfalcon.frescoimageviewersample.features.demo.formatter;

import android.os.Bundle;

import com.stfalcon.frescoimageviewer.ImageViewer;
import com.stfalcon.frescoimageviewersample.R;
import com.stfalcon.frescoimageviewersample.common.data.Demo;
import com.stfalcon.frescoimageviewersample.common.data.models.CustomImage;
import com.stfalcon.frescoimageviewersample.common.views.ImageOverlayView;
import com.stfalcon.frescoimageviewersample.features.demo.DemoActivity;
import com.stfalcon.frescoimageviewersample.utils.AppUtils;

import java.util.List;

/*
 * Created by troy379 on 06.03.17.
 */
public class CustomObjectsActivity extends DemoActivity {

    private static final java.lang.String KEY_CURRENT_POSITION = "position";
    private List<CustomImage> images;
    private ImageOverlayView overlayView;
    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        images = Demo.getCustomImages();
        AppUtils.showGotItSnackbar(findViewById(R.id.coordinator), R.string.custom_objects_hint);
    }

    @Override
    protected void showPicker(int startPosition) {
        overlayView = new ImageOverlayView(this);
        new ImageViewer.Builder<>(this, images)
                .setFormatter(getCustomFormatter())
                .setImageChangeListener(getImageChangeListener())
                .setOverlayView(overlayView)
                .show();
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

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_CURRENT_POSITION, currentPosition);
        super.onSaveInstanceState(outState);
    }

}
