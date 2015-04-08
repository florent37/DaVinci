package com.github.florent37.davinci;

/**
 * Created by florentchampigny on 02/04/15.
 */
public abstract class BitmapCallBack implements DaVinci.Callback{
    private Object into;

    public BitmapCallBack(Object into) {
        this.into = into;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BitmapCallBack that = (BitmapCallBack) o;

        if (into != null ? !into.equals(that.into) : that.into != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return into != null ? into.hashCode() : 0;
    }

}
