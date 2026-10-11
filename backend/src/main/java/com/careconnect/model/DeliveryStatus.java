package com.careconnect.model;

/**
 * Mirrors the `delivery_status` ENUM on the `item_donations` table.
 * Flow: PENDING -> ACCEPTED/REJECTED -> PICKED_UP -> DELIVERED (or CANCELLED).
 */
public enum DeliveryStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    PICKED_UP,
    DELIVERED,
    CANCELLED
}