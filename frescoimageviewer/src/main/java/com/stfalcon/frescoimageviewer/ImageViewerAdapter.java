package com.stfalcon.frescoimageviewer;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.stfalcon.frescoimageviewer.adapter.RecyclingPagerAdapter;
import com.stfalcon.frescoimageviewer.adapter.ViewHolder;
import com.stfalcon.frescoimageviewer.drawee.ZoomableDraweeView;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.relex.photodraweeview.OnScaleChangeListener;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * Created by troy379 on 07.12.16.
 */
class ImageViewerAdapter
        extends RecyclingPagerAdapter<ImageViewerAdapter.ImageViewHolder> {

    private Context context;
    private ImageViewer.DataSet<?> dataSet;
    private HashSet<ImageViewHolder> holders;
    private ImageRequestBuilder imageRequestBuilder;
    private GenericDraweeHierarchyBuilder hierarchyBuilder;
    private boolean isZoomingAllowed;

    ImageViewerAdapter(Context context, ImageViewer.DataSet<?> dataSet,
                       ImageRequestBuilder imageRequestBuilder,
                       GenericDraweeHierarchyBuilder hierarchyBuilder,
                       boolean isZoomingAllowed) {
        this.context = context;
        this.dataSet = dataSet;
        this.holders = new HashSet<>();
        this.imageRequestBuilder = imageRequestBuilder;
        this.hierarchyBuilder = hierarchyBuilder;
        this.isZoomingAllowed = isZoomingAllowed;
    }

    public boolean addImage(List<?> images) {
        return this.addImages(dataSet.getData().size(), images);
    }

    public boolean addImages(int position, List<?> images) {
        boolean result = dataSet.addData(position, images);
        notifyDataSetChanged();
        return result;
    }

    public void updateImages(List<?> images) {
        dataSet.updateData(images);
        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        return dataSet.getData().get(position);
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ZoomableDraweeView drawee = new ZoomableDraweeView(context);
        drawee.setEnabled(isZoomingAllowed);

        ImageViewHolder holder = new ImageViewHolder(drawee);
        holders.add(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return dataSet.getData().size();
    }


    boolean isScaled(int index) {
        for (ImageViewHolder holder : holders) {
            if (holder.position == index) {
                return holder.isScaled;
            }
        }
        return false;
    }

    void resetScale(int index) {
        for (ImageViewHolder holder : holders) {
            if (holder.position == index) {
                holder.resetScale();
                break;
            }
        }
    }

    String getUrl(int index) {
        return dataSet.format(index);
    }

    private BaseControllerListener<ImageInfo>
    getDraweeControllerListener(final ZoomableDraweeView drawee) {
        return new BaseControllerListener<ImageInfo>() {
            @Override
            public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                super.onFinalImageSet(id, imageInfo, animatable);
                if (imageInfo == null) {
                    return;
                }
                drawee.update(imageInfo.getWidth(), imageInfo.getHeight());
            }
        };
    }

    class ImageViewHolder extends ViewHolder implements OnScaleChangeListener {

        private int position = -1;
        private ZoomableDraweeView drawee;
        private boolean isScaled;

        ImageViewHolder(View itemView) {
            super(itemView);
            drawee = (ZoomableDraweeView) itemView;
        }

        void bind(int position) {
            this.position = position;

            tryToSetHierarchy();
            setController(dataSet.format(position));

            drawee.setOnScaleChangeListener(this);
        }

        @Override
        public void onScaleChange(float scaleFactor, float focusX, float focusY) {
            isScaled = drawee.getScale() > 1.0f;
        }

        void resetScale() {
            drawee.setScale(1.0f, true);
        }

        private void tryToSetHierarchy() {
            if (hierarchyBuilder != null) {
                hierarchyBuilder.setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
                drawee.setHierarchy(hierarchyBuilder.build());
            }
        }

        private void setController(final String url) {
            prefetchHeader(url, new HeaderResponse() {


                @Override
                public void onErrorLoaded(String s) {
                    buildImage(url, 0);
                }

                @Override
                public void onResponseLoaded(final int rotate) {
                    buildImage(url, rotate);
                }
            });


        }

        public void prefetchHeader(String url, final HeaderResponse serverResponse) {

//TODO WARNING a project specfic routine, not for general prupose , may need to deal with it later
            OkHttpClient client = new OkHttpClient();

            final Request request = new Request.Builder()
                    .url(url)
                    .head()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("response", call.request().body().toString());
                    serverResponse.onErrorLoaded(call.request().body().toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Set<String> names = response.headers().names();
                    for(String name : names){
                        Log.d("prefetchHeader","Name :"+name);
                    }
                    Headers header = response.headers();
                    String headerKey = "x-amz-meta-orientation";
                    int rotation = 0;
                    if (header.names().contains(headerKey)) {
                        String orientationValue = response.headers().get(headerKey);
                        int orientation = Integer.valueOf(orientationValue);


                        try {
                            rotation = exifToDegrees(orientation);
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                    }
                    serverResponse.onResponseLoaded(rotation);

                }


            });
        }

        private int exifToDegrees(int exifOrientation) {
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            } else {
                return 0;
            }
        }

        private void buildImage(final String url, final int rotate) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PipelineDraweeControllerBuilder controllerBuilder = Fresco.newDraweeControllerBuilder();
                    controllerBuilder.setUri(url);

                    controllerBuilder.setOldController(drawee.getController());
                    controllerBuilder.setControllerListener(getDraweeControllerListener(drawee));
                    if (imageRequestBuilder != null) {
                        imageRequestBuilder.setSource(Uri.parse(url))
                                .setRotationOptions(RotationOptions.forceRotation(rotate));
                        controllerBuilder.setImageRequest(imageRequestBuilder.build());
                    }
                    drawee.setController(controllerBuilder.build());
                }
            });
        }
    }



    interface HeaderResponse {
        void onErrorLoaded(String s);

        void onResponseLoaded(int rotate);
    }
}