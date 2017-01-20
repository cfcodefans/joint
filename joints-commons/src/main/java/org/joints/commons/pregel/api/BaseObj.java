package org.joints.commons.pregel.api;

import org.joints.commons.Jsons;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by fan on 2017/1/17.
 */
public abstract class BaseObj implements Serializable {
    private long id;

    private Date updated = new Date();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseObj)) return false;
        BaseObj baseObj = (BaseObj) o;
        return getId() == baseObj.getId();
    }

    public long getId() {
        return id;
    }

    public Date getUpdated() {
        return updated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public void setId(long id) {
        this.id = id;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", this.getClass().toString());
        map.put("id", getId());
        map.put("updated", getUpdated());
        return map;
    }

    @Override
    public String toString() {
        return Jsons.toString(toMap());
    }
}
