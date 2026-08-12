package com.careconnect.model;

import java.sql.Date;

/**
 * Represents a row in the `item_donations` table (in-kind / non-monetary donations).
 *
 * Every ItemDonation belongs to exactly one Donation row (donation_id, ON DELETE
 * CASCADE) - the parent Donation carries donor_id, institution_id, need_id and the
 * overall donation_status; this table carries the item-specific details:
 * what was donated, how much, and how it's making its way to the institution.
 *
 * Kept separate from Donation on purpose (mirrors the schema): a "MONEY" donation
 * never has an item_donations row, an "ITEM" donation always does.
 */
public class ItemDonation {

    private int itemDonationId;
    private int donationId;
    private String itemName;
    private int quantity;
    private PickupOrDrop pickupOrDrop;
    private String pickupAddress;
    private DeliveryStatus deliveryStatus;
    private Date deliveryDate;

    public ItemDonation() {
    }

    /**
     * Used when creating a new item donation. itemDonationId is assigned by the DB.
     * deliveryStatus always starts at PENDING and deliveryDate is null until delivered.
     */
    public ItemDonation(int donationId, String itemName, int quantity,
                         PickupOrDrop pickupOrDrop, String pickupAddress) {
        this.donationId = donationId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.pickupOrDrop = pickupOrDrop;
        this.pickupAddress = pickupAddress;
        this.deliveryStatus = DeliveryStatus.PENDING;
    }

    public int getItemDonationId() {
        return itemDonationId;
    }

    public void setItemDonationId(int itemDonationId) {
        this.itemDonationId = itemDonationId;
    }

    public int getDonationId() {
        return donationId;
    }

    public void setDonationId(int donationId) {
        this.donationId = donationId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public PickupOrDrop getPickupOrDrop() {
        return pickupOrDrop;
    }

    public void setPickupOrDrop(PickupOrDrop pickupOrDrop) {
        this.pickupOrDrop = pickupOrDrop;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    @Override
    public String toString() {
        return "ItemDonation{" +
                "itemDonationId=" + itemDonationId +
                ", donationId=" + donationId +
                ", itemName='" + itemName + '\'' +
                ", quantity=" + quantity +
                ", pickupOrDrop=" + pickupOrDrop +
                ", pickupAddress='" + pickupAddress + '\'' +
                ", deliveryStatus=" + deliveryStatus +
                ", deliveryDate=" + deliveryDate +
                '}';
    }
}
