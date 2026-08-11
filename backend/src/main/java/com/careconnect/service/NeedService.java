package com.careconnect.service;

import com.careconnect.dao.InstitutionDAO;
import com.careconnect.dao.NeedDAO;
import com.careconnect.model.Need;
import com.careconnect.model.NeedStatus;
import com.careconnect.model.NeedUrgency;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for institution needs/requirements.
 * Sits between the Controller and the DAO - the Controller should call
 * this class, never NeedDAO directly.
 *
 * The core rule this class enforces: status must always stay consistent
 * with quantity_received vs quantity_required. quantity_received is never
 * set directly by a caller - it only moves via recordQuantityReceived(),
 * which recalculates status in the same step. This keeps it impossible for
 * a need to end up e.g. FULFILLED with quantity_received < quantity_required.
 */
public class NeedService {

    private final NeedDAO needDAO;
    private final InstitutionDAO institutionDAO;

    public NeedService() {
        this.needDAO = new NeedDAO();
        this.institutionDAO = new InstitutionDAO();
    }

    /**
     * Creates a new need for an institution. Always starts at
     * quantity_received = 0 and status = OPEN.
     *
     * @throws IllegalArgumentException if the institution doesn't exist,
     *         quantityRequired isn't positive, or category/itemName is blank.
     */
    public Need createNeed(int institutionId, String category, String itemName, int quantityRequired,
                            NeedUrgency urgency, String description) throws SQLException {

        if (institutionDAO.findById(institutionId) == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category cannot be blank");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("itemName cannot be blank");
        }
        if (quantityRequired <= 0) {
            throw new IllegalArgumentException("quantityRequired must be positive");
        }

        Need need = new Need();
        need.setInstitutionId(institutionId);
        need.setCategory(category);
        need.setItemName(itemName);
        need.setQuantityRequired(quantityRequired);
        need.setQuantityReceived(0);
        need.setUrgency(urgency != null ? urgency : NeedUrgency.MEDIUM);
        need.setDescription(description);
        need.setStatus(NeedStatus.OPEN);

        needDAO.insert(need);
        return need;
    }

    /**
     * Fetches a need by need_id.
     *
     * @throws IllegalArgumentException if no such need exists.
     */
    public Need getNeed(int needId) throws SQLException {
        Need need = needDAO.findById(needId);
        if (need == null) {
            throw new IllegalArgumentException("No need found with need_id=" + needId);
        }
        return need;
    }

    /**
     * Returns every need posted by an institution.
     *
     * @throws IllegalArgumentException if the institution doesn't exist.
     */
    public List<Need> getNeedsForInstitution(int institutionId) throws SQLException {
        if (institutionDAO.findById(institutionId) == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
        return needDAO.findByInstitutionId(institutionId);
    }

    /**
     * Updates the editable fields of a need (category, item name, quantity
     * required, urgency, description). Does not touch quantity_received.
     *
     * If quantityRequired changes, status is recalculated against the
     * need's current quantity_received - e.g. raising the required amount
     * on an already-FULFILLED need can drop it back to PARTIALLY_FULFILLED.
     *
     * @throws IllegalArgumentException if no such need exists, quantityRequired
     *         isn't positive, or category/itemName is blank.
     */
    public void updateNeed(int needId, String category, String itemName, int quantityRequired,
                            NeedUrgency urgency, String description) throws SQLException {

        Need existing = needDAO.findById(needId);
        if (existing == null) {
            throw new IllegalArgumentException("No need found with need_id=" + needId);
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category cannot be blank");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("itemName cannot be blank");
        }
        if (quantityRequired <= 0) {
            throw new IllegalArgumentException("quantityRequired must be positive");
        }

        existing.setCategory(category);
        existing.setItemName(itemName);
        existing.setQuantityRequired(quantityRequired);
        existing.setUrgency(urgency != null ? urgency : existing.getUrgency());
        existing.setDescription(description);

        needDAO.update(existing);

        NeedStatus recalculated = calculateStatus(existing.getQuantityReceived(), quantityRequired);
        if (recalculated != existing.getStatus()) {
            needDAO.updateStatus(needId, recalculated);
        }
    }

    /**
     * Records that a donation/delivery contributed toward a need, then
     * recalculates status. This is the ONLY way quantity_received should
     * change - never set it directly through the DAO.
     *
     * quantity_received is capped at quantity_required (extra donations
     * beyond what's needed don't push the number past 100% fulfilled).
     *
     * @param quantityDonated how much is being added on top of whatever
     *                        quantity_received already is. Must be positive.
     * @return the need's new state after the update.
     * @throws IllegalArgumentException if no such need exists, or quantityDonated isn't positive.
     */
    public Need recordQuantityReceived(int needId, int quantityDonated) throws SQLException {
        if (quantityDonated <= 0) {
            throw new IllegalArgumentException("quantityDonated must be positive");
        }

        Need need = needDAO.findById(needId);
        if (need == null) {
            throw new IllegalArgumentException("No need found with need_id=" + needId);
        }

        int newQuantityReceived = Math.min(
                need.getQuantityReceived() + quantityDonated,
                need.getQuantityRequired());

        needDAO.updateQuantityReceived(needId, newQuantityReceived);

        NeedStatus newStatus = calculateStatus(newQuantityReceived, need.getQuantityRequired());
        needDAO.updateStatus(needId, newStatus);

        need.setQuantityReceived(newQuantityReceived);
        need.setStatus(newStatus);
        return need;
    }

    /**
     * Deletes a need.
     *
     * @throws IllegalArgumentException if no such need exists.
     */
    public void deleteNeed(int needId) throws SQLException {
        boolean deleted = needDAO.delete(needId);
        if (!deleted) {
            throw new IllegalArgumentException("No need found with need_id=" + needId);
        }
    }

    /**
     * The single source of truth for what status a need should have, given
     * how much has been received vs how much is required:
     * - 0 received                        -> OPEN
     * - some received, but < required     -> PARTIALLY_FULFILLED
     * - received >= required              -> FULFILLED
     */
    private NeedStatus calculateStatus(int quantityReceived, int quantityRequired) {
        if (quantityReceived <= 0) {
            return NeedStatus.OPEN;
        } else if (quantityReceived < quantityRequired) {
            return NeedStatus.PARTIALLY_FULFILLED;
        } else {
            return NeedStatus.FULFILLED;
        }
    }
}
