package org.joints.commons.pregel.api;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by fan on 2017/1/17.
 */
public class Vertex<V extends Serializable & Comparable<V>> extends BaseObj {

	enum State {
		ACTIVE, INACTIVE
	}

	private LinkedList<Message<?>> msgList = new LinkedList<>();

	private LinkedHashSet<Edge<?>> outgoingEdges = new LinkedHashSet<>();

	private State state;

	private V value;

	public Vertex() {
	}

	public Vertex(V value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}

	public LinkedList<Message<?>> getMsgList() {
		return msgList;
	}

	public LinkedHashSet<Edge<?>> getOutgoingEdges() {
		return outgoingEdges;
	}

	public State getState() {
		return state;
	}

	public V getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public void setMsgList(LinkedList<Message<?>> msgList) {
		this.msgList = msgList;
	}

	public void setOutgoingEdges(LinkedHashSet<Edge<?>> outgoingEdges) {
		this.outgoingEdges = outgoingEdges;
	}

	public void setState(State state) {
		this.state = state;
	}

	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put("value", getValue());
		return map;
	}
}
