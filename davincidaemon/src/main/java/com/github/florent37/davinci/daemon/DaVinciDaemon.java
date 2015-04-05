package com.github.florent37.davinci.daemon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

public class DaVinciDaemon implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener, DataApi.DataListener, GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = DaVinciDaemon.class.getCanonicalName();

    private Context mContext;
    private GoogleApiClient mApiClient;
    private CallBack mCallBack;
    private String mUrl;

    private static final String DEFAULT_PATH = "/davinci/";
    private String imageAssetName = "image";

    private final static String MESSAGE_DAVINCI = "/davinci/";
    private final static String DAVINCI_PATH = "/davinci/";

    private static DaVinciDaemon INSTANCE;

    private DaVinciDaemon(Context context) {
        this.mContext = context;

        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
        //TODO disconnect when the application close
    }

    public static DaVinciDaemon with(Context context) {
        if (INSTANCE == null)
            INSTANCE = new DaVinciDaemon(context);
        if (context != null)
            INSTANCE.mContext = context;
        return INSTANCE;
    }

    public String getImageAssetName() {
        return imageAssetName;
    }

    public DaVinciDaemon setImageAssetName(String imageAssetName) {
        this.imageAssetName = imageAssetName;
        return this;
    }

    public Bitmap getBitmapFromURL(String url) {
        try {
            Bitmap bitmap = Picasso.with(mContext)
                    .load(url)
                            //.transform(new ResizeTransformation(300))
                    .get();

            Log.d(TAG, "image getted " + url);

            return bitmap;

        } catch (IOException e) {
            Log.e(TAG, "getImage error " + url, e);
        }
        return null;
    }

    public static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private void sendImage(final String url, final String path) {
        Picasso.with(mContext)
                .load(url)
                .transform(new ResizeTransformation(300))
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        Log.d(TAG, "Picasso " + url + " loaded");
                        sentBitmap(bitmap, url, path);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        Log.d(TAG, "Picasso " + url + " failed");
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
    }

    private String generatePath(final String url, final String path) {
        if (path == null)
            return DEFAULT_PATH + url.hashCode();
        else
            return path;
    }

    private void sentBitmap(Bitmap bitmap, final String url, final String path) {
        if (bitmap != null) {
            final Asset asset = createAssetFromBitmap(bitmap);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String finalPath = generatePath(url, path);

                    final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(finalPath);

                    putDataMapRequest.getDataMap().putString("timestamp", new Date().toString());

                    putDataMapRequest.getDataMap().putAsset("image", asset);

                    if (mApiClient != null && mApiClient.isConnected())
                        Wearable.DataApi.putDataItem(mApiClient, putDataMapRequest.asPutDataRequest()).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                if (mCallBack != null) {
                                    if (dataItemResult.getStatus().isSuccess()) {
                                        Log.d(TAG, url + " send");
                                        mCallBack.onBitmapSent(url, finalPath);
                                    } else {
                                        Log.d(TAG, url + " send error");
                                        mCallBack.onBitmapError(url, finalPath);
                                    }
                                }
                            }
                        });
                    else {
                        Log.d(TAG, "ApiClient null of not connected");

                        if (mCallBack != null)
                            mCallBack.onBitmapError(url, path);
                    }
                }
            }).start();

        } else {
            if (mCallBack != null)
                mCallBack.onBitmapError(url, path);
        }
    }

    public DaVinciDaemon callBack(CallBack callBack) {
        this.mCallBack = callBack;
        return this;
    }


    public DaVinciDaemon load(final String url) {
        this.mUrl = url;
        return this;
    }

    public void send() {
        into(null);
    }

    public void into(final String path) {
        if(path != null && !path.trim().isEmpty()) {
            if (mUrl == null) {
                Log.d(TAG, "must execute .load(url) before");
            } else {
                Log.d(TAG, "load " + mUrl);

                final String tmpUrl = mUrl;

                //main handler for picasso
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        sendImage(tmpUrl, path);
                    }
                });
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
        Wearable.DataApi.addListener(mApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG,messageEvent.toString());
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG,dataEvents.toString());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void handleMessage(MessageEvent messageEvent) {
        final String path = messageEvent.getPath();

        if (path.equals(MESSAGE_DAVINCI)) {
            String message = new String(messageEvent.getData());
            if (message.startsWith("http") || message.startsWith("www")) {
                String sendPath = message.hashCode() + "";

                load(message).into(DAVINCI_PATH + sendPath);
            }
        }
    }

    public static interface CallBack {
        void onBitmapSent(String url, String path);

        void onBitmapError(String url, String path);
    }

}
