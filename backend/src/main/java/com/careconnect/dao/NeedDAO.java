package com.careconnect.dao;

import com.careconnect.model.Need;
import com.careconnect.model.NeedStatus;
import com.careconnect.model.NeedUrgency;
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
 * DAO for the `needs` table - core CRUD only.
 *
 * Filtered/multi-criteria searching (category + urgency + status combos)
 * already lives in NeedSearchDAO (Member 3's Search & Filter module) - this
 * class doesn't duplicate that, it only owns create/read/update/delete for
 * a single need or a single institution's needs.
 *
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 */
public class NeedDAO {

    /**
     * Inserts a new need.
     * Expects need.getInstitutionId(), getCategory(), getItemName(),
     * getQuantityRequired(), getQuantityReceived(), getUrgency(),
     * getDescription(), getStatus() to be set (NeedService sets
     * quantityReceived=0 and status=OPEN for brand-new needs).
     * On success, sets the generated need_id back onto the passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     */
    public boolean insert(Need need) throws SQLException {
        String sql = "INSERT INTO needs " +
                "(institution_id, category, item_name, quantity_required, quantity_received, " +
                "urgency, description, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, need.getInstitutionId());
            stmt.setString(2, need.getCategory());
            stmt.setString(3, need.getItemName());
            stmt.setInt(4, need.getQuantityRequired());
            stmt.setInt(5, need.getQuantityReceived());
            stmt.setString(6, need.getUrgency().name());
            stmt.setString(7, need.getDescription());
            stmt.setString(8, need.getStatus().name());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        need.setNeedId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds a single need by its need_id.
     *
     * @return the Need, or null if no row matches.
     */
    public Need findById(int needId) throws SQLException {
        String sql = "SELECT * FROM needs WHERE need_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, needId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToNeed(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns every need posted by a given institution, most recently
     * posted first. For filtered/combined-criteria search across all
     * institutions, use NeedSearchDAO instead.
     */
    public List<Need> findByInstitutionId(int institutionId) throws SQLException {
        String sql = "SELECT * FROM needs WHERE institution_id = ? ORDER BY posted_date DESC";

        List<Need> needs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, institutionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    needs.add(mapRowToNeed(rs));
                }
            }
        }
        return needs;
    }

    /**
     * Updates the editable fields of a need (category, item name, quantity
     * required, urgency, description). Does not touch quantity_received or
     * status - use updateQuantityReceived() / updateStatus() for those, so
     * an institution editing a need's description can never accidentally
     * wipe out fulfillment progress.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean update(Need need) throws SQLException {
        String sql = "UPDATE needs SET category = ?, item_name = ?, quantity_required = ?, " +
                "urgency = ?, description = ? WHERE need_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, need.getCategory());
            stmt.setString(2, need.getItemName());
            stmt.setInt(3, need.getQuantityRequired());
            stmt.setString(4, need.getUrgency().name());
            stmt.setString(5, need.getDescription());
            stmt.setInt(6, need.getNeedId());

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Updates only quantity_received. Kept separate from update() since
     * this changes based on donations coming in, not the institution
     * editing its own posting.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean updateQuantityReceived(int needId, int quantityReceived) throws SQLException {
        String sql = "UPDATE needs SET quantity_received = ? WHERE need_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantityReceived);
            stmt.setInt(2, needId);

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Updates only status. Kept separate so NeedService can recalculate
     * quantity_received and status together (see updateQuantityReceived).
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean updateStatus(int needId, NeedStatus status) throws SQLException {
        String sql = "UPDATE needs SET status = ? WHERE need_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, needId);

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes a need by need_id.
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int needId) throws SQLException {
        String sql = "DELETE FROM needs WHERE need_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, needId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Maps the current row of a ResultSet to a Need object.
     */
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
