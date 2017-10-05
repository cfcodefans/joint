package org.joints.commons

import java.io.InputStream

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonParser.Feature
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, SerializationFeature}


object Jsons {
    val MAPPER: ObjectMapper = new ObjectMapper

    MAPPER.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
    MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
    MAPPER.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
    MAPPER.setSerializationInclusion(Include.ALWAYS)
    MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true)
    MAPPER.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true)
    MAPPER.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false)


    def read(input: InputStream): JsonNode = {
        if (input == null || input.available() == 0)
            throw new NullPointerException("reading json however the input stream is empty")
        try
            MAPPER.readValue(input, classOf[JsonNode])
        catch {
            case e: Exception =>
                throw new RuntimeException("reading json stream", e)
        }
    }


    def read(raw: String): JsonNode = {
        if (raw == null) throw new Nothing("reading json however the json raw is empty")
        try
            MAPPER.readValue(raw, classOf[JsonNode])
        catch {
            case e: RuntimeException =>
                throw new Nothing("reading json raw", e)
        }
    }

    def toString(obj: Object): String = try
        MAPPER.writeValueAsString(obj)
    catch {
        case e: Exception =>
            throw new RuntimeException("deserialize json to string", e)
    }

    def toJson(map: java.util.Map[String, Object]): JsonNode = try {
        val jn: ObjectNode = MAPPER.createObjectNode
        map.forEach((k, v) => jn.putPOJO(k, v))
        jn
    } catch {
        case e: Exception =>
            throw new RuntimeException("deserialize json to string", e)
    }
}
