package com.florent37.davinci.sample;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.util.Log;

import com.florent37.davinci.DaVinci;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = (GridViewPager) findViewById(R.id.pager);
        dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setPager(pager);
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

    /**
     * Si nous avons une connection aux Google API, donc au smartphone
     * Nous autorisons l'envoie de messages
     */
    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
        Wearable.DataApi.addListener(mApiClient, this);

        //envoie le premier message
        sendMessage("bonjour", "smartphone");
    }

    /**
     * A la fermeture de l'application, desactive le GoogleApiClient
     * Et ferme l'envoie de message
     */
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

    /**
     * Appellé à la réception d'un message envoyé depuis le smartphone
     *
     * @param messageEvent message reçu
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        //traite le message reçu
        final String path = messageEvent.getPath();
        //récupère le contenu du message
        final String message = new String(messageEvent.getData());

        if (elementList == null || elementList.isEmpty()) {

            Log.d(TAG, "message reçu :" + path);

            if (path.equals("nombre_elements")) {
                elementList = new ArrayList<>();
                int nombre = Integer.parseInt(message); //on part du principe que c'est bien un integer

                Log.d(TAG, "nombre d'éléments à afficher :" + nombre);

                DaVinci.init(this, nombre, mApiClient);

                        //si on reçoit "nombre" c'est que les données ont bien étés envoyées
                for (int i = 0; i < nombre; ++i) {
                    elementList.add(getElement(i));
                }

                startMainScreen();
            } else if (path.equals("bonjour")) {
                elementList = new ArrayList<>();
                elementList.add(new Element("Message reçu", message, Color.parseColor("#F44336")));
                startMainScreen();
            }
        }
    }

    public void startMainScreen() {
        //penser à effectuer les actions graphiques dans le UIThread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //nous affichons ici dans notre viewpager

                if (pager != null && pager.getAdapter() == null)
                    pager.setAdapter(new ElementGridPagerAdapter(elementList, getFragmentManager()));
            }
        });
    }

    /**
     * Envoie un message à au smartphone
     *
     * @param path    identifiant du message
     * @param message message à transmettre
     */
    protected void sendMessage(final String path, final String message) {
        //effectué dans un trhead afin de ne pas être bloquant
        new Thread(new Runnable() {
            @Override
            public void run() {
                //envoie le message à tous les noeuds/montres connectées
                final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for (Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(mApiClient, node.getId(), path, message.getBytes()).await();

                }
            }
        }).start();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        //appellé lorsqu'une donnée à été mise à jour, nous utiliserons une autre méthode
    }

    /**
     * Récupère une URI de donnée en fonction d'un path
     * via l'identifiant nodeId du smartphone
     */
    protected Uri getUriForDataItem(String path) {
        try {
            //recupère le nodeId du smartphone
            final String nodeId = Wearable.NodeApi.getConnectedNodes(mApiClient).await().getNodes().get(0).getId();
            //construit l'uri pointant vers notre path
            Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path(path).build();
            return uri;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }



    /**
     * Récupère un element depuis sa position
     */
    public Element getElement(int index) {
        final Uri uri = getUriForDataItem("/element/" + index);
        if (uri != null) {
            final DataApi.DataItemResult result = Wearable.DataApi.getDataItem(mApiClient, uri).await();
            if (result != null && result.getDataItem() != null) {

                final DataMapItem dataMapItem = DataMapItem.fromDataItem(result.getDataItem());
                return new Element(
                        dataMapItem.getDataMap().getString("titre"),
                        dataMapItem.getDataMap().getString("description"),
                        dataMapItem.getDataMap().getString("url")
                );
            }
        }
        return null;
    }

}
