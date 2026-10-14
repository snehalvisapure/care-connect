package com.careconnect.dao;

import com.careconnect.model.User;
import com.careconnect.model.UserRole;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * PLACEHOLDER read-only DAO for `users`, built by Member 3 (Nehaa) on
 * 2026-08-09 to support the Admin module's "view users" feature, because
 * Member 1 had not yet built a UserDAO for User Management.
 *
 * READ-ONLY by design - creating/editing/deleting users belongs entirely
 * to Member 1's eventual UserDAO. If/when that lands, reconcile - this
 * class can likely just be deleted in favour of reusing their findAll().
 */
public class AdminUserDAO {

    /**
     * Returns all users. Passwords are never included (see User.password,
     * marked transient) even though this method reads every column.
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    /**
     * Returns all users filtered to a specific role (e.g. all VOLUNTEER users).
     */
    public List<User> findByRole(UserRole role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ?";
        List<User> users = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        // password intentionally never read into the object - not needed for admin viewing
        user.setPhone(rs.getString("phone"));

        String roleStr = rs.getString("role");
        user.setRole(roleStr != null ? UserRole.valueOf(roleStr) : null);

        user.setAddress(rs.getString("address"));
        user.setProfilePhoto(rs.getString("profile_photo"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        user.setCreatedAt(createdAt);

        return user;
    }
}
