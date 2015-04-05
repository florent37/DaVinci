package com.github.florent37.davinci.transformation;

import android.graphics.Bitmap;

/**
 * Created by florentchampigny on 22/03/15.
 */
public class ResizeTransformation implements Transformation {
    private int targetWidth;

    public ResizeTransformation(int width) {
        this.targetWidth = width;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        double aspectRatio = (double) source.getHeight() / (double) source.getWidth();
        int targetHeight = (int) (targetWidth * aspectRatio);
        Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
        if (result != source) {
            // Same bitmap is returned if sizes are the same
            source.recycle();
        }
        return result;
    }

    @Override
    public String key() {
        return "ResizeTransformation"+targetWidth;
    }
}
