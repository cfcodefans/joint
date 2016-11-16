package org.joints.commons;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Stack;

public class ProcTrace {
	private static final String INDENT = "    ";
	private StringBuilder buf = new StringBuilder();
	private final Stack<TraceEntry> stack = new Stack<TraceEntry>();
	
	public static class TraceStep implements Serializable, Cloneable {
//		final long startTime = System.currentTimeMillis();
		long perfTime;
		String stepInfo;
		public String toString() {
			return perfTime + " ms:\t" + stepInfo;
		}
	}
	
	public static class TraceSubStep extends TraceStep {
		public String toString() {
			return stepInfo;
		}
	}
	
	public static class TraceEntry implements Serializable, Cloneable {
		final long startTime = System.currentTimeMillis();
		long currentTime;
		final LinkedList<TraceStep> steps = new LinkedList<TraceStep>();
	}
	
	private static final ThreadLocal<ProcTrace> instancePool = new ThreadLocal<ProcTrace>() {
	    protected ProcTrace initialValue() {
	        return new ProcTrace();
	    }
	};
	
	public static ProcTrace instance() {
		return instancePool.get();
	}
	
	private ProcTrace() {
		
	}

	public static TraceEntry start() {
		return start(MiscUtils.invocationInfo(3));
	}
	
	public static TraceEntry start(String format, Object...args) {
		ProcTrace pt = ProcTrace.instance();
		
		final TraceEntry te = new TraceEntry();
		te.currentTime = te.startTime;
		
		TraceStep ts = new TraceStep();
		ts.perfTime = 0;
		ts.stepInfo = String.format(format, args);
		te.steps.push(ts);
		
		pt.stack.push(te);
		
		return te;
	}
	
//	public static void ongoing(String logMsg) {
//		final TraceStep newStep = new TraceStep();
//		newStep.stepInfo = logMsg;
//		ongoing(newStep);
//	}

	public static void ongoing(String format, Object...args) {
		final TraceStep newStep = new TraceStep();
		newStep.stepInfo = String.format(format, args);
		ongoing(newStep);
	}
	
	public static void ongoing(final TraceStep ts) {
		final ProcTrace pt = ProcTrace.instance();
		
		if (pt.stack.isEmpty()) {
			start(ts.stepInfo);
			return;
		}
		
		final TraceEntry te = pt.stack.peek();
		final TraceStep lastStep = te.steps.peekLast();
		final long curTime = System.currentTimeMillis();
		ts.perfTime = curTime - te.currentTime;
		te.currentTime = curTime;
		te.steps.add(ts);
	}

	public static void end() {
		ProcTrace pt = ProcTrace.instance();
		
		if (pt.stack.isEmpty()) {
			pt.buf.append("\nend of nothing\n");
			return;
		}
		
		final int layer = pt.stack.size();
		final String prefix = StringUtils.repeat(INDENT, layer);
		
		final TraceEntry te = pt.stack.pop();
		
		final StringBuilder sb = new StringBuilder();
		long now = System.currentTimeMillis();
		sb.append("\n").append(prefix).append(now - te.startTime).append(" ms:\t").append(te.steps.poll().stepInfo);
		te.steps.stream().forEach((ts) -> sb.append('\n').append(prefix).append(INDENT).append(ts.toString()));
		sb.append('\n');
//		pt.buf.append(sb);
		if (pt.stack.isEmpty()) {
			pt.buf.append(sb);
		} else {
			TraceStep ts = new TraceSubStep();
			ts.stepInfo = sb.toString();
			ongoing(ts);
		}
	}
	
	public static void end(TraceEntry te, String logMsg) {
		ProcTrace pt = ProcTrace.instance();
		if (te == null || StringUtils.isBlank(logMsg)) {
			return;
		}
		
//		final int layer = pt.stack.indexOf(te);
//		pt.finishedStack.push(new StringBuilder("\n" + StringUtils.repeat(INDENT, layer + 1) + "Exception: " + logMsg));
		for (TraceEntry _te = pt.stack.peek(); _te != te; _te = pt.stack.peek()) {
			end();
		}
		ProcTrace.ongoing("Exception: " + logMsg);
	}
	
	public static void end(final TraceEntry te, final Throwable e) {
		if (te == null || e == null) {
			return;
		}
		end(te, e.getMessage());
	}

	public static String flush() {
		ProcTrace pt = ProcTrace.instance();
		String str = pt.buf.toString().trim();
		pt.stack.clear();
		pt.buf = new StringBuilder("\n");
		return str;
	}
}
