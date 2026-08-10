package com.careconnect.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Donation {

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

    public Donation() {
    }

    public Donation(int donorId, int institutionId, Integer needId,
                    String donationType, BigDecimal amount) {
        this.donorId = donorId;
        this.institutionId = institutionId;
        this.needId = needId;
        this.donationType = donationType;
        this.amount = amount;
    }

    public int getDonationId() {
        return donationId;
    }

    public void setDonationId(int donationId) {
        this.donationId = donationId;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(int institutionId) {
        this.institutionId = institutionId;
    }

    public Integer getNeedId() {
        return needId;
    }

    public void setNeedId(Integer needId) {
        this.needId = needId;
    }

    public String getDonationType() {
        return donationType;
    }

    public void setDonationType(String donationType) {
        this.donationType = donationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getDonationStatus() {
        return donationStatus;
    }

    public void setDonationStatus(String donationStatus) {
        this.donationStatus = donationStatus;
    }

    public Timestamp getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(Timestamp donationDate) {
        this.donationDate = donationDate;
    }

    @Override
    public String toString() {
        return "Donation{" +
                "donationId=" + donationId +
                ", donorId=" + donorId +
                ", institutionId=" + institutionId +
                ", needId=" + needId +
                ", donationType='" + donationType + '\'' +
                ", amount=" + amount +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", receiptNumber='" + receiptNumber + '\'' +
                ", donationStatus='" + donationStatus + '\'' +
                ", donationDate=" + donationDate +
                '}';
    }
}