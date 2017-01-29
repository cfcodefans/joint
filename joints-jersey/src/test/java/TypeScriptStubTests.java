import org.joints.commons.Jsons;
import org.joints.rest.ajax.TypeScriptStubs;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by fan on 2017/1/24.
 */
public class TypeScriptStubTests {
    public static class SimplePOJO {
        private int intVal = 1;
        private short shortVal = 2;
        private byte byteVal = 3;
        private long longVal = 4;
        private float floatVal = 5f;
        private double doubleVal = 6d;
        private char charVal = '7';
        private boolean boolVal = true;
        private String stringVal = "string";

        private int[] intArray = {9};
        private short[] shortArray = {10};
        private byte[] byteArray = {11};
        private long[] longArray = {12};
        private float[] floatArray = {13f};
        private double[] doubleArray = {14d};
        private char[] charArray = {'1', '5'};
        private boolean[] boolArray = {false, true};
        private String[] stringArray = {"string", "array"};

        private Date dateVal = new Date();
        private Date[] dateArray = {new Date()};

        public int getIntVal() {
            return intVal;
        }

        public void setIntVal(int intVal) {
            this.intVal = intVal;
        }

        public short getShortVal() {
            return shortVal;
        }

        public void setShortVal(short shortVal) {
            this.shortVal = shortVal;
        }

        public byte getByteVal() {
            return byteVal;
        }

        public void setByteVal(byte byteVal) {
            this.byteVal = byteVal;
        }

        public long getLongVal() {
            return longVal;
        }

        public void setLongVal(long longVal) {
            this.longVal = longVal;
        }

        public float getFloatVal() {
            return floatVal;
        }

        public void setFloatVal(float floatVal) {
            this.floatVal = floatVal;
        }

        public double getDoubleVal() {
            return doubleVal;
        }

        public void setDoubleVal(double doubleVal) {
            this.doubleVal = doubleVal;
        }

        public char getCharVal() {
            return charVal;
        }

        public void setCharVal(char charVal) {
            this.charVal = charVal;
        }

        public boolean isBoolVal() {
            return boolVal;
        }

        public void setBoolVal(boolean boolVal) {
            this.boolVal = boolVal;
        }

        public String getStringVal() {
            return stringVal;
        }

        public void setStringVal(String stringVal) {
            this.stringVal = stringVal;
        }

        public int[] getIntArray() {
            return intArray;
        }

        public void setIntArray(int[] intArray) {
            this.intArray = intArray;
        }

        public short[] getShortArray() {
            return shortArray;
        }

        public void setShortArray(short[] shortArray) {
            this.shortArray = shortArray;
        }

        public byte[] getByteArray() {
            return byteArray;
        }

        public void setByteArray(byte[] byteArray) {
            this.byteArray = byteArray;
        }

        public long[] getLongArray() {
            return longArray;
        }

        public void setLongArray(long[] longArray) {
            this.longArray = longArray;
        }

        public float[] getFloatArray() {
            return floatArray;
        }

        public void setFloatArray(float[] floatArray) {
            this.floatArray = floatArray;
        }

        public double[] getDoubleArray() {
            return doubleArray;
        }

        public void setDoubleArray(double[] doubleArray) {
            this.doubleArray = doubleArray;
        }

        public char[] getCharArray() {
            return charArray;
        }

        public void setCharArray(char[] charArray) {
            this.charArray = charArray;
        }

        public boolean[] getBoolArray() {
            return boolArray;
        }

        public void setBoolArray(boolean[] boolArray) {
            this.boolArray = boolArray;
        }

        public String[] getStringArray() {
            return stringArray;
        }

        public void setStringArray(String[] stringArray) {
            this.stringArray = stringArray;
        }

        public Date getDateVal() {
            return dateVal;
        }

        public void setDateVal(Date dateVal) {
            this.dateVal = dateVal;
        }

        public Date[] getDateArray() {
            return dateArray;
        }

        public void setDateArray(Date[] dateArray) {
            this.dateArray = dateArray;
        }
    }

    @Test
    public void testSimplePOJO() {
        SimplePOJO sp = new SimplePOJO();
//        System.out.println(Jsons.toString(sp));

        TypeScriptStubs.TypeScriptParseContext ctx = new TypeScriptStubs.TypeScriptParseContext();
        String src = TypeScriptStubs.classToTypeScriptDef(SimplePOJO.class, ctx);
        System.out.println(ctx.classAndTypeSources.get(SimplePOJO.class));
    }

    public static class EnumHolder {
        private TimeUnit tu;
        private Long val;

        public TimeUnit getTu() {
            return tu;
        }

        public void setTu(TimeUnit tu) {
            this.tu = tu;
        }

        public Long getVal() {
            return val;
        }

        public void setVal(Long val) {
            this.val = val;
        }
    }

    @Test
    public void testEnumHolder() {
        EnumHolder eh = new EnumHolder();
//        System.out.println(Jsons.toString(eh));

        TypeScriptStubs.TypeScriptParseContext ctx = new TypeScriptStubs.TypeScriptParseContext();
        String src = TypeScriptStubs.classToTypeScriptDef(EnumHolder.class, ctx);
//        System.out.println(classAndTypeSources.get(EnumHolder.class));

        ctx.classAndTypeSources.entrySet().stream()
                .forEach((Map.Entry<Class, String> en) -> System.out.printf("\n%s -> %s\n", en.getKey(), en.getValue()));
    }
}
