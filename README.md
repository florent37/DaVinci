DaVinci
=======

[![Build Status](https://travis-ci.org/florent37/DaVinci.svg?branch=master)](https://travis-ci.org/florent37/DaVinci)


DaVinci is an image downloading and caching library for Android Wear

Download
--------

In your wear module [![Download](https://api.bintray.com/packages/florent37/maven/DaVinci/images/download.svg)](https://bintray.com/florent37/maven/DaVinci/_latestVersion)
```groovy
compile 'com.florent37.davinci:davinci:1.0.0@aar'
```

In your smartphone module  [![Download](https://api.bintray.com/packages/florent37/maven/DaVinciDaemon/images/download.svg)](https://bintray.com/florent37/maven/DaVinciDaemon/_latestVersion)
```groovy
compile 'com.florent37.davinci:davincidaemon:1.0.0@aar'
```


Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

Usage
--------

Don't forget to add WRITE_EXTERNAL_STORAGE in your Wear AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

And use it wherever you want
```java
DaVinci.with(context).load("/image/0").into(imageView);
DaVinci.with(context).load("http://i.imgur.com/o3ELrbX.jpg").into(imageView);
```

Into a an imageview
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

Community
--------

Looking for contributors, feel free to fork !

Wear
--------

If you want to learn wear development : [http://tutos-android-france.com/developper-une-application-pour-les-montres-android-wear/][tuto_wear].

Credits
-------

Author: Florent Champigny

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