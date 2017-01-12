package org.joints.commons;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Jsons {
    private Jsons() {
    }

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        MAPPER.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        MAPPER.setSerializationInclusion(Include.NON_NULL);
        MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
        MAPPER.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        MAPPER.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }

    /*This method is unused except the tests*/
    public static JsonNode read(final InputStream input) {
        if (input == null) {
            throw new NullPointerException("reading json however the input stream is empty");
        }
        try {
            return MAPPER.readValue(input, JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("reading json stream", e);
        }
    }

    public static JsonNode read(final String raw) {
        if (raw == null) {
            throw new NullPointerException("reading json however the json raw is empty");
        }
        try {
            return MAPPER.readValue(raw, JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("reading json raw", e);
        }
    }

    public static <T> T read(final String raw, final Class<T> cls) {
        if (raw == null) {
            throw new NullPointerException("reading json however the json raw is empty");
        }
        try {
            return MAPPER.readValue(raw, cls);
        } catch (Exception e) {
            throw new RuntimeException("reading json raw", e);
        }
    }

    public static String toString(final Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("deserialing json to string", e);
        }
    }

    private static ConcurrentMap<Class, String> classAndTypeScriptDefs = new ConcurrentHashMap<>();

    public static String classToTypeScriptDef(Class cls) {
        if (cls == null) return StringUtils.EMPTY;

        if (cls == int.class
            || cls == short.class
            || cls == byte.class
            || cls == long.class
            || cls == float.class
            || cls == double.class
            || Number.class.isAssignableFrom(cls))
            return TypeScriptBasics.number.literal;

        if (cls == boolean.class)
            return TypeScriptBasics._boolean.literal;

        if (cls == char.class
            || CharSequence.class.isAssignableFrom(cls)) {
            return TypeScriptBasics.string.literal;
        }

        if (cls.isEnum()) {
            Map enumMap = EnumUtils.getEnumMap(cls);
            return String.format("\nenum %s {%s};\n", cls.getSimpleName(), StringUtils.join(enumMap.keySet(), ", "));
        }

        if (cls.isArray()) {
            return String.format("%s[]", cls.getComponentType().getSimpleName());
        }

//        if (Collection.class.isAssignableFrom(cls) || Iterable.class.isAssignableFrom(cls)) {
//
//        }

        SerializationConfig cfg = MAPPER.getSerializationConfig();
        JavaType jt = cfg.constructType(cls);


        return null;
    }

    enum TypeScriptBasics {
        _boolean("boolean"), number("number"), string("string"), any("any");
        public String literal;

        TypeScriptBasics(String _literal) {
            this.literal = _literal;
        }
    }
}

