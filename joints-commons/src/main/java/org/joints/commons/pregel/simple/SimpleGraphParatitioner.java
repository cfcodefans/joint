package org.joints.commons.pregel.simple;

import org.joints.commons.pregel.partition.IGraphPartition;
import org.joints.commons.pregel.partition.IGraphPartitioner;

import java.util.Collection;

/**
 * Created by fan on 2017/1/20.
 */
public class SimpleGraphParatitioner implements IGraphPartitioner {
    @Override
    public Collection<IGraphPartition> partitions() {
        return null;
    }

    @Override
    public IGraphPartition getPartition(long vid) {
        return null;
    }
}
