package com.careconnect.model;

/**
 * Mirrors the `pickup_or_drop` ENUM on the `item_donations` table.
 * PICKUP  - the institution/a volunteer collects the item from the donor's address.
 * DROP_OFF - the donor delivers the item to the institution directly.
 */
public enum PickupOrDrop {
    PICKUP,
    DROP_OFF
}
