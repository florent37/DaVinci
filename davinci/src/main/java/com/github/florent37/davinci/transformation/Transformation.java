package com.github.florent37.davinci.transformation;

import android.graphics.Bitmap;

/**
 * Created by florentchampigny on 02/04/15.
 */
public interface Transformation {

    public abstract Bitmap transform(Bitmap bitmap);

    public abstract String key();
}
