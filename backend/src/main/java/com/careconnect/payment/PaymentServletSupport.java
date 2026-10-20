package com.careconnect.payment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/** Small helpers shared by the payment servlets. */
final class PaymentServletSupport {

    /** Session attribute set by AuthController on login. */
    static final String SESSION_USER_ID = "userId";

    private PaymentServletSupport() {}

    /** Returns the logged-in user's id, or null if nobody is logged in. */
    static Integer currentUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object v = session.getAttribute(SESSION_USER_ID);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }

    /** Reads the JSON request body. An empty body gives an empty object. Throws JsonParseException if malformed. */
    static JsonObject readJson(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String text = sb.toString().trim();
        if (text.isEmpty()) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (IllegalStateException notAnObject) {
            throw new JsonParseException("Request body must be a JSON object");
        }
    }

    /** Reads a string field from a JSON object, or null if missing. */
    static String str(JsonObject o, String key) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }

    static void writeJson(HttpServletResponse resp, int status, JsonObject body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        PrintWriter out = resp.getWriter();
        out.print(body.toString());
        out.flush();
    }

    /** Error shape matches the rest of the API: { "error": "message" } (api.js reads body.error). */
    static void error(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("error", message);
        writeJson(resp, status, o);
    }
}
