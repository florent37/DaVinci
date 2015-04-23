package com.florent37.davinci.sample;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.widget.ImageView;

import com.github.florent37.davinci.DaVinci;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener, DataApi.DataListener {

    private final static String TAG = MainActivity.class.getCanonicalName();

    private GridViewPager pager;
    private DotsPageIndicator dotsPageIndicator;

    //la liste des éléments à afficher
    private List<Element> elementList;

    protected GoogleApiClient mApiClient;

    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = (GridViewPager) findViewById(R.id.pager);
        dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setPager(pager);

        mImageView = (ImageView) findViewById(R.id.imageWithTransparency);

        DaVinci.with(this).load("http://www.seomofo.com/downloads/new-google-logo-knockoff.png").into(mImageView);
    }

    /**
     * A l'ouverture, connecte la montre au Google API Client / donc au smartphone
     */
    @Override
    protected void onStart() {
        super.onStart();
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
        Wearable.DataApi.addListener(mApiClient, this);

        sendMessage("hello", "smartphone");
    }

    @Override
    protected void onStop() {
        if (null != mApiClient && mApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mApiClient, this);
            Wearable.DataApi.removeListener(mApiClient, this);
            mApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {}

    public void startMainScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pager != null && pager.getAdapter() == null)
                    pager.setAdapter(new ElementGridPagerAdapter(MainActivity.this,elementList, getFragmentManager()));
            }
        });
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

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {

            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().startsWith("/elements/")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                List<DataMap> elementsDataMap = dataMapItem.getDataMap().getDataMapArrayList("/list/");

                if (elementList == null || elementList.isEmpty()) {
                    elementList = new ArrayList<>();

                    for (DataMap dataMap : elementsDataMap) {
                        elementList.add(getElement(dataMap));
                    }

                    startMainScreen();
                }

            }
        }
    }

    protected Uri getUriForDataItem(String path) {
        try {
            final String nodeId = Wearable.NodeApi.getConnectedNodes(mApiClient).await().getNodes().get(0).getId();
            Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path(path).build();
            return uri;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    public Element getElement(DataMap elementDataMap) {
        return new Element(
                elementDataMap.getString("titre"),
                elementDataMap.getString("description"),
                elementDataMap.getString("url"));
    }


}
