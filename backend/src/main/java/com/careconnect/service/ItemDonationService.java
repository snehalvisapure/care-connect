package com.careconnect.service;

import com.careconnect.dao.DonationDAO;
import com.careconnect.dao.InstitutionDAO;
import com.careconnect.dao.ItemDonationDAO;
import com.careconnect.dao.NeedDAO;
import com.careconnect.model.DeliveryStatus;
import com.careconnect.model.Donation;
import com.careconnect.model.ItemDonation;
import com.careconnect.model.Need;
import com.careconnect.model.PickupOrDrop;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for in-kind (non-monetary) donations.
 * Sits between the Controller and the DAOs - the Controller should call
 * this class, never ItemDonationDAO directly.
 *
 * An item donation is always backed by two rows: a `donations` row
 * (donation_type = ITEM, amount = 0.00) created by this class via
 * DonationDAO, and the `item_donations` row with the item-specific detail.
 * This mirrors DonationService's monetary-donation flow but for physical
 * items instead of money.
 *
 * THIS IS THE Needs <-> Donations INTEGRATION POINT: quantity_received on
 * a `needs` row only ever advances when an item donation tied to that need
 * is marked DELIVERED (see markDelivered()). NeedService.recordQuantityReceived()
 * is what actually recalculates the need's status (OPEN / PARTIALLY_FULFILLED /
 * FULFILLED) - this class never touches needs.quantity_received directly.
 *
 * Monetary donations (DonationService) are intentionally NOT wired into
 * quantity_received: a need's quantity is a count of physical items
 * (e.g. "20 bags of rice"), and money pledged toward a need doesn't
 * translate 1:1 into item units, so mixing the two would corrupt the
 * fulfillment count. A monetary donation can still be linked to a need_id
 * for tracking/reporting (see DashboardService), it just doesn't move
 * quantity_received.
 */
public class ItemDonationService {

    private static final String DONATION_TYPE_ITEM = "ITEM";
    private static final String DONATION_STATUS_COMPLETED = "COMPLETED";
    private static final String DONATION_STATUS_CANCELLED = "CANCELLED";

    private final ItemDonationDAO itemDonationDAO;
    private final DonationDAO donationDAO;
    private final InstitutionDAO institutionDAO;
    private final NeedDAO needDAO;
    private final NeedService needService;

    public ItemDonationService() {
        this.itemDonationDAO = new ItemDonationDAO();
        this.donationDAO = new DonationDAO();
        this.institutionDAO = new InstitutionDAO();
        this.needDAO = new NeedDAO();
        this.needService = new NeedService();
    }

