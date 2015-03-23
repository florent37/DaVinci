package com.florent37.davinci;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.view.GridPagerAdapter;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DaVinci implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener, DataApi.DataListener {

    public static final String TAG = DaVinci.class.getCanonicalName();

    private static final int DEFAULT_SIZE = 10;
    private final static String MESSAGE_DAVINCI = "davinci";
    private final static String DAVINCI_PATH = "/davinci/";

    private String imageAssetName = "image";

    private int mSize;
    private Context mContext;
    private LruCache<Integer, Bitmap> mImagesCache;
    private DiskLruImageCache mDiskImageCache;

    private ArrayList<String> mIndexes = new ArrayList<>();
    private Map<String, ArrayList<Object>> mIntoWaiting = new HashMap<>();

    private GoogleApiClient mApiClient;

    public static DaVinci INSTANCE;

    private Drawable mPlaceHolder;
    private String mPath;
    private Object mInto;

    /**
     * Initialise DaVinci, muse have a googleApiClient to retrieve Bitmaps from Smartphone
     *
     * @param context the application context
     * @param size    the number of entry on the cache
     */
    private DaVinci(Context context, int size) {
        this.mImagesCache = new LruCache<>(size);
        this.mSize = size;

        int cacheSize = 20 * 1024 * 1024; //20mo of disk cache
        this.mDiskImageCache = new DiskLruImageCache(context, TAG, cacheSize, Bitmap.CompressFormat.JPEG, 100);

        this.mPlaceHolder = new ColorDrawable(Color.TRANSPARENT);

        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
        //TODO disconnect when the application close
    }

    /**
     * Initialise DaVinci, muse have a googleApiClient to retrieve Bitmaps from Smartphone
     *
     * @param context the application context
     * @param size    the number of entry on the cache
     */
    public static DaVinci init(Context context, int size) {
        if (INSTANCE == null)
            INSTANCE = new DaVinci(context, size);
        if (context != null)
            INSTANCE.mContext = context;
        return INSTANCE;
    }

    /**
     * Initialise DaVinci, muse have a googleApiClient to retrieve Bitmaps from Smartphone
     *
     * @param context the application context
     */
    public static DaVinci init(Context context) {
        if (INSTANCE == null)
            INSTANCE = new DaVinci(context, DEFAULT_SIZE);
        if (context != null)
            INSTANCE.mContext = context;
        return INSTANCE;
    }

    /**
     * Initialise DaVinci or retrieve the initialised one
     */
    public static DaVinci with(Context context) {
        return init(context, DEFAULT_SIZE);
    }

    public String getImageAssetName() {
        return imageAssetName;
    }

    public DaVinci setImageAssetName(String imageAssetName) {
        this.imageAssetName = imageAssetName;
        return this;
    }

    /**
     * Prepare bitmap loading
     *
     * @param path path of the bitmap to load. Path can be an url
     * @return BitmapCache instance
     */
    public DaVinci load(String path) {
        this.mPath = path;
        return this;
    }

    /**
     * Load the bitmap into this ImageView
     * Starts the treatment
     */
    public void into(ImageView imageView) {
        if (imageView != null) {
            this.mInto = imageView;
            loadImage(this.mPath,mInto);
        }
    }

    /**
     * Load the bitmap into this callback
     * Starts the treatment
     */
    public void into(Callback callback) {
        if (callback != null) {
            this.mInto = callback;
            loadImage(this.mPath,mInto);
        }
    }

    private Drawable returnDrawableIfAvailable(String path) {
        Bitmap bitmap = loadImage(path,mInto);
        if (bitmap != null && mContext != null) {
            final Drawable[] drawables = new Drawable[]{
                    mPlaceHolder,
                    new BitmapDrawable(mContext.getResources(), bitmap)
            };
            TransitionDrawable transitionDrawable = new TransitionDrawable(drawables);
            transitionDrawable.startTransition(500);
            return transitionDrawable;
        }

        return null;
    }

    /**
     * Load the bitmap into a GridPagerAdapter ar row [row]
     */
    public Drawable into(final GridPagerAdapter adapter, final int row) {
        mInto = adapter;

        Drawable drawable = returnDrawableIfAvailable(mPath);
        if (drawable != null)
            return drawable;
        else {
            into(new Callback() {
                @Override
                public void onBitmapLoaded(String path, Bitmap bitmap) {
                    if (adapter != null)
                        adapter.notifyRowBackgroundChanged(row);
                }
            });
        }
        return mPlaceHolder;
    }

    /**
     * Load the bitmap into a GridPagerAdapter at Page [row] / [column]
     */
    public Drawable into(final GridPagerAdapter adapter, final int row, final int column) {
        mInto = adapter;
        Drawable drawable = returnDrawableIfAvailable(mPath);
        if (drawable != null)
            return drawable;
        else {
            into(new Callback() {
                @Override
                public void onBitmapLoaded(String path, Bitmap bitmap) {
                    if (adapter != null)
                        adapter.notifyPageBackgroundChanged(row, column);
                }
            });
        }
        return mPlaceHolder;
    }

    /**
     * Load the bitmap into a TransitionDrawable
     * Starts the treatment, when loaded, execute a transition
     */
    public Drawable get() {
        final Drawable[] drawables = new Drawable[]{
                mPlaceHolder,
                new BitmapDrawable(mContext.getResources(), Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)),
        };

        final TransitionDrawable transitionDrawable = new TransitionDrawable(drawables);
        into(new Callback() {
            @Override
            public void onBitmapLoaded(String path, Bitmap bitmap) {
                Log.d(TAG, "callback " + path + " called");
                if (bitmap != null) {
                    Log.d(TAG, "bitmap " + path + " loaded");

                    Drawable drawable = drawables[1];
                    if (drawable != null && drawable instanceof BitmapDrawable) {
                        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawables[1];

                        try {
                            Method method = BitmapDrawable.class.getDeclaredMethod("setBitmap", Bitmap.class);
                            method.setAccessible(true);
                            method.invoke(bitmapDrawable, bitmap);

                            Log.d(TAG, "bitmap " + path + " added to transition");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    transitionDrawable.startTransition(500);
                    Log.d(TAG, "image " + path + " transition started");
                }

            }
        });

        return transitionDrawable;
    }

    public void clear() {
        if (mImagesCache != null)
            mImagesCache.evictAll();
        if (mDiskImageCache != null)
            mDiskImageCache.clearCache();
    }

    private static void loadBitmap(Bitmap bitmap, String path, Object into) {
        if (into != null && path != null && bitmap != null) {
            if (into instanceof ImageView) {
                Log.d(TAG, "return bitmap " + path + " into ImageView");
                ((ImageView) into).setImageBitmap(bitmap);
            }
            else if (into instanceof Callback) {
                Log.d(TAG, "return bitmap " + path + " into Callback");
                ((Callback) into).onBitmapLoaded(path, bitmap);
            }
        }
    }

    /**
     * Start the loading of an image
     * @param path path of the bitmap Or url of the image
     * @param into element which will display the image
     * @return the image from cache
     */
    private Bitmap loadImage(final String path, final Object into) {
        if (mInto == null || mImagesCache == null)
            return null;

        Bitmap bitmap = null;

        //image from real path (like /image/0)
        int indexOfPath = mIndexes.indexOf(path);
        if (indexOfPath >= 0)
            bitmap = mImagesCache.get(indexOfPath);

        //image from url
        if(bitmap == null) {
            indexOfPath = mIndexes.indexOf(DAVINCI_PATH+path.hashCode());
            if (indexOfPath >= 0)
                bitmap = mImagesCache.get(indexOfPath);
        }

        Log.d(TAG, "load(" + path +")");

        Log.d(TAG, "bitmap from lruCache " + bitmap);
        if (bitmap != null) { //load directly from cache
            loadBitmap(bitmap,path,into);
            Log.d(TAG, "image " + path + " available in the cache");
        } else {
            Log.d(TAG, "image " + path + " not available in the cache, trying to download it");

            if (path.startsWith("http") || path.startsWith("www")) {
                Log.d(TAG,"loadImage "+path+" send request to smartphone");
                addIntoWaiting(path, into);
            } else {
                //download the bitmap from bluetooth
                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        Bitmap bitmap = getBitmap(path);
                        Log.d(TAG, "bitmap from bluetooth "+path+ " " + bitmap);
                        if (bitmap != null && mImagesCache != null) {
                            Log.d(TAG, "save bitmap " + path + " into cache");

                            if (!mIndexes.contains(path)) {
                                int index = mIndexes.size();
                                mIndexes.add(path);

                                mImagesCache.put(index, bitmap);
                                mDiskImageCache.put(path.hashCode() + "", bitmap);
                            }

                            Log.d(TAG,mIndexes.toString());

                        }
                        return bitmap;
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        loadBitmap(bitmap,path,into);
                    }
                }.execute();
            }
        }
        return bitmap;
    }

    private void addIntoWaiting(String path, Object into) {
        String pathId = path.hashCode()+"";
        synchronized(mIntoWaiting) {
            if (path != null && into != null && mIntoWaiting != null) {
                ArrayList<Object> intos = mIntoWaiting.get(pathId);
                if (intos == null) {
                    intos = new ArrayList<>();
                    intos.add(into);
                    mIntoWaiting.put(pathId, intos);
                    sendMessage(DAVINCI_PATH, path);
                }else{
                    //already waiting for this path
                    intos.add(into);
                }
            }
        }
    }

    private void callIntoWaiting(final String path) {
        String pathId = path.replace(DAVINCI_PATH, "");

        Log.d(TAG,mIntoWaiting.toString());

        synchronized(mIntoWaiting) {
            if (path != null && mIntoWaiting != null) {
                ArrayList<Object> intos = mIntoWaiting.get(pathId);
                if (intos != null) {
                    for (int i = 0; i < intos.size(); ++i) {
                        Object into = intos.get(i);
                        Log.d(TAG, "callIntoWaiting-loadImage " + path + " into " + into.getClass().toString());
                        loadImage(path, into);
                    }
                    intos.clear();
                    mIntoWaiting.remove(path);
                }
            }
        }
    }

    private Bitmap getBitmap(String path) {
        if (mApiClient != null) {
            return getBitmapFromDataApi(path);
        } else
            return null;
    }

    public void loadFromDiskCache() {
        if (mDiskImageCache != null && mImagesCache != null) {
            for (int i = 0; i < mSize; ++i) {
                Bitmap bitmap = mDiskImageCache.getBitmap("" + i);
                if (bitmap != null)
                    mImagesCache.put(i, bitmap);
            }
        }
    }


    public Bitmap getBitmapFromDataApi(String path) {
        final Uri uri = getUriForDataItem(path);

        Log.d(TAG, "Load bitmap " + path + " " + uri.toString());

        if (uri != null) {
            final DataApi.DataItemResult result = Wearable.DataApi.getDataItem(mApiClient, uri).await();
            if (result != null && result.getDataItem() != null) {

                Log.d(TAG, "From DataApi");

                final DataMapItem dataMapItem = DataMapItem.fromDataItem(result.getDataItem());
                final Asset firstAsset = dataMapItem.getDataMap().getAsset(imageAssetName);
                if (firstAsset != null) {
                    Bitmap bitmap = loadBitmapFromAsset(firstAsset);
                    return bitmap;
                }
            }
        }

        Log.d(TAG, "can't find " + path + " [" + imageAssetName + "] in DataApi");

        return null;
    }

    private String getRemoteNodeId() {
        final NodeApi.GetConnectedNodesResult nodesResult = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
        final List<Node> nodes = nodesResult.getNodes();
        if (nodes != null && nodes.size() > 0) {
            return nodes.get(0).getId();
        }
        return null;
    }

    protected Uri getUriForDataItem(String path) {
        // If you've put data on the local node
        //String nodeId = getLocalNodeId();
        // Or if you've put data on the remote node
        final String nodeId = getRemoteNodeId();
        // Or If you already know the node id
        // String nodeId = "some_node_id";
        return new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path(path).build();
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        final ConnectionResult result =
                mApiClient.blockingConnect(3000, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        final InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mApiClient, asset).await().getInputStream();
        //mApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
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
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
        Wearable.DataApi.addListener(mApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent dataEvent : dataEvents){
            String path = dataEvent.getDataItem().getUri().getPath();
            Log.d(TAG,"onDataChanged("+path+")");
            if (path.startsWith(DAVINCI_PATH)){
                Log.d(TAG,"davinci-onDataChanged "+path);

                callIntoWaiting(path);
            }
        }
    }

    public interface Callback {
        public void onBitmapLoaded(String path, Bitmap bitmap);
    }

}
