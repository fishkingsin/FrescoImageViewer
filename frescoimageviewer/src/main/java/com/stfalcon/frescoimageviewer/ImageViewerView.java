/*
 * Copyright (C) 2016 stfalcon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stfalcon.frescoimageviewer;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.List;

/*
 * Created by Alexander Krol (troy379) on 29.08.16.
 */
public class ImageViewerView extends RelativeLayout
        implements OnDismissListener, SwipeToDismissListener.OnViewMoveListener {

    private View backgroundView;
    private MultiTouchViewPager pager;
    private ImageViewerAdapter adapter;
    private SwipeDirectionDetector directionDetector;
    private ScaleGestureDetector scaleDetector;
    private ViewPager.OnPageChangeListener pageChangeListener;
    private GestureDetectorCompat gestureDetector;

    private ViewGroup dismissContainer;
    private SwipeToDismissListener swipeDismissListener;
    private View overlayView;

    private SwipeDirectionDetector.Direction direction;

    private ImageRequestBuilder customImageRequestBuilder;
    private GenericDraweeHierarchyBuilder customDraweeHierarchyBuilder;

    private boolean wasScaled;
    private OnDismissListener onDismissListener;
    private boolean isOverlayWasClicked;

    private boolean isZoomingAllowed = true;
    private boolean isSwipeToDismissAllowed = true;

    public ImageViewerView(Context context) {
        super(context);
        init();
    }

    public ImageViewerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageViewerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setUrls(ImageViewer.DataSet<?> dataSet, int startPosition) {
        adapter = new ImageViewerAdapter(
                getContext(), dataSet, customImageRequestBuilder, customDraweeHierarchyBuilder, isZoomingAllowed);
        pager.setAdapter(adapter);
        setStartPosition(startPosition);
    }

    private int scrollState;
    private List<?> newImages;
    public void updateImages(List<?> images) {
        newImages = images;
        if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
            modifyData();
        }
    }

    protected void modifyData() {
        if (newImages != null) {
            Object item = adapter.getItem(pager.getCurrentItem());
            int index = newImages.indexOf(item);
            adapter.updateImages(newImages);
            pager.setCurrentItem(index, false);
        }
        newImages = null;
    }

    public void setCustomImageRequestBuilder(ImageRequestBuilder customImageRequestBuilder) {
        this.customImageRequestBuilder = customImageRequestBuilder;
    }

    public void setCustomDraweeHierarchyBuilder(GenericDraweeHierarchyBuilder customDraweeHierarchyBuilder) {
        this.customDraweeHierarchyBuilder = customDraweeHierarchyBuilder;
    }

    @Override
    public void setBackgroundColor(int color) {
        findViewById(R.id.backgroundView)
                .setBackgroundColor(color);
    }

    public void setOverlayView(View view) {
        this.overlayView = view;
//        if (overlayView != null) {
//            dismissContainer.addView(view);
//            ViewGroup.LayoutParams lp = view.getLayoutParams();
//            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
//            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
//            view.setLayoutParams(lp);
//        }
    }

    public void allowZooming(boolean allowZooming) {
        this.isZoomingAllowed = allowZooming;
    }

    public void allowSwipeToDismiss(boolean allowSwipeToDismiss) {
        this.isSwipeToDismissAllowed = allowSwipeToDismiss;
    }

    public void setImageMargin(int marginPixels) {
        pager.setPageMargin(marginPixels);
    }

    public void setContainerPadding(int[] paddingPixels) {
        pager.setPadding(
                paddingPixels[0],
                paddingPixels[1],
                paddingPixels[2],
                paddingPixels[3]);
    }

    private void init() {
        inflate(getContext(), R.layout.image_viewer, this);

        backgroundView = findViewById(R.id.backgroundView);
        pager = (MultiTouchViewPager) findViewById(R.id.pager);

        dismissContainer = (ViewGroup) findViewById(R.id.container);
        swipeDismissListener = new SwipeToDismissListener(findViewById(R.id.dismissView), this, this);
        dismissContainer.setOnTouchListener(swipeDismissListener);

        directionDetector = new SwipeDirectionDetector(getContext()) {
            @Override
            public void onDirectionDetected(Direction direction) {
                ImageViewerView.this.direction = direction;
            }
        };

        scaleDetector = new ScaleGestureDetector(getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener());

        gestureDetector = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (pager.isScrolled()) {
                    onClick(e, isOverlayWasClicked);
                }
                return false;
            }
        });

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                scrollState = state;
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    modifyData();
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        onUpDownEvent(event);

        if (direction == null) {
            if (scaleDetector.isInProgress() || event.getPointerCount() > 1) {
                wasScaled = true;
                return pager.dispatchTouchEvent(event);
            }
        }

        if (!adapter.isScaled(pager.getCurrentItem())) {
            directionDetector.onTouchEvent(event);
            if (direction != null) {
                switch (direction) {
                    case UP:
                    case DOWN:
                        if (isSwipeToDismissAllowed && !wasScaled && pager.isScrolled()) {
                            return swipeDismissListener.onTouch(dismissContainer, event);
                        } else break;
                    case LEFT:
                    case RIGHT:
                        return pager.dispatchTouchEvent(event);
                }
            }
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onDismiss() {
        if (onDismissListener != null) {
            onDismissListener.onDismiss();
        }
    }

    @Override
    public void onViewMove(float translationY, int translationLimit) {
        float backgroundAlpha = 1.0f - (1.0f / translationLimit / 4) * Math.abs(translationY);
        float alpha = 1.0f - 2 * (1.0f / translationLimit / 4) * Math.abs(translationY);
        if (alpha < 0) alpha = 0;
        backgroundView.setAlpha(backgroundAlpha);
        if (overlayView != null) overlayView.setAlpha(alpha);
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public void resetScale() {
        adapter.resetScale(pager.getCurrentItem());
    }

    public boolean isScaled() {
        return adapter.isScaled(pager.getCurrentItem());
    }

    public String getUrl() {
        return adapter.getUrl(pager.getCurrentItem());
    }

    public void setPageChangeListener(ViewPager.OnPageChangeListener pageChangeListener) {
        pager.removeOnPageChangeListener(this.pageChangeListener);
        this.pageChangeListener = pageChangeListener;
        pager.addOnPageChangeListener(pageChangeListener);
        pageChangeListener.onPageSelected(pager.getCurrentItem());
    }

    private void setStartPosition(int position) {
        pager.setCurrentItem(position);
    }

    private void onUpDownEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            onActionUp(event);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            onActionDown(event);
        }

        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
    }

    private void onActionDown(MotionEvent event) {
        direction = null;
        wasScaled = false;
        pager.dispatchTouchEvent(event);
        swipeDismissListener.onTouch(dismissContainer, event);
        isOverlayWasClicked = dispatchOverlayTouch(event);
    }

    private void onActionUp(MotionEvent event) {
        swipeDismissListener.onTouch(dismissContainer, event);
        pager.dispatchTouchEvent(event);
        isOverlayWasClicked = dispatchOverlayTouch(event);
    }

    private void onClick(MotionEvent event, boolean isOverlayWasClicked) {
        if (overlayView != null && !isOverlayWasClicked) {
            AnimationUtils.animateVisibility(overlayView);
            super.dispatchTouchEvent(event);
        }
    }

    private boolean dispatchOverlayTouch(MotionEvent event) {
        return overlayView != null
                && overlayView.getVisibility() == VISIBLE
                && overlayView.dispatchTouchEvent(event);
    }

}