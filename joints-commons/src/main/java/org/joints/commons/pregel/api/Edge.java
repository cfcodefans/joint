package org.joints.commons.pregel.api;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Created by fan on 2017/1/17.
 */
public class Edge<V extends Serializable & Comparable<V>> extends BaseObj {
    private long destVertexID;

    private long srcVertexID;

    private V value;

    public Edge() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge<?> edge = (Edge<?>) o;
        return getSrcVertexID() == edge.getSrcVertexID() &&
            getDestVertexID() == edge.getDestVertexID() &&
            Objects.equals(getValue(), edge.getValue());
    }
    public long getDestVertexID() {
        return destVertexID;
    }

    public long getSrcVertexID() {
        return srcVertexID;
    }

    public V getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue(), getSrcVertexID(), getDestVertexID());
    }

    public void setDestVertexID(long destVertexID) {
        this.destVertexID = destVertexID;
    }

    public void setSrcVertexID(long srcVertexID) {
        this.srcVertexID = srcVertexID;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("srcVertexID", getSrcVertexID());
        map.put("destVertexID", getDestVertexID());
        map.put("value", getValue());
        return map;
    }
}
