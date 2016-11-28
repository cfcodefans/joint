package org.joints.commons;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Used to replace the holders with values in the expression
 *
 * @author fan@thenetcircle.com
 */
public final class PlaceHolder {
    private final String delimiterHead, delimiterTail;

    @Override
    public int hashCode() {
        return PlaceHolder.hashCode(delimiterHead, delimiterTail);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PlaceHolder other = (PlaceHolder) obj;
        if (delimiterHead == null) {
            if (other.delimiterHead != null)
                return false;
        } else if (!delimiterHead.equals(other.delimiterHead))
            return false;
        if (delimiterTail == null) {
            if (other.delimiterTail != null)
                return false;
        } else if (!delimiterTail.equals(other.delimiterTail))
            return false;
        return true;
    }

    private final static int hashCode(String head, String tail) {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((head == null) ? 0 : head.hashCode());
        result = prime * result + ((tail == null) ? 0 : tail.hashCode());
        return result;
    }

    private static final Map<Integer, PlaceHolder> PLACEHOLDERS = new HashMap<Integer, PlaceHolder>();

    private PlaceHolder(final String aDelimiterHead, final String aDelimiterTail) {
        if (aDelimiterHead == null || aDelimiterTail == null) {
            throw new NullPointerException("Placeholder Initialization Error: Head or Tail Delimiter is null");
        }

        this.delimiterTail = aDelimiterTail;
        this.delimiterHead = aDelimiterHead;
    }

    public static PlaceHolder get(final String aDelimiterHead, final String aDelimiterTail) {
        final Integer id = hashCode(aDelimiterHead, aDelimiterTail);
        PlaceHolder result = PLACEHOLDERS.get(id);
        if (result == null) {
            result = new PlaceHolder(aDelimiterHead, aDelimiterTail);
            PLACEHOLDERS.put(id, result);
        }
        return result;
    }

    protected String replace(String str, BiFunction<Integer, String, String> func) {
        if (StringUtils.isBlank(str))
            return str;

        StringBuilder sb = new StringBuilder(str);
        int headDelimiterLen = delimiterHead.length();
        int tailDelimiterLen = delimiterTail.length();

        for (int headerIdx = sb.indexOf(delimiterHead), tailIdx = sb.indexOf(delimiterTail, headerIdx), i = 0;
             tailIdx > 0;
             headerIdx = sb.indexOf(delimiterHead), tailIdx = sb.indexOf(delimiterTail, headerIdx), i++) {
            int start = headerIdx + headDelimiterLen;
            String key = sb.substring(start, tailIdx);
            String val = func.apply(i, key);
            if (val != null) {
                sb.replace(headerIdx, tailIdx + tailDelimiterLen, val);
            }
        }

        return sb.toString();
    }

    public String replace(String str, Map<String, String> values) {
        return replace(str, (i, key) -> values.get(key));
    }

    public String replace(String str, String... values) {
        return replace(str, (i, key) -> values[i]);
    }
}
