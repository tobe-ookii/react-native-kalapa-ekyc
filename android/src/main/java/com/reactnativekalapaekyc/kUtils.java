package com.reactlibrary;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import org.json.JSONArray;

import java.util.List;
import java.util.Map;

class kUtils {
    public static WritableMap convertMapToWritableMap(Map<String, Object> input) {
        WritableMap output = new WritableNativeMap();

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                output.putMap(key, convertMapToWritableMap((Map<String, Object>) value));
            } else if (value instanceof JSONArray) {
                output.putArray(key, convertArrayToWritableArray((Object[]) value));
            } else if (value instanceof List) {
                output.putArray(key, convertArrayToWritableArray(((List) value).toArray()));
            } else if (value instanceof Boolean) {
                output.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                output.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                output.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                output.putString(key, (String) value);
            } else {
                output.putString(key, value.toString());
            }
        }
        return output;
    }

    public static WritableArray convertArrayToWritableArray(Object[] input) {
        WritableArray output = new WritableNativeArray();

        for (int i = 0; i < input.length; i++) {
            Object value = input[i];
            if (value instanceof Map) {
                output.pushMap(convertMapToWritableMap((Map<String, Object>) value));
            } else if (value instanceof JSONArray) {
                output.pushArray(convertArrayToWritableArray((Object[]) value));
            } else if (value instanceof List) {
                output.pushArray(convertArrayToWritableArray(((List) value).toArray()));
            } else if (value instanceof Boolean) {
                output.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                output.pushInt((Integer) value);
            } else if (value instanceof Double) {
                output.pushDouble((Double) value);
            } else if (value instanceof String) {
                output.pushString((String) value);
            } else {
                output.pushString(value.toString());
            }
        }
        return output;
    }
}
