package com.careconnect.dao;

import com.careconnect.model.Need;
import com.careconnect.model.NeedStatus;
import com.careconnect.model.NeedUrgency;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * PLACEHOLDER read-only search DAO for `needs`, built by Member 3 (Nehaa)
 * on 2026-08-09 to support Search & Filter, because Member 2 had not yet
 * built the full Need DAO for the Needs/Requirements module.
 *
 * This DAO ONLY reads (SELECT) - all create/update/delete for needs
 * belongs in Member 2's NeedDAO once they build it. If/when that lands,
 * reconcile - this search-specific query might still be worth keeping
 * separately, since search filtering logic is often kept apart from basic
 * CRUD even in DAOs that "own" a table.
 *
 * All filter parameters are optional (pass null to skip that filter) -
 * the WHERE clause is built dynamically based on which filters are provided.
 */
public class NeedSearchDAO {

    /**
     * Searches needs with any combination of optional filters.
     * Pass null for any filter you don't want applied.
     *
     * @param category      partial match (SQL LIKE) on category, case-insensitive
     * @param urgency       exact match on urgency
     * @param institutionId exact match on institution_id
     * @param status        exact match on status
     */
    public List<Need> search(String category, NeedUrgency urgency, Integer institutionId,
                              NeedStatus status) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT * FROM needs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            sql.append(" AND category LIKE ?");
            params.add("%" + category + "%");
        }
        if (urgency != null) {
            sql.append(" AND urgency = ?");
            params.add(urgency.name());
        }
        if (institutionId != null) {
            sql.append(" AND institution_id = ?");
            params.add(institutionId);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }

        List<Need> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToNeed(rs));
                }
            }
        }
        return results;
    }

    private Need mapRowToNeed(ResultSet rs) throws SQLException {
        Need need = new Need();
        need.setNeedId(rs.getInt("need_id"));
        need.setInstitutionId(rs.getInt("institution_id"));
        need.setCategory(rs.getString("category"));
        need.setItemName(rs.getString("item_name"));
        need.setQuantityRequired(rs.getInt("quantity_required"));
        need.setQuantityReceived(rs.getInt("quantity_received"));

        String urgencyStr = rs.getString("urgency");
        need.setUrgency(urgencyStr != null ? NeedUrgency.valueOf(urgencyStr) : null);

        need.setDescription(rs.getString("description"));

        String statusStr = rs.getString("status");
        need.setStatus(statusStr != null ? NeedStatus.valueOf(statusStr) : null);

        Timestamp postedDate = rs.getTimestamp("posted_date");
        need.setPostedDate(postedDate);

        return need;
    }
}
