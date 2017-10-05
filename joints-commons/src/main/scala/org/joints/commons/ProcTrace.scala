package org.joints.commons

import java.util

import org.apache.commons.lang3.StringUtils

object ProcTrace {
    val INDENT: String = "    "
    val instancePool: ThreadLocal[ProcTrace] = ThreadLocal.withInitial(() => new ProcTrace)

    def instance: ProcTrace = instancePool.get

    def start: TraceEntry = start(MiscUtils.invocationInfo(3))

    def start(logMsg: String): TraceEntry = {
        val pt: ProcTrace = instance
        val te: TraceEntry = new TraceEntry
        te.currentTime = te.startTime
        val ts: TraceStep = new TraceStep
        ts.performTime = 0
        ts.stepInfo = logMsg
        te.steps.push(ts)
        pt.stack.push(te)
        te
    }

    def ongoing(logMsg: String): Unit = {
        val newStep: TraceStep = new TraceStep
        newStep.stepInfo = logMsg
        ongoing(newStep)
    }

    def ongoing(ts: TraceStep): Unit = {
        val pt: ProcTrace = instance
        if (pt.stack.isEmpty) {
            start(ts.stepInfo)
            return
        }
        val te: TraceEntry = pt.stack.peek
        te.ongoing(ts)
    }

    def end(): Unit = {
        val pt: ProcTrace = instance
        if (pt.stack.isEmpty) {
            pt.buf.append("\nend of nothing\n")
            return
        }
        val layer: Int = pt.stack.size
        val prefix: String = StringUtils.repeat(INDENT, layer)
        val te: TraceEntry = pt.stack.pop
        val now: Long = System.currentTimeMillis
        val sb: String = te.end(prefix, now)
        if (pt.stack.isEmpty) pt.buf.append(sb)
        else {
            val ts: TraceStep = new TraceSubStep
            ts.stepInfo = sb
            ongoing(ts)
        }
    }

    def end(te: TraceEntry, logMsg: String): Unit = {
        val pt: ProcTrace = instance
        if (te == null || StringUtils.isBlank(logMsg)) return
        val now: Long = System.currentTimeMillis

        while (!pt.stack.isEmpty && pt.stack.peek != te) {
            val prefix: String = StringUtils.repeat(INDENT, pt.stack.size)
            val _te: TraceEntry = pt.stack.pop
            val ended: String = _te.end(prefix, now)

            val ts: TraceStep = new TraceSubStep
            ts.stepInfo = ended
            val __te: TraceEntry = pt.stack.peek
            __te.ongoing(ts)
        }

        ongoing("Exception: " + logMsg)
        end()
    }

    def end(te: TraceEntry, e: Throwable): Unit = {
        if (te == null || e == null) return
        end(te, e.getMessage)
    }

    def flush: String = {
        val pt: ProcTrace = instance
        val str: String = pt.buf.toString.trim
        pt.stack.clear()
        pt.buf = new StringBuilder("\n")
        str
    }

    def endAndFlush: String = {
        val pt: ProcTrace = instance
        val now: Long = System.currentTimeMillis
        while (!pt.stack.isEmpty) {
            val prefix: String = StringUtils.repeat(INDENT, pt.stack.size)
            val _te: TraceEntry = pt.stack.pop
            val ended: String = _te.end(prefix, now)
            val ts: TraceStep = new TraceSubStep
            ts.stepInfo = ended.trim
            val __te: TraceEntry = pt.stack.peek
            __te.ongoing(ts)
        }
        end()
        flush
    }
}


class TraceStep {
    private[commons] var performTime: Long = 0L
    private[commons] var stepInfo: String = null

    override def toString: String = s"${performTime} ms:\t${stepInfo}"
}

class TraceSubStep extends TraceStep {
    override def toString: String = stepInfo
}

class TraceEntry {

    import ProcTrace._

    final private[commons] val startTime: Long = System.currentTimeMillis
    final private[commons] val steps: util.LinkedList[TraceStep] = new util.LinkedList[TraceStep]
    private[commons] var currentTime: Long = 0L

    private[commons] def end(prefix: String, now: Long): String = {
        val sb: StringBuilder = new StringBuilder
        sb.append("\n").append(prefix).append(System.currentTimeMillis - startTime).append(" ms:\t").append(steps.poll.stepInfo)
        steps.forEach((ts: TraceStep) => sb.append('\n').append(prefix).append(INDENT).append(ts.toString))
        val lastPerformTime: Long = now - currentTime
        if (lastPerformTime > 0) sb.append('\n').append(prefix).append(INDENT).append(String.format("%d end", lastPerformTime))
        sb.append('\n')
        sb.toString
    }

    private[commons] def ongoing(ts: TraceStep): TraceStep = {
        val te: TraceEntry = this
        val curTime: Long = System.currentTimeMillis
        ts.performTime = curTime - te.currentTime
        te.currentTime = curTime
        te.steps.add(ts)
        ts
    }
}

class ProcTrace {
    private[commons] val stack: util.Stack[TraceEntry] = new util.Stack[TraceEntry]
    private[commons] var buf: StringBuilder = new StringBuilder
}
