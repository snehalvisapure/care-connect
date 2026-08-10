package com.careconnect.service;

import com.careconnect.dao.InstitutionDAO;
import com.careconnect.model.Institution;
import com.careconnect.model.InstitutionType;
import com.careconnect.model.VerificationStatus;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for institution registration and profiles.
 * Sits between the Controller and the DAO - the Controller should call
 * this class, never InstitutionDAO directly.
 *
 * Note: there's no UserDAO in the codebase yet, so this class can't check
 * that userId actually belongs to a user with role = INSTITUTION. Once a
 * UserDAO exists, add that check to registerInstitution().
 */
public class InstitutionService {

    private final InstitutionDAO institutionDAO;

    // MySQL error code for a UNIQUE constraint violation (duplicate entry)
    private static final int MYSQL_DUPLICATE_ENTRY_ERROR_CODE = 1062;

    public InstitutionService() {
        this.institutionDAO = new InstitutionDAO();
    }

    /**
     * Registers a new institution.
     * Business rules:
     * 1. One institution profile per user (this app's intent, even though the
     *    DB doesn't enforce it with a UNIQUE constraint on user_id).
     * 2. registration_number must be unique (DB-enforced; this turns the raw
     *    SQL error into a friendly message).
     * 3. New registrations always start at PENDING - only an admin can approve them.
     *
     * @throws IllegalStateException if this user already registered an institution,
     *         or if the registration_number is already in use.
     */
    public Institution registerInstitution(int userId, String institutionName,
                                            InstitutionType institutionType, String registrationNumber,
                                            String description, String address, String city,
                                            String state, String pincode, String contactPerson)
            throws SQLException {

        Institution existing = institutionDAO.findByUserId(userId);
        if (existing != null) {
            throw new IllegalStateException(
                    "This user has already registered an institution (institution_id="
                            + existing.getInstitutionId() + ")");
        }

        Institution institution = new Institution(userId, institutionName, institutionType,
                registrationNumber, description, address, city, state, pincode, contactPerson);

        try {
            institutionDAO.insert(institution);
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY_ERROR_CODE) {
                throw new IllegalStateException(
                        "Registration number '" + registrationNumber + "' is already registered.");
            }
            throw e; // some other DB error - let it propagate
        }

        return institution;
    }

    /**
     * Fetches an institution profile by institution_id.
     *
     * @throws IllegalArgumentException if no such institution exists.
     */
    public Institution getInstitution(int institutionId) throws SQLException {
        Institution institution = institutionDAO.findById(institutionId);
        if (institution == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
        return institution;
    }

    /**
     * Fetches an institution profile by the linked user_id.
     * Returns null if this user hasn't registered an institution yet
     * (a normal, expected case - e.g. deciding whether to show
     * "Register Institution" vs "My Institution Dashboard" on the frontend).
     */
    public Institution getInstitutionByUserId(int userId) throws SQLException {
        return institutionDAO.findByUserId(userId);
    }

    /**
     * Returns every registered institution.
     */
    public List<Institution> listAllInstitutions() throws SQLException {
        return institutionDAO.findAll();
    }

    /**
     * Returns only institutions that an admin has approved. Useful for
     * donor/volunteer-facing pages that should only show verified institutions.
     */
    public List<Institution> listApprovedInstitutions() throws SQLException {
        return institutionDAO.findAll().stream()
                .filter(i -> i.getVerificationStatus() == VerificationStatus.APPROVED)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Updates the editable profile fields of an institution (name, type,
     * registration number, description, address, city, state, pincode,
     * contact person). Does not touch verification status.
     *
     * @throws IllegalArgumentException if no such institution exists.
     * @throws IllegalStateException if the new registration_number collides
     *         with a different institution's.
     */
    public void updateProfile(int institutionId, String institutionName, InstitutionType institutionType,
                               String registrationNumber, String description, String address,
                               String city, String state, String pincode, String contactPerson)
            throws SQLException {

        Institution existing = institutionDAO.findById(institutionId);
        if (existing == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }

        existing.setInstitutionName(institutionName);
        existing.setInstitutionType(institutionType);
        existing.setRegistrationNumber(registrationNumber);
        existing.setDescription(description);
        existing.setAddress(address);
        existing.setCity(city);
        existing.setState(state);
        existing.setPincode(pincode);
        existing.setContactPerson(contactPerson);

        try {
            institutionDAO.update(existing);
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY_ERROR_CODE) {
                throw new IllegalStateException(
                        "Registration number '" + registrationNumber + "' is already registered.");
            }
            throw e;
        }
    }

    /**
     * An admin approves an institution's registration.
     *
     * @throws IllegalArgumentException if no such institution exists.
     * @throws IllegalStateException if the institution isn't currently PENDING.
     */
    public void approveInstitution(int institutionId) throws SQLException {
        Institution institution = institutionDAO.findById(institutionId);
        if (institution == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }

        if (institution.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a PENDING institution can be approved. Current status: "
                            + institution.getVerificationStatus());
        }

        institutionDAO.updateVerificationStatus(institutionId, VerificationStatus.APPROVED, null);
    }

    /**
     * An admin rejects an institution's registration, with a reason.
     *
     * @throws IllegalArgumentException if no such institution exists.
     * @throws IllegalStateException if the institution isn't currently PENDING.
     */
    public void rejectInstitution(int institutionId, String rejectionReason) throws SQLException {
        Institution institution = institutionDAO.findById(institutionId);
        if (institution == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }

        if (institution.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a PENDING institution can be rejected. Current status: "
                            + institution.getVerificationStatus());
        }

        institutionDAO.updateVerificationStatus(institutionId, VerificationStatus.REJECTED, rejectionReason);
    }

    /**
     * Deletes an institution's registration entirely.
     * Note: cascades to delete its documents, needs, events, donations, etc. (DB-enforced).
     *
     * @throws IllegalArgumentException if no such institution exists.
     */
    public void deleteInstitution(int institutionId) throws SQLException {
        boolean deleted = institutionDAO.delete(institutionId);
        if (!deleted) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
    }
}
