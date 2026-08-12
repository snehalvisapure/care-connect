package com.careconnect.model;

/**
 * Mirrors the `delivery_status` ENUM on the `item_donations` table.
 */
public enum DeliveryStatus {
    PENDING,
    PICKED_UP,
    DELIVERED,
    CANCELLED
}
