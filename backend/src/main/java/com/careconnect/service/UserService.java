package com.careconnect.service;

import com.careconnect.dao.UserDAO;
import com.careconnect.model.User;
import com.careconnect.model.UserRole;
import com.careconnect.util.PasswordUtil;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Business logic for Module 1 (User Management): registration, login,
 * profile view/edit, change password. Sits between the Controller and
 * UserDAO - the Controller should call this class, never UserDAO directly.
 *
 * Follows the same pattern as NeedService/InstitutionService: validation
 * failures throw plain IllegalArgumentException with a message the
 * controller can send straight back to the client.
 */
public class UserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Registers a new user.
     *
     * ADMIN is intentionally not allowed here - self-registering as admin
     * through the public form would be a privilege escalation bug. Create
     * the first admin row directly in MySQL (hash its password with
     * PasswordUtil.hashPassword() first) instead.
     *
     * @throws IllegalArgumentException if any required field is missing/invalid,
     *         role is ADMIN or null, or the email is already registered.
     */
    public User register(String fullName, String email, String password, String phone,
                          UserRole role, String address) throws SQLException {

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName cannot be blank");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (role == null || role == UserRole.ADMIN) {
            throw new IllegalArgumentException("role must be one of DONOR, VOLUNTEER, INSTITUTION");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userDAO.emailExists(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(PasswordUtil.hashPassword(password));
        user.setPhone(phone);
        user.setRole(role);
        user.setAddress(address);

        boolean inserted = userDAO.insert(user);
        if (!inserted) {
            throw new IllegalArgumentException("Registration failed, please try again");
        }
        return user;
    }

    /**
     * Verifies email + password.
     *
     * @throws IllegalArgumentException with a generic "Invalid email or password"
     *         message for both "no such user" and "wrong password" - deliberately
     *         not revealing which one it was.
     */
    public User login(String email, String password) throws SQLException {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("email and password are required");
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null || !PasswordUtil.verifyPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return user;
    }

    /**
     * @throws IllegalArgumentException if no such user exists.
     */
    public User getProfile(int userId) throws SQLException {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("No user found with user_id=" + userId);
        }
        return user;
    }

    /**
     * Updates the editable profile fields (full name, phone, address, profile photo).
     * Does not touch email, password, or role.
     *
     * @throws IllegalArgumentException if fullName is blank or no such user exists.
     */
    public User updateProfile(int userId, String fullName, String phone, String address, String profilePhoto)
            throws SQLException {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName cannot be blank");
        }
        boolean updated = userDAO.updateProfile(userId, fullName.trim(), phone, address, profilePhoto);
        if (!updated) {
            throw new IllegalArgumentException("No user found with user_id=" + userId);
        }
        return userDAO.findById(userId);
    }

    /**
     * Changes a user's password after verifying their current one.
     *
     * @throws IllegalArgumentException if the new password is too short,
     *         the current password is wrong, or no such user exists.
     */
    public void changePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        if (oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("oldPassword and newPassword are required");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("No user found with user_id=" + userId);
        }
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        userDAO.updatePassword(userId, PasswordUtil.hashPassword(newPassword));
    }
}
