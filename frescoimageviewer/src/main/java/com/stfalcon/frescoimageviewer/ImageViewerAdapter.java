package com.stfalcon.frescoimageviewer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.stfalcon.frescoimageviewer.adapter.RecyclingPagerAdapter;
import com.stfalcon.frescoimageviewer.adapter.ViewHolder;
import com.stfalcon.frescoimageviewer.drawee.CircleProgressBarDrawable;
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
    private List<String> thumbnails;
    private HashSet<ImageViewHolder> holders;
    private ImageRequestBuilder imageRequestBuilder;
    private GenericDraweeHierarchyBuilder hierarchyBuilder;
    private boolean isZoomingAllowed;
    private ImageViewer.OnOrientationListener orientationListener;

    private Handler mHandler = new Handler();
    static HttpProxyCacheServer httpProxyCache;

    ImageViewerAdapter(Context context, ImageViewer.DataSet<?> dataSet,
                       ImageRequestBuilder imageRequestBuilder,
                       GenericDraweeHierarchyBuilder hierarchyBuilder,
                       boolean isZoomingAllowed,
                       ImageViewer.OnOrientationListener orientationListener,
                       List<String> thumbnails) {
        this.context = context;
        this.dataSet = dataSet;
        this.holders = new HashSet<>();
        this.imageRequestBuilder = imageRequestBuilder;
        this.hierarchyBuilder = hierarchyBuilder;
        this.isZoomingAllowed = isZoomingAllowed;
        this.thumbnails = thumbnails;

        this.orientationListener = orientationListener;

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
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_view, parent, false);

        ImageViewHolder holder = new ImageViewHolder(itemView);
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
        private VideoView videoView;
        private ImageView playButton;
        private boolean isScaled;

        ImageViewHolder(View itemView) {
            super(itemView);
//            drawee = (ZoomableDraweeView) itemView;
            drawee = (ZoomableDraweeView) itemView.findViewById(R.id.draweeView);
            videoView = (VideoView) itemView.findViewById(R.id.videoView);
            playButton = (ImageView) itemView.findViewById(R.id.iv_play);
        }

        void bind(int position) {
            this.position = position;

            String mediaUrl = dataSet.format(position);
            if (mediaUrl.contains(".mp4") || mediaUrl.contains(".3gp") || mediaUrl.contains(".m4a") || mediaUrl.contains(".mkv")) {
                Log.v("ImageViewerAdapter", "contains mp4");

                drawee.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.VISIBLE);
                playButton.setVisibility(View.VISIBLE);

                //used for video caching
                HttpProxyCacheServer proxy = getProxy();
                videoView.setVideoPath(proxy.getProxyUrl(mediaUrl));

                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d("ImageViewerAdapter", "Video ended");
                        drawee.setVisibility(View.VISIBLE);
                        playButton.setVisibility(View.VISIBLE);
                    }
                });

                playButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        videoView.start();
                        playButton.setVisibility(View.INVISIBLE);
                        videoView.setVisibility(View.VISIBLE);
                        //delay added for smooth transition from video_preview (drawee) -> video (videoView) when video is played initially
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                drawee.setVisibility(View.INVISIBLE);
                            }
                        }, 300);
                    }
                });

                tryToSetHierarchy();
                int orientation = 0;
                if (orientationListener != null) {
                    orientation = orientationListener.OnOrientaion(position);
                }

                //use thumbnail for video preview
                setController(thumbnails.get(position), orientation, false);

            } else {
                drawee.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.INVISIBLE);
                playButton.setVisibility(View.INVISIBLE);

                tryToSetHierarchy();
                int orientation = 0;
                if (orientationListener != null) {
                    orientation = orientationListener.OnOrientaion(position);
                }

                setController(mediaUrl, orientation, false);
                drawee.setOnScaleChangeListener(this);
            }
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

        private void setController(String url, int orientation, boolean videoPreview) {
            buildImage(url, orientation, videoPreview);

//            prefetchHeader(url, new HeaderResponse() {
//
//
//                @Override
//                public void onErrorLoaded(String s) {
//
//                }
//
//                @Override
//                public void onResponseLoaded(final int rotate) {
//                    buildImage(url, rotate);
//                }
//            });


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
                    for (String name : names) {
                        Log.d("prefetchHeader", "Name :" + name);
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

        private void buildImage(final String url, final int rotate, boolean videoPreview) {

            ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                    .setRotationOptions(RotationOptions.forceRotation(rotate))
                    .build();

            DraweeController controller;
            if (videoPreview) {
                controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(drawee.getController())
                        .setImageRequest(request)
                        .build();
            } else {
                controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(drawee.getController())
                        .setControllerListener(getDraweeControllerListener(drawee))
                        .setImageRequest(request)
                        .build();
            }

            CircleProgressBarDrawable progressBarDrawable = new CircleProgressBarDrawable();
            progressBarDrawable.setColor(Color.WHITE);
            progressBarDrawable.setBackgroundColor(Color.DKGRAY);
            final Drawable failureDrawable = context.getResources().getDrawable(R.drawable.empty);
            DrawableCompat.setTint(failureDrawable, Color.WHITE);
            drawee.getHierarchy().setFailureImage(failureDrawable, ScalingUtils.ScaleType.CENTER_INSIDE);
            drawee.getHierarchy().setProgressBarImage(progressBarDrawable, ScalingUtils.ScaleType.CENTER_INSIDE);
            drawee.setController(controller);
        }
    }


    interface HeaderResponse {
        void onErrorLoaded(String s);

        void onResponseLoaded(int rotate);
    }

    public void stopVideoPlayback() {
        for (ImageViewHolder holder : holders) {
            if (holder.videoView.isPlaying()) {
                holder.videoView.stopPlayback();
                holder.videoView.setVisibility(View.INVISIBLE);
                holder.drawee.setVisibility(View.VISIBLE);
                holder.playButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public HttpProxyCacheServer getProxy() {
        if (httpProxyCache == null) {
            httpProxyCache = new HttpProxyCacheServer(context);
        }
        return httpProxyCache;
    }
}