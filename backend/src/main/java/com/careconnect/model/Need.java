package com.careconnect.model;

import java.sql.Timestamp;

/**
 * PLACEHOLDER model, created by Member 3 (Nehaa) on 2026-08-09 to support
 * the Search & Filter module (Member 3's scope), because Member 2 had not
 * yet built the Need model/DAO for the Needs/Requirements module.
 *
 * This is intentionally READ-ONLY in spirit - Search & Filter only needs to
 * query and display needs, not create/edit them (that's Member 2's
 * responsibility). If/when Member 2 delivers their own Need model,
 * reconcile the two.
 *
 * Mirrors the `needs` table. Note: category is a free-text varchar(50) in
 * the schema, not an ENUM, so it's kept as a plain String here.
 */
public class Need {

    private int needId;
    private int institutionId;
    private String category;
    private String itemName;
    private int quantityRequired;
    private int quantityReceived; // defaults to 0 in DB
    private NeedUrgency urgency;  // defaults to MEDIUM in DB
    private String description;
    private NeedStatus status;    // defaults to OPEN in DB
    private Timestamp postedDate;

    public Need() {
    }

    public int getNeedId() {
        return needId;
    }

    public void setNeedId(int needId) {
        this.needId = needId;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(int institutionId) {
        this.institutionId = institutionId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(int quantityRequired) {
        this.quantityRequired = quantityRequired;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(int quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public NeedUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(NeedUrgency urgency) {
        this.urgency = urgency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NeedStatus getStatus() {
        return status;
    }

    public void setStatus(NeedStatus status) {
        this.status = status;
    }

    public Timestamp getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(Timestamp postedDate) {
        this.postedDate = postedDate;
    }

    @Override
    public String toString() {
        return "Need{" +
                "needId=" + needId +
                ", institutionId=" + institutionId +
                ", category='" + category + '\'' +
                ", itemName='" + itemName + '\'' +
                ", urgency=" + urgency +
                ", status=" + status +
                '}';
    }
}
