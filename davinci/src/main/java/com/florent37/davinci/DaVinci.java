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
import android.support.wearable.view.GridPagerAdapter;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DaVinci {

    public static final String TAG = DaVinci.class.getCanonicalName();

    private static final int DEFAULT_SIZE = 10;

    private String imageAssetName = "image";

    private int mSize;
    private Context mContext;
    private LruCache<Integer, Bitmap> mImagesCache;
    private DiskLruImageCache mDiskImageCache;

    private ArrayList<String> indexes = new ArrayList<>();

    private GoogleApiClient mApiClient;

    public static DaVinci INSTANCE;

    private Drawable mPlaceHolder;
    private String mPath;
    private Object mInto;

    /**
     * Initialise DaVinci, muse have a googleApiClient to retrieve Bitmaps from Smartphone
     *
     * @param context         the application context
     * @param size            the number of entry on the cache
     * @param googleApiClient the google api client used by the wear application
     */
    private DaVinci(Context context, int size, GoogleApiClient googleApiClient) {
        this.mImagesCache = new LruCache<>(size);
        this.mSize = size;

        int cacheSize = 20 * 1024 * 1024; //20mo of disk cache
        this.mDiskImageCache = new DiskLruImageCache(context, TAG, cacheSize, Bitmap.CompressFormat.JPEG, 100);

        this.mApiClient = googleApiClient;
        this.mPlaceHolder = new ColorDrawable(Color.TRANSPARENT);
    }

    /**
     * Initialise DaVinci, muse have a googleApiClient to retrieve Bitmaps from Smartphone
     *
     * @param context         the application context
     * @param size            the number of entry on the cache
     * @param googleApiClient the google api client used by the wear application
     */
    public static DaVinci init(Context context, int size, GoogleApiClient googleApiClient) {
        if (INSTANCE == null)
            INSTANCE = new DaVinci(context, size, googleApiClient);
        if (context != null)
            INSTANCE.mContext = context;
        return INSTANCE;
    }

    /**
     * Initialise DaVinci, muse have a googleApiClient to retrieve Bitmaps from Smartphone
     *
     * @param context         the application context
     * @param googleApiClient the google api client used by the wear application
     */
    public static DaVinci init(Context context, GoogleApiClient googleApiClient) {
        if (INSTANCE == null)
            INSTANCE = new DaVinci(context, DEFAULT_SIZE, googleApiClient);
        if (context != null)
            INSTANCE.mContext = context;
        return INSTANCE;
    }

    /**
     * Initialise DaVinci or retrieve the initialised one
     */
    public static DaVinci with(Context context) {
        return init(context, DEFAULT_SIZE, null);
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
     * @param path path of the bitmap to load
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
            loadImage(this.mPath);
        }
    }

    /**
     * Load the bitmap into this callback
     * Starts the treatment
     */
    public void into(Callback callback) {
        if (callback != null) {
            this.mInto = callback;
            loadImage(this.mPath);
        }
    }

    private Drawable returnDrawableIfAvailable(String path){
        Bitmap bitmap = loadImage(path);
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
        if(drawable != null)
            return drawable;
        else{
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
    public Drawable into(final GridPagerAdapter adapter,final int row,final int column) {
        mInto = adapter;
        Drawable drawable = returnDrawableIfAvailable(mPath);
        if(drawable != null)
            return drawable;
        else{
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

    private Bitmap loadImage(final String path) {
        if (mInto == null || mImagesCache == null)
            return null;

        Bitmap bitmap = null;

        int indexOfPath = indexes.indexOf(path);
        if(indexOfPath >= 0)
            bitmap = mImagesCache.get(indexOfPath);

        Log.d(TAG, "bitmap from lruCache " + bitmap);
        if (bitmap != null) { //load directly from cache
            if (mInto instanceof ImageView)
                ((ImageView) mInto).setImageBitmap(bitmap);
            else if (mInto instanceof Callback) {
                ((Callback) mInto).onBitmapLoaded(path, bitmap);
            }
            Log.d(TAG, "image" + path + " available in the cache");
        } else {
            Log.d(TAG, "image" + path + " not available in the cache, trying to download it");
            //download the bitmap
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    Bitmap bitmap = getBitmap(mPath);
                    Log.d(TAG, "bitmap from bluetooth " + bitmap);
                    if (bitmap != null && mImagesCache != null) {
                        Log.d(TAG, "save bitmap " + path + " into cache");

                        if(!indexes.contains(path)) {
                            int index = indexes.size();
                            indexes.add(path);

                            mImagesCache.put(index, bitmap);
                            mDiskImageCache.put(path.hashCode() + "", bitmap);
                        }
                    }
                    return bitmap;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (mInto != null && bitmap != null) {
                        if (mInto instanceof ImageView) {
                            Log.d(TAG, "return bitmap " + path + " into ImageView");
                            ((ImageView) mInto).setImageBitmap(bitmap);
                        } else if (mInto instanceof Callback) {
                            Log.d(TAG, "return bitmap " + path + " into Callback");
                            ((Callback) mInto).onBitmapLoaded(path, bitmap);
                        }
                    }
                }
            }.execute();
        }
        return bitmap;
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

        Log.d(TAG, "can't find "+path+" ["+imageAssetName+"] in DataApi");

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

    public interface Callback {
        public void onBitmapLoaded(String path, Bitmap bitmap);
    }

}
