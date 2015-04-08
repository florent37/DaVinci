package com.github.florent37.davinci.transformation;

import android.graphics.Bitmap;

import com.github.florent37.davinci.utils.FastBlurHelper;

/**
 * Created by florentchampigny on 01/05/2014.
 */
public class BlurTransformation implements Transformation {
    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap result = FastBlurHelper.doBlur(source, 10, false);
        if (result != source) {
            // Same bitmap is returned if sizes are the same
            source.recycle();
        }
        return result;
    }

    @Override
    public String key() {
        return "BlurTransformation2";
    }
}