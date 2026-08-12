package com.careconnect.service;

import com.careconnect.model.DeliveryStatus;
import com.careconnect.model.Donation;
import com.careconnect.model.InstitutionDashboard;
import com.careconnect.model.InstitutionDocument;
import com.careconnect.model.ItemDonation;
import com.careconnect.model.Need;
import com.careconnect.model.NeedStatus;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for the institution dashboard (Member 2, Module: Institution
 * dashboard). Aggregates data that already lives across InstitutionService,
 * NeedService, DonationService, ItemDonationService and
 * InstitutionDocumentService into a single InstitutionDashboard summary -
 * it doesn't own or duplicate any of their data, it only reads it.
 */
public class DashboardService {

    private static final String DONATION_TYPE_MONEY = "MONEY";
    private static final String PAYMENT_STATUS_PENDING = "PENDING";
    private static final String DONATION_STATUS_COMPLETED = "COMPLETED";

    private final InstitutionService institutionService;
    private final NeedService needService;
    private final DonationService donationService;
    private final ItemDonationService itemDonationService;
    private final InstitutionDocumentService documentService;

    public DashboardService() {
        this.institutionService = new InstitutionService();
        this.needService = new NeedService();
        this.donationService = new DonationService();
        this.itemDonationService = new ItemDonationService();
        this.documentService = new InstitutionDocumentService();
    }

    /**
     * Builds the full dashboard summary for one institution.
     *
     * @throws IllegalArgumentException if no such institution exists.
     */
    public InstitutionDashboard getDashboard(int institutionId) throws SQLException {
        InstitutionDashboard dashboard = new InstitutionDashboard();

        dashboard.setInstitution(institutionService.getInstitution(institutionId));

        List<Need> needs = needService.getNeedsForInstitution(institutionId);
        dashboard.setTotalNeeds(needs.size());
        dashboard.setOpenNeeds((int) needs.stream().filter(n -> n.getStatus() == NeedStatus.OPEN).count());
        dashboard.setPartiallyFulfilledNeeds((int) needs.stream()
                .filter(n -> n.getStatus() == NeedStatus.PARTIALLY_FULFILLED).count());
        dashboard.setFulfilledNeeds((int) needs.stream()
                .filter(n -> n.getStatus() == NeedStatus.FULFILLED).count());

        List<Donation> donations = donationService.getDonationHistoryForInstitution(institutionId);

        BigDecimal totalRaised = donations.stream()
                .filter(d -> DONATION_TYPE_MONEY.equals(d.getDonationType())
                        && DONATION_STATUS_COMPLETED.equals(d.getDonationStatus()))
                .map(Donation::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalMonetaryRaised(totalRaised);

        dashboard.setPendingMonetaryPledges((int) donations.stream()
                .filter(d -> DONATION_TYPE_MONEY.equals(d.getDonationType())
                        && PAYMENT_STATUS_PENDING.equals(d.getPaymentStatus()))
                .count());

        dashboard.setCompletedMonetaryDonations((int) donations.stream()
                .filter(d -> DONATION_TYPE_MONEY.equals(d.getDonationType())
                        && DONATION_STATUS_COMPLETED.equals(d.getDonationStatus()))
                .count());

        List<ItemDonation> itemDonations = itemDonationService.getItemDonationsForInstitution(institutionId);
        dashboard.setTotalItemDonations(itemDonations.size());
        dashboard.setPendingItemDonations((int) itemDonations.stream()
                .filter(id -> id.getDeliveryStatus() == DeliveryStatus.PENDING
                        || id.getDeliveryStatus() == DeliveryStatus.PICKED_UP)
                .count());
        dashboard.setDeliveredItemDonations((int) itemDonations.stream()
                .filter(id -> id.getDeliveryStatus() == DeliveryStatus.DELIVERED)
                .count());
        dashboard.setCancelledItemDonations((int) itemDonations.stream()
                .filter(id -> id.getDeliveryStatus() == DeliveryStatus.CANCELLED)
                .count());

        List<InstitutionDocument> documents = documentService.getDocumentsForInstitution(institutionId);
        dashboard.setVerificationDocumentsCount(documents.size());

        return dashboard;
    }
}
