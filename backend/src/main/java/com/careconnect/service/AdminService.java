
package com.careconnect.service;

import com.careconnect.dao.AdminDonationDAO;
import com.careconnect.dao.AdminUserDAO;
import com.careconnect.model.Donation;
import com.careconnect.model.User;
import com.careconnect.model.UserRole;

import java.sql.SQLException;
import java.util.List;

public class AdminService {

    private final AdminUserDAO userDAO;
    private final AdminDonationDAO donationDAO;

    public AdminService() {
        this.userDAO = new AdminUserDAO();
        this.donationDAO = new AdminDonationDAO();
    }

    public List<User> listAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    public List<User> listUsersByRole(UserRole role) throws SQLException {
        return userDAO.findByRole(role);
    }

    public List<Donation> listAllDonations() throws SQLException {
        return donationDAO.findAll();
    }
}