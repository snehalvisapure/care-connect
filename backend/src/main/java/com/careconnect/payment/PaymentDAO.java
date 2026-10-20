package com.careconnect.payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    private static final String COLUMNS =
            "payment_id, user_id, reference_type, reference_id, amount_paise, currency, status, "
          + "razorpay_order_id, razorpay_payment_id, razorpay_signature, failure_reason, created_at";

    /** Inserts a new payment in CREATED state and returns its generated id. */
    public long insertCreated(Payment p) throws SQLException {
        String sql = "INSERT INTO payments "
                + "(user_id, reference_type, reference_id, amount_paise, currency, status, razorpay_order_id) "
                + "VALUES (?, ?, ?, ?, ?, 'CREATED', ?)";
        try (Connection c = PaymentDb.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getUserId());
            ps.setString(2, p.getReferenceType());
            ps.setLong(3, p.getReferenceId());
            ps.setLong(4, p.getAmountPaise());
            ps.setString(5, p.getCurrency());
            ps.setString(6, p.getRazorpayOrderId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    public Payment findByOrderId(String orderId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM payments WHERE razorpay_order_id = ?";
        try (Connection c = PaymentDb.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Returns the PAID payment for this item, if one exists. Used to stop a second charge. */
    public Payment findPaidByReference(String referenceType, long referenceId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM payments "
                + "WHERE reference_type = ? AND reference_id = ? AND status = 'PAID' LIMIT 1";
        try (Connection c = PaymentDb.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, referenceType);
            ps.setLong(2, referenceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Payment> findByUser(int userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM payments WHERE user_id = ? ORDER BY created_at DESC";
        List<Payment> list = new ArrayList<>();
        try (Connection c = PaymentDb.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Marks the payment PAID. Returns 1 the first time and 0 if it was already PAID or the
     * order is unknown. A FAILED payment can still become PAID, because Razorpay lets the user
     * retry on the same order.
     */
    public int markPaid(String orderId, String paymentId, String signature) throws SQLException {
        String sql = "UPDATE payments SET status = 'PAID', razorpay_payment_id = ?, "
                + "razorpay_signature = COALESCE(?, razorpay_signature), failure_reason = NULL "
                + "WHERE razorpay_order_id = ? AND status <> 'PAID'";
        try (Connection c = PaymentDb.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            ps.setString(2, signature);
            ps.setString(3, orderId);
            return ps.executeUpdate();
        }
    }

    /** Marks a still-open payment FAILED. Never overwrites a PAID payment. */
    public int markFailed(String orderId, String reason) throws SQLException {
        String sql = "UPDATE payments SET status = 'FAILED', failure_reason = ? "
                + "WHERE razorpay_order_id = ? AND status = 'CREATED'";
        try (Connection c = PaymentDb.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String r = reason == null ? null : (reason.length() > 255 ? reason.substring(0, 255) : reason);
            ps.setString(1, r);
            ps.setString(2, orderId);
            return ps.executeUpdate();
        }
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getLong("payment_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setReferenceType(rs.getString("reference_type"));
        p.setReferenceId(rs.getLong("reference_id"));
        p.setAmountPaise(rs.getLong("amount_paise"));
        p.setCurrency(rs.getString("currency"));
        p.setStatus(rs.getString("status"));
        p.setRazorpayOrderId(rs.getString("razorpay_order_id"));
        p.setRazorpayPaymentId(rs.getString("razorpay_payment_id"));
        p.setRazorpaySignature(rs.getString("razorpay_signature"));
        p.setFailureReason(rs.getString("failure_reason"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
