package com.github.florent37.davinci;

import com.github.florent37.davinci.transformation.Transformation;

/**
 * Created by florentchampigny on 02/04/15.
 */
public class WaintingContainer {
    private Object into;
    private Transformation transformation;

    public WaintingContainer(Object into, Transformation transformation) {
        this.into = into;
        this.transformation = transformation;
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
    }

    public Object getInto() {
        return into;
    }

    public void setInto(Object into) {
        this.into = into;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WaintingContainer that = (WaintingContainer) o;

        if (into != null ? !into.equals(that.into) : that.into != null) return false;
        if (transformation != null ? !transformation.equals(that.transformation) : that.transformation != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = into != null ? into.hashCode() : 0;
        result = 31 * result + (transformation != null ? transformation.hashCode() : 0);
        return result;
    }
}
