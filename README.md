DaVinci
=======

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-DaVinci-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1678)
[![Android Weekly](https://img.shields.io/badge/android--weekly-147-blue.svg)](http://androidweekly.net/issues/issue-147)

![Alt DaVinciDroid](https://raw.githubusercontent.com/florent37/DaVinci/master/mobile/src/main/res/drawable-hdpi/davinci_new_small.jpg)

DaVinci is an image downloading and caching library for Android Wear


<a href="https://goo.gl/WXW8Dc">
  <img alt="Android app on Google Play" src="https://developer.android.com/images/brand/en_app_rgb_wo_45.png" />
</a>


Usage
--------

Use DaVinci from your SmartWatch app
```java
DaVinci.with(context).load("/image/0").into(imageView);
DaVinci.with(context).load("http://i.imgur.com/o3ELrbX.jpg").into(imageView);
```

Into an imageview
```java
DaVinci.with(context).load("/image/0").into(imageView);
```

Into a FragmentGridPagerAdapter
```java
@Override
public Drawable getBackgroundForRow(final int row) {
    return DaVinci.with(context).load("/image/" + row).into(this, row);
}
```

Into a CallBack
```java
DaVinci.with(context).load("http://i.imgur.com/o3ELrbX.jpg").into(new DaVinci.Callback() {
            @Override
            public void onBitmapLoaded(String path, Bitmap bitmap) {

            }
});
```

By default, the asset name used for the bitmap is "image", you can modify this 
```java
DaVinci.with(context).load("/image/0").setImageAssetName("myImage").into(imageView);
```

Send Bitmaps
--------

In your smartphone service
```java
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        DaVinciDaemon.with(getApplicationContext()).handleMessage(messageEvent);
        ...
    }
```

Preload Bitmaps
--------

Send image to wear
```java
DaVinciDaemon.with(getApplicationContext()).load("http://i.imgur.com/o3ELrbX.jpg").send();
```

or with "/image/0" path
```java
DaVinciDaemon.with(getApplicationContext()).load("http://i.imgur.com/o3ELrbX.jpg").into("/image/0");
```

Image Transformation
--------

You can specify custom transformations on your Bitmaps

```java
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
```

Pass an instance of this class to the transform method

```java
DaVinci.with(context).load(url).transform(new ResizeTransformation(300)).into(imageView);
```

Prodvided Transformations :

**Blur**
```java
DaVinci.with(context).load(url).transform(new BlurTransformation()).into(imageView);
```

**Resizing**
```java
DaVinci.with(context).load(url).transform(new ResizeTransformation(maxWidth)).into(imageView);
```

Download
--------

In your wear module [![Download](https://api.bintray.com/packages/florent37/maven/DaVinci/images/download.svg)](https://bintray.com/florent37/maven/DaVinci/_latestVersion)
```groovy
compile ('com.github.florent37:davinci:1.0.3@aar'){
    transitive = true
}
```

In your smartphone module  [![Download](https://api.bintray.com/packages/florent37/maven/DaVinciDaemon/images/download.svg)](https://bintray.com/florent37/maven/DaVinciDaemon/_latestVersion)
```groovy
compile ('com.github.florent37:davincidaemon:1.0.3@aar'){
     transitive = true
}
```

Don't forget to add WRITE_EXTERNAL_STORAGE in your Wear AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

TODO
--------

- Customize bitmap resizing (actually : width=300px)
- Enabling multiples transformations
- Apply transformations on Smartphone then send them to Wear

Community
--------

Looking for contributors, feel free to fork !

Wear
--------

If you want to learn wear development : [http://tutos-android-france.com/developper-une-application-pour-les-montres-android-wear/][tuto_wear].

Dependencies
-------

* [Picasso][picasso] used in DaVinciDaemon (from Square)
* [DiskLruCache][disklrucache] used in DaVinci (from JakeWharton)

Changelog
-------

**1.0.2**
- Bitmaps are now saved as PNG to preserve transparency

Credits
-------

Author: Florent Champigny www.florentchampigny.com/


<a href="https://goo.gl/WXW8Dc">
  <img alt="Android app on Google Play" src="https://developer.android.com/images/brand/en_app_rgb_wo_45.png" />
</a>

<a href="https://plus.google.com/+florentchampigny">
  <img alt="Follow me on Google+"
       src="https://raw.githubusercontent.com/florent37/DaVinci/master/mobile/src/main/res/drawable-hdpi/gplus.png" />
</a>
<a href="https://twitter.com/florent_champ">
  <img alt="Follow me on Twitter"
       src="https://raw.githubusercontent.com/florent37/DaVinci/master/mobile/src/main/res/drawable-hdpi/twitter.png" />
</a>
<a href="https://www.linkedin.com/profile/view?id=297860624">
  <img alt="Follow me on LinkedIn"
       src="https://raw.githubusercontent.com/florent37/DaVinci/master/mobile/src/main/res/drawable-hdpi/linkedin.png" />
</a>


Pictures by Logan Bourgouin

<a href="https://plus.google.com/+LoganBOURGOIN">
  <img alt="Follow me on Google+"
       src="https://raw.githubusercontent.com/florent37/DaVinci/master/mobile/src/main/res/drawable-hdpi/gplus.png" />
</a>

License
--------

    Copyright 2015 florent37, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[snap]: https://oss.sonatype.org/content/repositories/snapshots/
[android_doc]: https://developer.android.com/training/wearables/data-layer/assets.html
[tuto_wear]: http://tutos-android-france.com/developper-une-application-pour-les-montres-android-wear/
[picasso]: https://github.com/square/picasso
[disklrucache]: https://github.com/JakeWharton/DiskLruCache
