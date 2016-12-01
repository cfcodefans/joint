package org.joints.rest;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fan on 2016/11/28.
 */
public class RestUtils {
    public static Map<String, String> extractParams(MultivaluedMap<String, String> params) {
        Map<String, String> paramsMap = new HashMap<String, String>();
        params.keySet().forEach(key -> paramsMap.put(key, params.getFirst(key)));
        return paramsMap;
    }

    public static Map<String, String[]> toParamMap(MultivaluedMap<String, String> params) {
        Map<String, String[]> paramsMap = new HashMap<String, String[]>();
        params.keySet().forEach(key -> paramsMap.put(key, params.get(key).toArray(new String[0])));
        return paramsMap;
    }
}
