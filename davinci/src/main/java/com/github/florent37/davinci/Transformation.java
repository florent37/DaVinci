package com.github.florent37.davinci;

import android.graphics.Bitmap;

/**
 * Created by florentchampigny on 02/04/15.
 */
public abstract class Transformation {

    public abstract Bitmap transform(Bitmap bitmap);

    public abstract String getKey();
}
