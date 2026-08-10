package com.careconnect.dao;

import com.careconnect.model.Institution;
import com.careconnect.model.InstitutionType;
import com.careconnect.model.VerificationStatus;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the `institutions` table. Core CRUD only.
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 */
public class InstitutionDAO {

    /**
     * Inserts a new institution registration.
     * Expects institution.getUserId(), getInstitutionName(), getInstitutionType(),
     * getRegistrationNumber(), getDescription(), getAddress(), getCity(), getState(),
     * getPincode(), getContactPerson() to be set.
     * verificationStatus defaults to PENDING (set by the model's short constructor).
     * On success, sets the generated institution_id back onto the passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     */
    public boolean insert(Institution institution) throws SQLException {
        String sql = "INSERT INTO institutions " +
                "(user_id, institution_name, institution_type, registration_number, " +
                "description, address, city, state, pincode, contact_person, verification_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, institution.getUserId());
            stmt.setString(2, institution.getInstitutionName());
            stmt.setString(3, institution.getInstitutionType().name());
            stmt.setString(4, institution.getRegistrationNumber());
            stmt.setString(5, institution.getDescription());
            stmt.setString(6, institution.getAddress());
            stmt.setString(7, institution.getCity());
            stmt.setString(8, institution.getState());
            stmt.setString(9, institution.getPincode());
            stmt.setString(10, institution.getContactPerson());
            VerificationStatus status = institution.getVerificationStatus() != null
                    ? institution.getVerificationStatus() : VerificationStatus.PENDING;
            stmt.setString(11, status.name());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        institution.setInstitutionId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds an institution by its institution_id.
     *
     * @return the Institution, or null if no row matches.
     */
    public Institution findById(int institutionId) throws SQLException {
        String sql = "SELECT * FROM institutions WHERE institution_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, institutionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToInstitution(rs);
                }
                return null;
            }
        }
    }

    /**
     * Finds an institution profile by the linked user_id.
     * Useful for checking "does this user already have an institution profile?"
     * before creating a new one.
     *
     * @return the Institution, or null if this user has no institution profile.
     */
    public Institution findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM institutions WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToInstitution(rs);
                }
                return null;
            }
        }
    }

    /**
     * Finds an institution by its registration_number (UNIQUE in the schema).
     *
     * @return the Institution, or null if no row matches.
     */
    public Institution findByRegistrationNumber(String registrationNumber) throws SQLException {
        String sql = "SELECT * FROM institutions WHERE registration_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, registrationNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToInstitution(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns all institution profiles.
     */
    public List<Institution> findAll() throws SQLException {
        String sql = "SELECT * FROM institutions";
        List<Institution> institutions = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                institutions.add(mapRowToInstitution(rs));
            }
        }
        return institutions;
    }

    /**
     * Updates the editable fields (name, type, registration number, description,
     * address, city, state, pincode, contact person) of an existing institution.
     * Does not touch verification_status or rejection_reason - use
     * updateVerificationStatus() for that. institution_id and user_id are not changed here.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean update(Institution institution) throws SQLException {
        String sql = "UPDATE institutions SET institution_name = ?, institution_type = ?, " +
                "registration_number = ?, description = ?, address = ?, city = ?, " +
                "state = ?, pincode = ?, contact_person = ? WHERE institution_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, institution.getInstitutionName());
            stmt.setString(2, institution.getInstitutionType().name());
            stmt.setString(3, institution.getRegistrationNumber());
            stmt.setString(4, institution.getDescription());
            stmt.setString(5, institution.getAddress());
            stmt.setString(6, institution.getCity());
            stmt.setString(7, institution.getState());
            stmt.setString(8, institution.getPincode());
            stmt.setString(9, institution.getContactPerson());
            stmt.setInt(10, institution.getInstitutionId());

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Updates only the verification_status and rejection_reason of an institution.
     * Used by an admin approving/rejecting a registration - kept separate from
     * update() so the institution's own profile edits can never accidentally
     * change its verification state.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean updateVerificationStatus(int institutionId, VerificationStatus status,
                                             String rejectionReason) throws SQLException {
        String sql = "UPDATE institutions SET verification_status = ?, rejection_reason = ? " +
                "WHERE institution_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setString(2, rejectionReason);
            stmt.setInt(3, institutionId);

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes an institution by institution_id.
     * Note: this will cascade-delete its documents, needs, events, donations, etc.
     * per the ON DELETE CASCADE foreign keys in the schema.
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int institutionId) throws SQLException {
        String sql = "DELETE FROM institutions WHERE institution_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, institutionId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Maps the current row of a ResultSet to an Institution object.
     */
    private Institution mapRowToInstitution(ResultSet rs) throws SQLException {
        Institution institution = new Institution();
        institution.setInstitutionId(rs.getInt("institution_id"));
        institution.setUserId(rs.getInt("user_id"));
        institution.setInstitutionName(rs.getString("institution_name"));

        String typeStr = rs.getString("institution_type");
        institution.setInstitutionType(typeStr != null ? InstitutionType.valueOf(typeStr) : null);

        institution.setRegistrationNumber(rs.getString("registration_number"));
        institution.setDescription(rs.getString("description"));
        institution.setAddress(rs.getString("address"));
        institution.setCity(rs.getString("city"));
        institution.setState(rs.getString("state"));
        institution.setPincode(rs.getString("pincode"));
        institution.setContactPerson(rs.getString("contact_person"));

        String statusStr = rs.getString("verification_status");
        institution.setVerificationStatus(statusStr != null ? VerificationStatus.valueOf(statusStr) : null);

        institution.setRejectionReason(rs.getString("rejection_reason"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        institution.setCreatedAt(createdAt);

        return institution;
    }
}
