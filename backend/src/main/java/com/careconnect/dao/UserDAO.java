package com.careconnect.dao;

import com.careconnect.model.User;
import com.careconnect.model.UserRole;
import com.careconnect.util.DBConnection;

import java.sql.*;

/**
 * DAO for the `users` table - the official version for User Management
 * (Module 1). Handles the write path (create, update profile, update
 * password) that AdminUserDAO intentionally left out, since that class was
 * built read-only for the Admin "view users" feature.
 *
 * AdminUserDAO.findAll()/findByRole() can stay as-is - they're a subset
 * of what this class does and don't need to change.
 *
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources), same as every other
 * DAO in this project.
 */
public class UserDAO {

    /**
     * Inserts a new user. Expects fullName, email, password (already hashed),
     * role to be set; phone/address/profilePhoto may be null.
     * On success, sets the generated user_id back onto the passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     */
    public boolean insert(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password, phone, role, address, profile_photo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getPhone());
            stmt.setString(5, user.getRole().name());
            stmt.setString(6, user.getAddress());
            stmt.setString(7, user.getProfilePhoto());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /** @return true if a user with this email already exists. */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Finds a user by email, including the password hash (needed internally
     * by UserService for login verification - never returned over the API
     * since User.password is transient and Gson skips it).
     *
     * @return the User, or null if no row matches.
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
                return null;
            }
        }
    }

    /**
     * Finds a user by user_id, including the password hash (needed internally
     * by UserService for change-password verification).
     *
     * @return the User, or null if no row matches.
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
                return null;
            }
        }
    }

    /**
     * Updates the editable profile fields (not email, password, or role).
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean updateProfile(int userId, String fullName, String phone, String address, String profilePhoto) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, address = ?, profile_photo = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, phone);
            stmt.setString(3, address);
            stmt.setString(4, profilePhoto);
            stmt.setInt(5, userId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Overwrites the stored password hash for a user (used after
     * PasswordUtil.hashPassword() has already been called on the new password).
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean updatePassword(int userId, String newHashedPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHashedPassword);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() == 1;
        }
    }

    /** Maps the current row of a ResultSet to a User object, including password. */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
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
