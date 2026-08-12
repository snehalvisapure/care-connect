package com.careconnect.dao;

import com.careconnect.model.Donation;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only DAO for admin-wide donation viewing, built by Member 3 (Nehaa)
 * to support the Admin module. Uses Member 2's Donation model
 * (com.careconnect.model.Donation) - does NOT duplicate that model.
 *
 * Member 2's DonationDAO (this same package) only has donor-specific and
 * institution-specific queries (getDonationsByDonor, getDonationsByInstitution) -
 * this class adds the admin-wide "see everything" view on top, without
 * touching their file.
 */
public class AdminDonationDAO {

    /**
     * Returns every donation in the system, most recent first.
     */
    public List<Donation> findAll() throws SQLException {
        String sql = "SELECT * FROM donations ORDER BY donation_date DESC";
        List<Donation> donations = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                donations.add(mapDonation(rs));
            }
        }
        return donations;
    }

    /**
     * Maps the current row of a ResultSet to a Donation object.
     * Mirrors DonationDAO's own mapping - kept local so this class has no
     * dependency on DonationDAO's private methods.
     */
    private Donation mapDonation(ResultSet rs) throws SQLException {
        Donation donation = new Donation();

        donation.setDonationId(rs.getInt("donation_id"));
        donation.setDonorId(rs.getInt("donor_id"));
        donation.setInstitutionId(rs.getInt("institution_id"));

        int needId = rs.getInt("need_id");
        donation.setNeedId(rs.wasNull() ? null : needId);

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
