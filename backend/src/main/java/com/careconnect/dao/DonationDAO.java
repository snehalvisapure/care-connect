package com.careconnect.dao;

import com.careconnect.model.Donation;
import com.careconnect.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DonationDAO {

    // Create a new donation / pledge
    public int createDonation(Donation donation) throws SQLException {

        String sql = """
                INSERT INTO donations
                (donor_id, institution_id, need_id, donation_type, amount)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, donation.getDonorId());
            statement.setInt(2, donation.getInstitutionId());

            if (donation.getNeedId() != null) {
                statement.setInt(3, donation.getNeedId());
            } else {
                statement.setNull(3, Types.INTEGER);
            }

            statement.setString(4, donation.getDonationType());

            if (donation.getAmount() != null) {
                statement.setBigDecimal(5, donation.getAmount());
            } else {
                statement.setBigDecimal(5, java.math.BigDecimal.ZERO);
            }

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return -1;
    }

    // Get one donation by ID
    public Donation getDonationById(int donationId) throws SQLException {

        String sql = "SELECT * FROM donations WHERE donation_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, donationId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapDonation(rs);
                }
            }
        }

        return null;
    }

    // Get all donations made by a donor
    public List<Donation> getDonationsByDonor(int donorId) throws SQLException {

        String sql = """
                SELECT * FROM donations
                WHERE donor_id = ?
                ORDER BY donation_date DESC
                """;

        List<Donation> donations = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, donorId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    donations.add(mapDonation(rs));
                }
            }
        }

        return donations;
    }

    // Get all donations for an institution
    public List<Donation> getDonationsByInstitution(int institutionId)
            throws SQLException {

        String sql = """
                SELECT * FROM donations
                WHERE institution_id = ?
                ORDER BY donation_date DESC
                """;

        List<Donation> donations = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, institutionId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    donations.add(mapDonation(rs));
                }
            }
        }

        return donations;
    }

    // Get pending pledges
    public List<Donation> getPendingPledgesByDonor(int donorId)
            throws SQLException {

        String sql = """
                SELECT * FROM donations
                WHERE donor_id = ?
                AND payment_status = 'PENDING'
                ORDER BY donation_date DESC
                """;

        List<Donation> pledges = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, donorId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    pledges.add(mapDonation(rs));
                }
            }
        }

        return pledges;
    }

    // Update payment status
    public boolean updatePaymentStatus(int donationId, String paymentStatus)
            throws SQLException {

        String sql = """
                UPDATE donations
                SET payment_status = ?
                WHERE donation_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, paymentStatus);
            statement.setInt(2, donationId);

            return statement.executeUpdate() > 0;
        }
    }

    // Update donation status
    public boolean updateDonationStatus(int donationId, String donationStatus)
            throws SQLException {

        String sql = """
                UPDATE donations
                SET donation_status = ?
                WHERE donation_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, donationStatus);
            statement.setInt(2, donationId);

            return statement.executeUpdate() > 0;
        }
    }

    // Convert database row into Donation object
    private Donation mapDonation(ResultSet rs) throws SQLException {

        Donation donation = new Donation();

        donation.setDonationId(rs.getInt("donation_id"));
        donation.setDonorId(rs.getInt("donor_id"));
        donation.setInstitutionId(rs.getInt("institution_id"));

        int needId = rs.getInt("need_id");
        if (rs.wasNull()) {
            donation.setNeedId(null);
        } else {
            donation.setNeedId(needId);
        }

        donation.setDonationType(rs.getString("donation_type"));
        donation.setAmount(rs.getBigDecimal("amount"));
        donation.setPaymentStatus(rs.getString("payment_status"));
        donation.setTransactionId(rs.getString("transaction_id"));
        donation.setReceiptNumber(rs.getString("receipt_number"));
        donation.setDonationStatus(rs.getString("donation_status"));
        donation.setDonationDate(rs.getTimestamp("donation_date"));

        return donation;
    }
}