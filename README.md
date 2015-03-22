DaVinci
=======

DaVinci is an image downloading and caching library for Android Wear

Download
--------

Download via Gradle:
```groovy
allprojects {
    repositories {
        jcenter()
        maven {
            url "http://dl.bintray.com/florent37/maven"
        }
    }
}
```

In your wear module
```groovy
compile 'com.florent37.davinci:davinci:1.0.1@aar'
```

In your smartphone module
```groovy
compile 'com.florent37.davinci:davincidaemon:1.0.1@aar'
```


Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

Usage
--------

Don't forget to add WRITE_EXTERNAL_STORAGE in your Wear AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

Initialise DaVinci in your Activity
```java

@Override
public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
        Wearable.DataApi.addListener(mApiClient, this);

        DaVinci.init(this, mApiClient);
}

```

Load old bitmaps cached into disk

```java
DaVinci.init(this, mApiClient).loadFromDiskCache();
```

And use it wherever you want 

Into an imageview
```java
DaVinci.with(context).load("/image/0").into(imageView);
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
DaVinci.with(context).load("/image/" + row).into(new DaVinci.Callback() {
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
    public void onConnected(Bundle bundle) {
        DaVinciDaemon.init(getApplicationContext(),mApiClient);
    }
```

```java
DaVinciDaemon.with(getApplicationContext()).load("http://i.imgur.com/o3ELrbX.jpg").into("/image/0");
```

ToDo
--------

* GoogleApiClient included into DaVinci
* Service added into the smartphone module
* Use URL to display bitmap onto your Wear

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