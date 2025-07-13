package net.calvuz.qdue.smartshifts.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class for JSON operations
 */
@Singleton
public class JsonHelper {

    private final Gson gson;

    @Inject
    public JsonHelper() {
        this.gson = new Gson();
    }

    /**
     * Convert object to JSON string
     */
    public String toJson(Object object) {
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse JSON string to object
     */
    public <T> T fromJson(String json, Class<T> classType) {
        try {
            return gson.fromJson(json, classType);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /**
     * Check if string is valid JSON
     */
    public boolean isValidJson(String json) {
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Pretty print JSON
     */
    public String prettyPrint(String json) {
        try {
            Object obj = gson.fromJson(json, Object.class);
            return gson.newBuilder().setPrettyPrinting().create().toJson(obj);
        } catch (JsonSyntaxException e) {
            return json; // Return original if parsing fails
        }
    }
}