    /**
     * Creates a new in-kind donation: a parent `donations` row (type ITEM,
     * amount 0.00) plus its `item_donations` detail row. Starts at
     * delivery_status = PENDING - nothing is counted toward a need's
     * quantity_received until it's actually marked DELIVERED.
     *
     * @param needId optional - pass null for an item donation not tied to
     *               any specific posted need.
     * @throws IllegalArgumentException if the institution doesn't exist,
     *         the need doesn't exist or belongs to a different institution,
     *         quantity isn't positive, itemName is blank, pickupOrDrop is
     *         null, or pickupOrDrop is PICKUP with a blank pickupAddress.
     */
    public ItemDonation createItemDonation(int donorId, int institutionId, Integer needId,
                                            String itemName, int quantity,
                                            PickupOrDrop pickupOrDrop, String pickupAddress)
            throws SQLException {

        if (institutionDAO.findById(institutionId) == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("itemName cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (pickupOrDrop == null) {
            throw new IllegalArgumentException("pickupOrDrop cannot be null");
        }
        if (pickupOrDrop == PickupOrDrop.PICKUP && (pickupAddress == null || pickupAddress.isBlank())) {
            throw new IllegalArgumentException("pickupAddress is required when pickupOrDrop = PICKUP");
        }
        if (needId != null) {
            Need need = needDAO.findById(needId);
            if (need == null) {
                throw new IllegalArgumentException("No need found with need_id=" + needId);
            }
            if (need.getInstitutionId() != institutionId) {
                throw new IllegalArgumentException(
                        "need_id=" + needId + " does not belong to institution_id=" + institutionId);
            }
        }

        Donation donation = new Donation(donorId, institutionId, needId, DONATION_TYPE_ITEM, BigDecimal.ZERO);
        int donationId = donationDAO.createDonation(donation);

        ItemDonation itemDonation = new ItemDonation(donationId, itemName, quantity, pickupOrDrop, pickupAddress);
        itemDonationDAO.insert(itemDonation);

        return itemDonation;
    }

    /**
     * Fetches a single item donation by item_donation_id.
     *
     * @throws IllegalArgumentException if no such item donation exists.
     */
    public ItemDonation getItemDonation(int itemDonationId) throws SQLException {
        ItemDonation itemDonation = itemDonationDAO.findById(itemDonationId);
        if (itemDonation == null) {
            throw new IllegalArgumentException("No item donation found with item_donation_id=" + itemDonationId);
        }
        return itemDonation;
    }

    /**
     * All in-kind donations made by a donor, most recent first.
     */
    public List<ItemDonation> getItemDonationsForDonor(int donorId) throws SQLException {
        return itemDonationDAO.findByDonorId(donorId);
    }

    /**
     * All in-kind donations received by an institution, most recent first.
     *
     * @throws IllegalArgumentException if the institution doesn't exist.
     */
    public List<ItemDonation> getItemDonationsForInstitution(int institutionId) throws SQLException {
        if (institutionDAO.findById(institutionId) == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
        return itemDonationDAO.findByInstitutionId(institutionId);
    }

    /**
     * Marks a PICKUP-type item donation as collected from the donor.
     * (DROP_OFF donations skip this step and go straight PENDING -> DELIVERED.)
     *
     * @throws IllegalArgumentException if no such item donation exists.
     * @throws IllegalStateException if it isn't currently PENDING.
     */
    public ItemDonation markPickedUp(int itemDonationId) throws SQLException {
        ItemDonation itemDonation = getItemDonation(itemDonationId);

        if (itemDonation.getDeliveryStatus() != DeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a PENDING item donation can be marked PICKED_UP. Current status: "
                            + itemDonation.getDeliveryStatus());
        }

        itemDonationDAO.updateDeliveryStatus(itemDonationId, DeliveryStatus.PICKED_UP);
        itemDonation.setDeliveryStatus(DeliveryStatus.PICKED_UP);
        return itemDonation;
    }

    /**
     * Marks an item donation as delivered to the institution. This is the
     * point where the donation actually counts:
     * 1. delivery_status -> DELIVERED, delivery_date recorded.
     * 2. The parent donation's donation_status -> COMPLETED.
     * 3. If this item donation is tied to a need (need_id), that need's
     *    quantity_received is advanced by this donation's quantity via
     *    NeedService.recordQuantityReceived() - which also recalculates
     *    the need's status (OPEN / PARTIALLY_FULFILLED / FULFILLED).
     *
     * Allowed from PENDING (typical for DROP_OFF - donor hands it over
     * directly) or from PICKED_UP (typical for PICKUP - collected earlier,
     * now confirmed received).
     *
     * @param deliveryDate the date it was received; pass null to default to today.
     * @throws IllegalArgumentException if no such item donation exists.
     * @throws IllegalStateException if it's already DELIVERED or was CANCELLED.
     */
    public ItemDonation markDelivered(int itemDonationId, Date deliveryDate) throws SQLException {
        ItemDonation itemDonation = getItemDonation(itemDonationId);

        DeliveryStatus current = itemDonation.getDeliveryStatus();
        if (current == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("This item donation has already been marked DELIVERED.");
        }
        if (current == DeliveryStatus.CANCELLED) {
            throw new IllegalStateException("A CANCELLED item donation cannot be marked DELIVERED.");
        }

        Date effectiveDate = deliveryDate != null ? deliveryDate : new Date(System.currentTimeMillis());

        itemDonationDAO.updateDeliveryStatus(itemDonationId, DeliveryStatus.DELIVERED);
        itemDonationDAO.updateDeliveryDate(itemDonationId, effectiveDate);

        itemDonation.setDeliveryStatus(DeliveryStatus.DELIVERED);
        itemDonation.setDeliveryDate(effectiveDate);

        Donation donation = donationDAO.getDonationById(itemDonation.getDonationId());
        if (donation != null) {
            donationDAO.updateDonationStatus(donation.getDonationId(), DONATION_STATUS_COMPLETED);

            // --- Needs <-> Donations integration ---
            if (donation.getNeedId() != null) {
                needService.recordQuantityReceived(donation.getNeedId(), itemDonation.getQuantity());
            }
        }

        return itemDonation;
    }

    /**
     * Cancels an item donation. Only allowed before it's been delivered -
     * once DELIVERED it has already been counted toward a need's
     * quantity_received and can't be un-counted through this method.
     *
     * @throws IllegalArgumentException if no such item donation exists.
     * @throws IllegalStateException if it's already DELIVERED or CANCELLED.
     */
    public ItemDonation cancelItemDonation(int itemDonationId) throws SQLException {
        ItemDonation itemDonation = getItemDonation(itemDonationId);

        if (itemDonation.getDeliveryStatus() == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("A DELIVERED item donation cannot be cancelled.");
        }
        if (itemDonation.getDeliveryStatus() == DeliveryStatus.CANCELLED) {
            throw new IllegalStateException("This item donation is already CANCELLED.");
        }

        itemDonationDAO.updateDeliveryStatus(itemDonationId, DeliveryStatus.CANCELLED);
        itemDonation.setDeliveryStatus(DeliveryStatus.CANCELLED);

        Donation donation = donationDAO.getDonationById(itemDonation.getDonationId());
        if (donation != null) {
            donationDAO.updateDonationStatus(donation.getDonationId(), DONATION_STATUS_CANCELLED);
        }

        return itemDonation;
    }
}
