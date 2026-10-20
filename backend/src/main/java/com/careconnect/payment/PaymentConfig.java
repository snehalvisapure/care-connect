package com.careconnect.payment;

/**
 * Central place for the payment settings. Values are read from environment variables first,
 * then from JVM system properties (-DNAME=value). Secrets never live in source code.
 *
 * Database settings are NOT here: the payment module reuses the project's own
 * DBConnection (src/main/resources/db.properties).
 */
public final class PaymentConfig {

    private PaymentConfig() {}

    static String get(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            v = System.getProperty(key);
        }
        return (v == null || v.isBlank()) ? defaultValue : v.trim();
    }

    static String required(String key) {
        String v = get(key, null);
        if (v == null) {
            throw new IllegalStateException("Missing configuration: " + key);
        }
        return v;
    }

    public static String keyId()         { return required("RAZORPAY_KEY_ID"); }
    public static String keySecret()     { return required("RAZORPAY_KEY_SECRET"); }
    public static String webhookSecret() { return required("RAZORPAY_WEBHOOK_SECRET"); }
    public static String currency()      { return "INR"; }
}
