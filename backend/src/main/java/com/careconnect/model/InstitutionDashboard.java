package com.careconnect.model;

import java.math.BigDecimal;

/**
 * NOT a database entity - this is a read-only aggregation object returned by
 * DashboardService/InstitutionController for the institution dashboard
 * (Member 2, Module: Institution dashboard). It's assembled in Java from
 * existing Institution / Need / Donation / ItemDonation / InstitutionDocument
 * data - there's no `dashboard` table.
 */
public class InstitutionDashboard {

    private Institution institution;

    // Needs summary
    private int totalNeeds;
    private int openNeeds;
    private int partiallyFulfilledNeeds;
    private int fulfilledNeeds;

    // Monetary donations summary
    private BigDecimal totalMonetaryRaised;
    private int pendingMonetaryPledges;
    private int completedMonetaryDonations;

    // In-kind (item) donations summary
    private int totalItemDonations;
    private int pendingItemDonations;
    private int deliveredItemDonations;
    private int cancelledItemDonations;

    // Verification documents summary
    private int verificationDocumentsCount;

    public InstitutionDashboard() {
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public int getTotalNeeds() {
        return totalNeeds;
    }

    public void setTotalNeeds(int totalNeeds) {
        this.totalNeeds = totalNeeds;
    }

    public int getOpenNeeds() {
        return openNeeds;
    }

    public void setOpenNeeds(int openNeeds) {
        this.openNeeds = openNeeds;
    }

    public int getPartiallyFulfilledNeeds() {
        return partiallyFulfilledNeeds;
    }

    public void setPartiallyFulfilledNeeds(int partiallyFulfilledNeeds) {
        this.partiallyFulfilledNeeds = partiallyFulfilledNeeds;
    }

    public int getFulfilledNeeds() {
        return fulfilledNeeds;
    }

    public void setFulfilledNeeds(int fulfilledNeeds) {
        this.fulfilledNeeds = fulfilledNeeds;
    }

    public BigDecimal getTotalMonetaryRaised() {
        return totalMonetaryRaised;
    }

    public void setTotalMonetaryRaised(BigDecimal totalMonetaryRaised) {
        this.totalMonetaryRaised = totalMonetaryRaised;
    }

    public int getPendingMonetaryPledges() {
        return pendingMonetaryPledges;
    }

    public void setPendingMonetaryPledges(int pendingMonetaryPledges) {
        this.pendingMonetaryPledges = pendingMonetaryPledges;
    }

    public int getCompletedMonetaryDonations() {
        return completedMonetaryDonations;
    }

    public void setCompletedMonetaryDonations(int completedMonetaryDonations) {
        this.completedMonetaryDonations = completedMonetaryDonations;
    }

    public int getTotalItemDonations() {
        return totalItemDonations;
    }

    public void setTotalItemDonations(int totalItemDonations) {
        this.totalItemDonations = totalItemDonations;
    }

    public int getPendingItemDonations() {
        return pendingItemDonations;
    }

    public void setPendingItemDonations(int pendingItemDonations) {
        this.pendingItemDonations = pendingItemDonations;
    }

    public int getDeliveredItemDonations() {
        return deliveredItemDonations;
    }

    public void setDeliveredItemDonations(int deliveredItemDonations) {
        this.deliveredItemDonations = deliveredItemDonations;
    }

    public int getCancelledItemDonations() {
        return cancelledItemDonations;
    }

    public void setCancelledItemDonations(int cancelledItemDonations) {
        this.cancelledItemDonations = cancelledItemDonations;
    }

    public int getVerificationDocumentsCount() {
        return verificationDocumentsCount;
    }

    public void setVerificationDocumentsCount(int verificationDocumentsCount) {
        this.verificationDocumentsCount = verificationDocumentsCount;
    }
}
