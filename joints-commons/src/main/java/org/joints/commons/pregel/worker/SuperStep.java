package org.joints.commons.pregel.worker;

import org.joints.commons.pregel.api.BaseObj;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by fan on 2017/1/20.
 */
public class SuperStep extends BaseObj {
    protected final AtomicLong ID = new AtomicLong(0);

    public SuperStep() {
        setId(ID.getAndIncrement());
    }
}
