package org.joints.commons.pregel.api;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Created by fan on 2017/1/17.
 */
public class Message<V extends Serializable & Comparable<V>> extends BaseObj {
	private long destVertexID;

	private long srcVertexID;

	private long superStep;

	private V value;

	public Message() {
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Message))
			return false;
		Message<?> message = (Message<?>) o;
		return getSrcVertexID() == message.getSrcVertexID() && getDestVertexID() == message.getDestVertexID() && getSuperStep() == message.getSuperStep() && Objects.equals(getValue(), message.getValue());
	}

	public long getDestVertexID() {
		return destVertexID;
	}

	public long getSrcVertexID() {
		return srcVertexID;
	}

	public long getSuperStep() {
		return superStep;
	}

	public V getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getValue(), getSrcVertexID(), getDestVertexID(), getSuperStep());
	}

	public void setDestVertexID(long destVertexID) {
		this.destVertexID = destVertexID;
	}

	public void setSrcVertexID(long srcVertexID) {
		this.srcVertexID = srcVertexID;
	}

	public void setSuperStep(long superStep) {
		this.superStep = superStep;
	}

	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put("superStep", getSuperStep());
		map.put("srcVertexID", getSrcVertexID());
		map.put("destVertexID", getDestVertexID());
		map.put("value", getValue());
		return map;
	}
}
