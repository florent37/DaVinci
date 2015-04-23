package com.github.florent37.davinci;

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
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.GridPagerAdapter;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.github.florent37.davinci.transformation.Transformation;
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

    private final static String DAVINCI_ASSET_IMAGE = "image";

    private String imageAssetName = DAVINCI_ASSET_IMAGE;

    private final int cacheSize = 20 * 1024 * 1024; //20mo of disk cache

    private int mSize;
    private Context mContext;
    private LruCache<Integer, Bitmap> mImagesCache;
    private DiskLruImageCache mDiskImageCache;

    private Map<String, ArrayList<WaintingContainer>> mIntoWaiting = new HashMap<>();

    private GoogleApiClient mApiClient;

    public static DaVinci INSTANCE;

    private Drawable mPlaceHolder;
    private String mPath;
    private Object mInto;
    private Transformation mTransformation;

    //region initialisation

    /**
     * Initialise DaVinci, muse have a googleApiClient to retrieve Bitmaps from Smartphone
     *
     * @param context the application context
     * @param size    the number of entry on the cache
     */
    private DaVinci(Context context, int size) {

        Log.d(TAG, "====================================");

        this.mSize = size;
        this.mContext = context;
        this.mImagesCache = new LruCache<>(mSize);
        this.mDiskImageCache= new DiskLruImageCache(mContext, TAG, cacheSize, Bitmap.CompressFormat.PNG, 100);

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
     * Initialise DaVinci or retrieve the initialised one
     */
    public static DaVinci with(Context context) {
        return init(context, DEFAULT_SIZE);
    }

    //endregion

    //region parameters

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
        this.resetParameters();

        this.mPath = path;
        return this;
    }

    public DaVinci transform(Transformation transformation) {
        this.mTransformation = transformation;
        return this;
    }

    /**
     * Load the bitmap into this ImageView
     * Starts the treatment
     */
    public void into(ImageView imageView) {
        intoObject(imageView);
    }

    /**
     * Load the bitmap into this callback
     * Starts the treatment
     */
    public void into(final Callback callback) {
        intoObject(callback);
    }

    //endregion

    private void intoObject(final Object objectInto) {
        if (objectInto != null) {
            mInto = objectInto;

            final String path = mPath;
            final Object into = objectInto;
            final Transformation transformation = mTransformation;

            //no need to retrieve image directly
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    loadImage(path, into, transformation);
                    return null;
                }
            }.execute();

            this.resetParameters();
        }
    }

    private Drawable returnDrawableIfAvailable(final String path, final Transformation transformation) {
        Bitmap bitmap = null;
        if (transformation == null) {
            bitmap = loadFromLruCache(path, false);
        } else {
            bitmap = loadFromLruCache(generatePathFromTransformation(path, transformation), false);
        }

        if (bitmap != null && mContext != null) {
            return new BitmapDrawable(mContext.getResources(), bitmap);
        }

        return null;
    }

    /**
     * Load the bitmap into a GridPagerAdapter ar row [row]
     */
    public Drawable into(final GridPagerAdapter adapter, final int row) {
        final String path = mPath;
        final Transformation transformation = mTransformation;

        final Drawable drawable = returnDrawableIfAvailable(path, transformation);
        if (drawable != null)
            return drawable;
        else {
            into(new BitmapCallBack(adapter) {
                @Override
                public void onBitmapLoaded(String path, Bitmap bitmap) {
                    if (adapter != null)
                        adapter.notifyRowBackgroundChanged(row);
                }
            });
        }

        this.resetParameters();

        return mPlaceHolder;
    }

    /**
     * Load the bitmap into a GridPagerAdapter at Page [row] / [column]
     */
    public Drawable into(final GridPagerAdapter adapter, final int row, final int column) {
        final String path = mPath;
        final Transformation transformation = mTransformation;

        Drawable drawable = returnDrawableIfAvailable(path, transformation);
        if (drawable != null)
            return drawable;
        else {
            into(new BitmapCallBack(adapter) {
                @Override
                public void onBitmapLoaded(String path, Bitmap bitmap) {
                    if (adapter != null)
                        adapter.notifyPageBackgroundChanged(row, column);
                }
            });
        }

        this.resetParameters();

        return mPlaceHolder;
    }

    private void resetParameters() {
        this.mTransformation = null;
        this.mInto = null;
        this.mPath = null;
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

    /**
     * Clear both cache
     */
    public void clear() {
        if (mImagesCache != null)
            mImagesCache.evictAll();
        if (mDiskImageCache != null)
            mDiskImageCache.clearCache();
    }

    /**
     * When the bitmap has been downloaded, load it into the [into] object
     *
     * @param bitmap the downloaded bitmap
     * @param path   the image source path (path or url)
     * @param into   the destination object
     */
    private static void returnBitmapInto(final Bitmap bitmap, final String path, final Object into) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (into != null && path != null && bitmap != null) {
                    if (into instanceof ImageView) {
                        Log.d(TAG, "return bitmap " + path + " into ImageView");
                        ((ImageView) into).setImageBitmap(bitmap);
                    } else if (into instanceof Callback) {
                        Log.d(TAG, "return bitmap " + path + " into Callback");
                        ((Callback) into).onBitmapLoaded(path, bitmap);
                    }
                }
            }
        });
    }

    /**
     * Start the loading of an image
     *
     * @param path path of the bitmap Or url of the image
     * @param into element which will display the image
     * @return the image from cache
     */
    private Bitmap loadImage(final String path, final Object into, final Transformation transformation) {
        if (into == null || mImagesCache == null || path == null || path.trim().isEmpty())
            return null;

        Log.d(TAG, "load(" + path + ")");

        Bitmap bitmap = null;

        if (transformation == null) {
            bitmap = loadFromLruCache(path, true);
        } else {
            final String pathTransformed = generatePathFromTransformation(path, transformation);
            bitmap = loadFromLruCache(pathTransformed, true);

            if (bitmap == null) { //if bitmap transformed not found in cache
                bitmap = loadFromLruCache(path, true); //try to get the bitmap without transformation
                if (bitmap != null) {
                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            return transformAndSaveBitmap(path, transformation);
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            super.onPostExecute(bitmap);
                            if (bitmap != null) {
                                //now its available on cache, can call again loadImage with same parameters
                                loadImage(path, into, transformation);
                            }
                        }
                    }.execute();
                    return null;
                }
            }
        }

        Log.d(TAG, "bitmap from cache " + bitmap + " for " + path);

        if (bitmap != null) { //load directly from cache
            returnBitmapInto(bitmap, path, into);
            Log.d(TAG, "image " + path + " available in the cache");
        } else {
            Log.d(TAG, "image " + path + " not available in the cache, trying to download it");

            if (isUrlPath(path)) {
                Log.d(TAG, "loadImage " + path + " send request to smartphone " + path.hashCode());
                addIntoWaiting(path, into, transformation);
            } else {
                downloadBitmap(path, into, transformation);
            }
        }
        return bitmap;
    }

    private String generatePathFromTransformation(final String path, final Transformation transformation) {
        if (transformation == null)
            return path;
        else
            return path + "|||" + transformation.key();
    }

    private boolean isUrlPath(final String path) {
        return path.startsWith("http") || path.startsWith("www");
    }

    private Bitmap transformAndSaveBitmap(final String path, final Transformation transformation) {
        Bitmap originalBitmap = loadFromDiskLruCache(getKey(path)); //try to get the bitmap without transformation

        final String pathTransformed = generatePathFromTransformation(path, transformation);

        if (originalBitmap != null) {
            //transform & save the bitmap
            Bitmap transformedBitmap = transformation.transform(originalBitmap);
            saveBitmap(getKey(pathTransformed), transformedBitmap);

            return transformedBitmap;
        }
        return null;
    }

    private int getKey(final String path) {
        int key = path.hashCode();

        if (isUrlPath(path)) {
            final String imagePath = DAVINCI_PATH + key;
            key = imagePath.hashCode();

            Log.d(TAG, "key " + path + " = " + imagePath + " key=" + key);
        }

        Log.d(TAG, "key for " + path + " is " + key);

        return key;
    }

    /**
     * Try to load [path] image from cache
     *
     * @param path Path or Url of the bitmap
     * @return Bitmap from cache if founded
     */
    private Bitmap loadFromLruCache(final String path, boolean tryFromDisk) {

        int key = getKey(path);

        Bitmap bitmap = mImagesCache.get(key); //try to retrieve from lruCache

        Log.d(TAG, "bitmap " + path + " from lruCache [" + key + "] " + bitmap);

        if (tryFromDisk && bitmap == null) {
            bitmap = loadFromDiskLruCache(key); //try to retrieve from disk cache

            Log.d(TAG, "bitmap " + path + " from diskLruCache " + bitmap);
            if (bitmap != null) { //if found on disk cache
                mImagesCache.put(key, bitmap); //save it into lruCache
            }
        }

        return bitmap;
    }

    private Bitmap loadFromDiskLruCache(final int key) {
        Log.d(TAG, "try to load from disk cache " + key);

        Bitmap bitmap = mDiskImageCache.getBitmap(key + "");
        return bitmap;
    }

    /**
     * Download bitmap from bluetooth asset and store it into LruCache and LruDiskCache
     *
     * @param path the path or url of the image
     * @param into the destination object
     */
    private void downloadBitmap(final String path, final Object into, final Transformation transformation) {
        //download the bitmap from bluetooth
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap bitmap = getBitmap(path);
                Log.d(TAG, "bitmap from bluetooth " + path + " " + bitmap);
                if (bitmap != null) {
                    Log.d(TAG, "save bitmap " + path + " into cache");
                    saveBitmap(getKey(path), bitmap);

                    if (transformation != null) {
                        return transformAndSaveBitmap(path, transformation);
                    }
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (into != null)
                    returnBitmapInto(bitmap, path, into);
            }
        }.execute();
    }

    private void saveBitmap(final int key, final Bitmap bitmap) {
        Log.d(TAG, "save bitmap " + key + " into cache");

        if (mImagesCache != null) {
            //save the image into LruCache
            mImagesCache.put(key, bitmap);
        }

        if (mDiskImageCache != null) {
            //save the image into LruDiskCache
            mDiskImageCache.put(key + "", bitmap);
        }
    }

    private void addIntoWaiting(final String path, final Object into, final Transformation transformation) {
        final String pathId = path.hashCode() + "";
        synchronized (mIntoWaiting) {
            if (path != null && into != null && mIntoWaiting != null) {
                ArrayList<WaintingContainer> intos = mIntoWaiting.get(pathId);
                WaintingContainer waintingContainer = new WaintingContainer(into, transformation);

                if (intos == null) {
                    intos = new ArrayList<>();
                    intos.add(waintingContainer);
                    mIntoWaiting.put(pathId, intos);
                    sendMessage(DAVINCI_PATH, path);
                } else {
                    //already waitings for this path
                    if (!intos.contains(waintingContainer))
                        intos.add(waintingContainer);
                }
            }
        }
    }

    private void callIntoWaiting(final String path) {

        final String pathId = path.replace(DAVINCI_PATH, "");

        Log.d(TAG, "callIntoWaiting " + path);
        Log.d(TAG, "mIntoWaiting=" + mIntoWaiting.toString());

        synchronized (mIntoWaiting) {
            if (mIntoWaiting != null) {

                //retrieve the waiting callbacks
                ArrayList<WaintingContainer> intos = mIntoWaiting.get(pathId);
                if (intos != null) {
                    for (int i = 0; i < intos.size(); ++i) {
                        WaintingContainer waintingContainer = intos.get(i);
                        final Object into = waintingContainer.getInto();
                        final Transformation transformation = waintingContainer.getTransformation();
                        Log.d(TAG, "callIntoWaiting-loadImage " + path + " into " + into.getClass().toString());

                        //transform the bitmap before load
                        if (transformation != null) {
                            transformAndSaveBitmap(path, transformation);
                        }

                        //download the bitmap from bluetooth or retrieve from cache, and send it to callback
                        loadImage(path, into, transformation);
                    }

                    //clear the waitings for this path
                    intos.clear();
                    mIntoWaiting.remove(pathId);
                }
            }
        }
    }

    //region DataApi

    private Bitmap getBitmap(String path) {
        if (mApiClient != null) {
            return getBitmapFromDataApi(path);
        } else
            return null;
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
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for (Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(mApiClient, node.getId(), path, message.getBytes()).await();
                }
                return null;
            }
        }.execute();
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

    //endregion

    /**
     * When received assets from DataApi
     *
     * @param dataEvents
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            String path = dataEvent.getDataItem().getUri().getPath();
            Log.d(TAG, "onDataChanged(" + path + ")");
            if (path.startsWith(DAVINCI_PATH)) { //if it's a davinci path
                Log.d(TAG, "davinci-onDataChanged " + path);

                //download the bitmap and add it to cache
                Asset asset = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap().getAsset(DAVINCI_ASSET_IMAGE);
                Bitmap bitmap = loadBitmapFromAsset(asset);
                if (bitmap != null)
                    saveBitmap(getKey(path), bitmap);

                //callbacks
                callIntoWaiting(path);
            }
        }
    }

//region CallBack

    public interface Callback {
        public void onBitmapLoaded(String path, Bitmap bitmap);
    }

//endregion

}
