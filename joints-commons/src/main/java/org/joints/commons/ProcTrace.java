package org.joints.commons;


import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.Stack;

public class ProcTrace {
    private static final String INDENT = "    ";
    private static final ThreadLocal<ProcTrace> instancePool = ThreadLocal.withInitial(ProcTrace::new);
    private final Stack<TraceEntry> stack = new Stack<>();
    private StringBuilder buf = new StringBuilder();

    private ProcTrace() {

    }

    public static ProcTrace instance() {
        return instancePool.get();
    }

    public static TraceEntry start() {
        return start(MiscUtils.invocationInfo(3));
    }

    public static TraceEntry start(String logMsg) {
        ProcTrace pt = ProcTrace.instance();

        final TraceEntry te = new TraceEntry();
        te.currentTime = te.startTime;

        TraceStep ts = new TraceStep();
        ts.performTime = 0;
        ts.stepInfo = logMsg;
        te.steps.push(ts);

        pt.stack.push(te);
        return te;
    }

    public static void ongoing(String logMsg) {
        final TraceStep newStep = new TraceStep();
        newStep.stepInfo = logMsg;
        ongoing(newStep);
    }

    public static void ongoing(final TraceStep ts) {
        final ProcTrace pt = ProcTrace.instance();

        if (pt.stack.isEmpty()) {
            start(ts.stepInfo);
            return;
        }

        final TraceEntry te = pt.stack.peek();
        te.ongoing(ts);
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

        long now = System.currentTimeMillis();
        String sb = te.end(prefix, now);

        if (pt.stack.isEmpty()) {
            pt.buf.append(sb);
        } else {
            TraceStep ts = new TraceSubStep();
            ts.stepInfo = sb;
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
//        for (TraceEntry _te = pt.stack.peek();
//             _te != te && !pt.stack.isEmpty();
//             _te = pt.stack.peek()) {
//            end();
//        }

        long now = System.currentTimeMillis();
        for (int j = pt.stack.size(), i = j - 1; i >= 0; i--) {
            if (pt.stack.peek() == te) break;

            final String prefix = StringUtils.repeat(INDENT, pt.stack.size());
            TraceEntry _te = pt.stack.pop();
            String ended = _te.end(prefix, now);

            TraceStep ts = new TraceSubStep();
            ts.stepInfo = ended;
            TraceEntry __te = pt.stack.peek();
            __te.ongoing(ts);
        }

        ProcTrace.ongoing("Exception: " + logMsg);
        ProcTrace.end();
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

    public static String endAndFlush() {
        ProcTrace pt = ProcTrace.instance();

        long now = System.currentTimeMillis();
        for (int j = pt.stack.size(), i = j - 1; i > 0; i--) {
            final String prefix = StringUtils.repeat(INDENT, pt.stack.size());
            TraceEntry _te = pt.stack.pop();
            String ended = _te.end(prefix, now);

            TraceStep ts = new TraceSubStep();
            ts.stepInfo = ended.trim();
            TraceEntry __te = pt.stack.peek();
            __te.ongoing(ts);
        }

        end();
        return flush();
    }

    public static class TraceStep {
        long performTime;
        String stepInfo;

        public String toString() {
            return performTime + " ms:\t" + stepInfo;
        }
    }

    public static class TraceSubStep extends TraceStep {
        public String toString() {
            return stepInfo;
        }
    }

    public static class TraceEntry {
        final long startTime = System.currentTimeMillis();
        final LinkedList<TraceStep> steps = new LinkedList<>();
        long currentTime;

        String end(String prefix, long now) {
            final StringBuilder sb = new StringBuilder();
//            sb.append("\n").append(prefix).append(String.format("%d ms [%s - %s]",
//                (now - startTime),
//                DateFormatUtils.format(startTime, "yyyy-MM-dd hh:mm:ss.sss"),
//                DateFormatUtils.format(now, "yyyy-MM-dd hh:mm:ss.sss")));
//            sb.append("\n").append(prefix).append(steps.poll().stepInfo);

            sb.append("\n").append(prefix).append(System.currentTimeMillis() - startTime).append(" ms:\t").append(steps.poll().stepInfo);
            steps.forEach((ts) -> sb.append('\n').append(prefix).append(INDENT).append(ts.toString()));
            long lastPerformTime = now - currentTime;
            if (lastPerformTime > 0)
                sb.append('\n').append(prefix).append(INDENT).append(String.format("%d end", lastPerformTime));
            sb.append('\n');

            return sb.toString();
        }

        TraceStep ongoing(final TraceStep ts) {
            TraceEntry te = this;
            final long curTime = System.currentTimeMillis();
            ts.performTime = curTime - te.currentTime;
            te.currentTime = curTime;
            te.steps.add(ts);
            return ts;
        }

    }
}
