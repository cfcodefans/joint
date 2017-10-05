package org.joints.commons

import java.util.Objects

import org.apache.commons.lang3.StringUtils

object PlaceHolder {

}

class PlaceHolder {
    private val delimiterHead: String = null
    private val delimiterTail: String = null

    override def hashCode(): Int = Objects.hash(delimiterHead, delimiterTail)

    def canEqual(other: Any): Boolean = other.isInstanceOf[PlaceHolder]

    override def equals(other: Any): Boolean = other match {
        case that: PlaceHolder =>
            (that canEqual this) &&
                delimiterHead == that.delimiterHead &&
                delimiterTail == that.delimiterTail
        case _ => false
    }

    protected def replace(str: String, func: (Int, String) => String): String = {
        if (StringUtils.isBlank(str)) return str
        val sb: StringBuilder = new StringBuilder(str)
        val headDelimiterLen: Int = delimiterHead.length
        val tailDelimiterLen: Int = delimiterTail.length
        var headerIdx: Int = sb.indexOf(delimiterHead)
        var tailIdx: Int = sb.indexOf(delimiterTail, headerIdx)
        var i: Int = 0
        while (tailIdx > 0) {
            val start: Int = headerIdx + headDelimiterLen
            val key: String = sb.substring(start, tailIdx)
            val _val: String = func(i, key)
            if (_val != null) sb.replace(headerIdx, tailIdx + tailDelimiterLen, _val)

            headerIdx = sb.indexOf(delimiterHead)
            tailIdx = sb.indexOf(delimiterTail, headerIdx)
            i += 1
        }
        sb.toString
    }
}
