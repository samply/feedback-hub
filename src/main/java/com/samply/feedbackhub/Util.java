package com.samply.feedbackhub;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.gson.Gson;


public class Util {

    /**
     * Converts an object into a JSON string representation using Gson library.
     *
     * @param object The object to convert into a JSON string.
     * @return The JSON string representation of the object.
     */
    public static String jsonStringFromObject(Object object) {
        if (object == null)
            return null;
        Gson gson = new Gson();
        return gson.toJson(object);
    }

    /**
     * Get a printable stack trace from an Exception object.
    * @param e
    * @return
    */
    public static String traceFromException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
            return sw.toString();
    }

}
