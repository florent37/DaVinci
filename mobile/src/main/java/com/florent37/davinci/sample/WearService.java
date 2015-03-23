package com.florent37.davinci.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.florent37.davinci.daemon.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class WearService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks {

    private final static String TAG = WearService.class.getCanonicalName();

    protected GoogleApiClient mApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        DaVinciDaemon.with(getApplicationContext()).handleMessage(messageEvent);

        ConnectionResult connectionResult = mApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }


        final String path = messageEvent.getPath();

        if (path.equals("hello")) {

            AndroidService androidService = new RestAdapter.Builder()
                    .setEndpoint(AndroidService.ENDPOINT)
                    .build().create(AndroidService.class);

            androidService.getElements(new Callback<List<Element>>() {
                @Override
                public void success(List<Element> elements, Response response) {
                    sendListElements(elements);
                }

                @Override
                public void failure(RetrofitError error) {
                }
            });

        }
    }

    private void sendListElements(final List<Element> elements) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int nbElements = elements.size();

                sendElements(elements);

                sendMessage("nb_elements",String.valueOf(nbElements));
            }
        }).start();
    }

    protected void sendMessage(final String path, final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for (Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(mApiClient, node.getId(), path, message.getBytes()).await();

                }
            }
        }).start();
    }

    protected void sendElements(final List<Element> elements) {

        for (int position = 0; position < elements.size(); ++position) {

            Element element = elements.get(position);

            final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/element/" + position);

            putDataMapRequest.getDataMap().putString("timestamp", new Date().toString());

            putDataMapRequest.getDataMap().putString("title", element.getTitre());
            putDataMapRequest.getDataMap().putString("description", element.getDescription());
            putDataMapRequest.getDataMap().putString("url", element.getUrl());

            if (mApiClient.isConnected())
                Wearable.DataApi.putDataItem(mApiClient, putDataMapRequest.asPutDataRequest());

            DaVinciDaemon.with(getApplicationContext()).load(element.getUrl()).send();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
