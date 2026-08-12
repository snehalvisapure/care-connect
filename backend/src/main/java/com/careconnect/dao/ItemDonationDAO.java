package com.careconnect.dao;

import com.careconnect.model.DeliveryStatus;
import com.careconnect.model.ItemDonation;
import com.careconnect.model.PickupOrDrop;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the `item_donations` table - core CRUD only.
 *
 * Each row here belongs to a parent `donations` row (donation_id, ON DELETE
 * CASCADE) which carries donor_id / institution_id / need_id. To list item
 * donations "for a donor" or "for an institution" this DAO joins against
 * `donations`, since item_donations itself has no donor_id/institution_id
 * column - that keeps the parent/child relationship exactly as modeled by
 * the schema instead of denormalizing it.
 *
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 */
public class ItemDonationDAO {

    /**
     * Inserts a new item donation row. Expects donationId, itemName,
     * quantity, pickupOrDrop and deliveryStatus (PENDING) to be set on the
     * passed-in object. On success, sets the generated item_donation_id
     * back onto it.
     *
     * @return true if the insert affected exactly 1 row.
     */
    public boolean insert(ItemDonation itemDonation) throws SQLException {
        String sql = "INSERT INTO item_donations " +
                "(donation_id, item_name, quantity, pickup_or_drop, pickup_address, delivery_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, itemDonation.getDonationId());
            stmt.setString(2, itemDonation.getItemName());
            stmt.setInt(3, itemDonation.getQuantity());
            stmt.setString(4, itemDonation.getPickupOrDrop().name());
            stmt.setString(5, itemDonation.getPickupAddress());
            stmt.setString(6, itemDonation.getDeliveryStatus().name());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        itemDonation.setItemDonationId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds a single item donation by its item_donation_id.
     *
     * @return the ItemDonation, or null if no row matches.
     */
    public ItemDonation findById(int itemDonationId) throws SQLException {
        String sql = "SELECT * FROM item_donations WHERE item_donation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemDonationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToItemDonation(rs);
                }
                return null;
            }
        }
    }

    /**
     * Finds the item donation row belonging to a given parent donation_id
     * (each ITEM donation has exactly one item_donations row).
     *
     * @return the ItemDonation, or null if no row matches.
     */
    public ItemDonation findByDonationId(int donationId) throws SQLException {
        String sql = "SELECT * FROM item_donations WHERE donation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToItemDonation(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns every item donation made to a given institution, most recent first.
     * Joins against `donations` since institution_id lives there.
     */
    public List<ItemDonation> findByInstitutionId(int institutionId) throws SQLException {
        String sql = "SELECT id.* FROM item_donations id " +
                "JOIN donations d ON id.donation_id = d.donation_id " +
                "WHERE d.institution_id = ? " +
                "ORDER BY d.donation_date DESC";

        return queryList(sql, institutionId);
    }

    /**
     * Returns every item donation made by a given donor, most recent first.
     * Joins against `donations` since donor_id lives there.
     */
    public List<ItemDonation> findByDonorId(int donorId) throws SQLException {
        String sql = "SELECT id.* FROM item_donations id " +
                "JOIN donations d ON id.donation_id = d.donation_id " +
                "WHERE d.donor_id = ? " +
                "ORDER BY d.donation_date DESC";

        return queryList(sql, donorId);
    }

    /**
     * Updates only delivery_status.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean updateDeliveryStatus(int itemDonationId, DeliveryStatus status) throws SQLException {
        String sql = "UPDATE item_donations SET delivery_status = ? WHERE item_donation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, itemDonationId);

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Updates only delivery_date (set when a donation is marked DELIVERED).
     * Pass null to clear it.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean updateDeliveryDate(int itemDonationId, Date deliveryDate) throws SQLException {
        String sql = "UPDATE item_donations SET delivery_date = ? WHERE item_donation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (deliveryDate != null) {
                stmt.setDate(1, deliveryDate);
            } else {
                stmt.setNull(1, Types.DATE);
            }
            stmt.setInt(2, itemDonationId);

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes an item donation row by item_donation_id.
     * Note: normally this cascades automatically from deleting the parent
     * `donations` row - this method is for deleting just the item_donations
     * row directly, if ever needed.
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int itemDonationId) throws SQLException {
        String sql = "DELETE FROM item_donations WHERE item_donation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemDonationId);
            return stmt.executeUpdate() == 1;
        }
    }

    private List<ItemDonation> queryList(String sql, int param) throws SQLException {
        List<ItemDonation> itemDonations = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, param);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itemDonations.add(mapRowToItemDonation(rs));
                }
            }
        }
        return itemDonations;
    }

    /**
     * Maps the current row of a ResultSet to an ItemDonation object.
     */
    private ItemDonation mapRowToItemDonation(ResultSet rs) throws SQLException {
        ItemDonation itemDonation = new ItemDonation();
        itemDonation.setItemDonationId(rs.getInt("item_donation_id"));
        itemDonation.setDonationId(rs.getInt("donation_id"));
        itemDonation.setItemName(rs.getString("item_name"));
        itemDonation.setQuantity(rs.getInt("quantity"));

        String pickupOrDropStr = rs.getString("pickup_or_drop");
        itemDonation.setPickupOrDrop(pickupOrDropStr != null ? PickupOrDrop.valueOf(pickupOrDropStr) : null);

        itemDonation.setPickupAddress(rs.getString("pickup_address"));

        String deliveryStatusStr = rs.getString("delivery_status");
        itemDonation.setDeliveryStatus(deliveryStatusStr != null ? DeliveryStatus.valueOf(deliveryStatusStr) : null);

        itemDonation.setDeliveryDate(rs.getDate("delivery_date"));

        return itemDonation;
    }
}
