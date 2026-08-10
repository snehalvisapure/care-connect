package com.careconnect.dao;

import com.careconnect.model.Institution;
import com.careconnect.model.InstitutionType;
import com.careconnect.model.VerificationStatus;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only search DAO for `institutions`, built by Member 3 (Nehaa) to
 * support Search & Filter (Module 7). Uses Member 2's Institution model
 * (com.careconnect.model.Institution) - this DAO does NOT duplicate that
 * model, it only adds dynamic-filter search capability on top of it.
 *
 * InstitutionDAO (Member 2's, in this same package) already covers core
 * CRUD - this class is intentionally separate and read-only, since search
 * filtering (building a dynamic WHERE clause from optional parameters) is
 * a different concern from basic CRUD and keeping it separate avoids the
 * two of us editing the same file.
 *
 * All filter parameters are optional (pass null to skip that filter) -
 * the WHERE clause is built dynamically based on which filters are provided.
 */
public class InstitutionSearchDAO {

    /**
     * Searches institutions with any combination of optional filters.
     * Pass null for any filter you don't want applied.
     *
     * @param city               partial match (SQL LIKE) on city, case-insensitive
     * @param institutionType    exact match on institution_type
     * @param verificationStatus exact match on verification_status
     */
    public List<Institution> search(String city, InstitutionType institutionType,
                                     VerificationStatus verificationStatus) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT * FROM institutions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (city != null && !city.isBlank()) {
            sql.append(" AND city LIKE ?");
            params.add("%" + city + "%");
        }
        if (institutionType != null) {
            sql.append(" AND institution_type = ?");
            params.add(institutionType.name());
        }
        if (verificationStatus != null) {
            sql.append(" AND verification_status = ?");
            params.add(verificationStatus.name());
        }

        List<Institution> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToInstitution(rs));
                }
            }
        }
        return results;
    }

    /**
     * Maps the current row of a ResultSet to an Institution object.
     * Mirrors InstitutionDAO's own mapping - kept local here so this class
     * has no dependency on InstitutionDAO's private methods.
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
