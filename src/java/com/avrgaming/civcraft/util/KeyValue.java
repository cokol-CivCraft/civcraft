package com.avrgaming.civcraft.util;

import com.mysql.jdbc.StringUtils;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.util.Collection;
import java.util.HashMap;
 
/*
 * Utility class that implements serialization/deserialization for key-value pairs into a single string.
 *              String outEncoded = new String(Base64Coder.encode(out.getBytes()));
                return outEncoded;
        }
 
        public static ItemStack deserializeEnhancements(ItemStack stack, String serial) {
                String in = StringUtils.toAsciiString(Base64Coder.decode(serial));
 */

public class KeyValue {

    private final HashMap<String, Object> keyValues = new HashMap<>();

    public String serialize() {
        StringBuilder builder = new StringBuilder();

        for (String key : keyValues.keySet()) {
            Object value = keyValues.get(key);
            builder.append(key);
            builder.append(",");
            builder.append(value.getClass().getSimpleName());
            builder.append(",");
            String valueString = String.valueOf(value);
            builder.append(Base64Coder.encode(valueString.getBytes()));
            builder.append(";");
        }

        return builder.toString();
    }

    public void deserialize(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        String[] kvs = input.split(";");

        for (String kv : kvs) {
            String[] data = kv.split(",");


            String key = data[0];
            String className = data[1];
            String decodedValue;

            if (data.length < 3) {
                /* string key with no value? */
                decodedValue = "";
            } else {
                String encodedValue = data[2];
                decodedValue = StringUtils.toAsciiString(Base64Coder.decode(encodedValue));
            }

            try {
                Object valueInstance = switch (className) {
                    case "Integer" -> Integer.valueOf(decodedValue);
                    case "Boolean" -> Boolean.valueOf(decodedValue);
                    case "Double" -> Double.valueOf(decodedValue);
                    default -> decodedValue;
                };

                keyValues.put(key, valueInstance);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void setString(String key, String value) {
        keyValues.put(key, value);
    }

    public void setInt(String key, Integer value) {
        keyValues.put(key, value);
    }

    public void setDouble(String key, Double value) {
        keyValues.put(key, value);
    }

    public void setBoolean(String key, Boolean value) {
        keyValues.put(key, value);
    }

    public String getString(String key) {
        return (String) keyValues.get(key);
    }

    public Integer getInt(String key) {
        return (Integer) keyValues.get(key);
    }

    public Double getDouble(String key) {
        return (Double) keyValues.get(key);
    }

    public Boolean getBoolean(String key) {
        return (Boolean) keyValues.get(key);
    }

    public Collection<String> getKeySet() {
        return this.keyValues.keySet();
    }
}