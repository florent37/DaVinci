package com.florent37.davinci.sample;

import android.os.Bundle;
import android.util.Log;

import com.github.florent37.davinci.daemon.DaVinciDaemon;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
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

            //DaVinciDaemon.with(getApplicationContext()).load("http://lorempixel.com/400/200/").into("/image/0");

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
            }
        }).start();
    }

    protected void sendElements(final List<Element> elements) {
        final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/elements/");

        ArrayList<DataMap> elementsDataMap = new ArrayList<>();

        for (int position = 0; position < elements.size(); ++position) {

            DataMap elementDataMap = new DataMap();
            Element element = elements.get(position);
            elementDataMap.putString("timestamp", new Date().toString());

            elementDataMap.putString("titre", element.getTitre());
            elementDataMap.putString("description", element.getDescription());
            elementDataMap.putString("url", element.getUrl());

            elementsDataMap.add(elementDataMap);

        }
        putDataMapRequest.getDataMap().putDataMapArrayList("/list/",elementsDataMap);

        if (mApiClient.isConnected())
            Wearable.DataApi.putDataItem(mApiClient, putDataMapRequest.asPutDataRequest());

        for(int position = 0; position < elements.size(); ++position){
            DaVinciDaemon.with(getApplicationContext()).load(elements.get(position).getUrl()).send();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
