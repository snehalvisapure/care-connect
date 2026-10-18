package com.careconnect.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * One row of the combined donation history: every field of a `donations` row,
 * plus - for ITEM-type donations only - the matching `item_donations` detail
 * (what was donated, how many, how it travels, and where it is in delivery).
 *
 * For MONEY donations all the item* fields are null, and Gson leaves null
 * fields out of the JSON, so the frontend can simply check whether
 * "itemName" is present.
 *
 * Flat on purpose (not a nested Donation + ItemDonation) so the frontend can
 * render one table row per entry without digging into sub-objects.
 */
public class DonationHistoryEntry {

    // --- from `donations` ---
    private int donationId;
    private int donorId;
    private int institutionId;
    private Integer needId;
    private String donationType;
    private BigDecimal amount;
    private String paymentStatus;
    private String transactionId;
    private String receiptNumber;
    private String donationStatus;
    private Timestamp donationDate;

    // --- from `item_donations` (null for MONEY donations) ---
    private Integer itemDonationId;
    private String itemName;
    private Integer quantity;
    private PickupOrDrop pickupOrDrop;
    private String pickupAddress;
    private DeliveryStatus deliveryStatus;
    private Date deliveryDate;

    public DonationHistoryEntry() {
    }

    /**
     * @param donation the parent donation row (required)
     * @param item     its item_donations detail row, or null for MONEY donations
     */
    public DonationHistoryEntry(Donation donation, ItemDonation item) {
        this.donationId = donation.getDonationId();
        this.donorId = donation.getDonorId();
        this.institutionId = donation.getInstitutionId();
        this.needId = donation.getNeedId();
        this.donationType = donation.getDonationType();
        this.amount = donation.getAmount();
        this.paymentStatus = donation.getPaymentStatus();
        this.transactionId = donation.getTransactionId();
        this.receiptNumber = donation.getReceiptNumber();
        this.donationStatus = donation.getDonationStatus();
        this.donationDate = donation.getDonationDate();

        if (item != null) {
            this.itemDonationId = item.getItemDonationId();
            this.itemName = item.getItemName();
            this.quantity = item.getQuantity();
            this.pickupOrDrop = item.getPickupOrDrop();
            this.pickupAddress = item.getPickupAddress();
            this.deliveryStatus = item.getDeliveryStatus();
            this.deliveryDate = item.getDeliveryDate();
        }
    }

    public int getDonationId() {
        return donationId;
    }

    public int getDonorId() {
        return donorId;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public Integer getNeedId() {
        return needId;
    }

    public String getDonationType() {
        return donationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public String getDonationStatus() {
        return donationStatus;
    }

    public Timestamp getDonationDate() {
        return donationDate;
    }

    public Integer getItemDonationId() {
        return itemDonationId;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public PickupOrDrop getPickupOrDrop() {
        return pickupOrDrop;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    @Override
    public String toString() {
        return "DonationHistoryEntry{" +
                "donationId=" + donationId +
                ", donorId=" + donorId +
                ", institutionId=" + institutionId +
                ", needId=" + needId +
                ", donationType='" + donationType + '\'' +
                ", amount=" + amount +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", donationStatus='" + donationStatus + '\'' +
                ", donationDate=" + donationDate +
                ", itemDonationId=" + itemDonationId +
                ", itemName='" + itemName + '\'' +
                ", quantity=" + quantity +
                ", pickupOrDrop=" + pickupOrDrop +
                ", deliveryStatus=" + deliveryStatus +
                ", deliveryDate=" + deliveryDate +
                '}';
    }
}
