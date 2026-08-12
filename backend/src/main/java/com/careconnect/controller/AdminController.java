package com.careconnect.controller;

import com.careconnect.model.Donation;
import com.careconnect.model.User;
import com.careconnect.model.UserRole;
import com.careconnect.service.AdminService;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet({"/api/admin/users", "/api/admin/donations"})
public class AdminController extends HttpServlet {

    private final AdminService adminService = new AdminService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();

        try {
            switch (servletPath) {
                case "/api/admin/users": {
                    String roleParam = req.getParameter("role");
                    List<User> users = roleParam != null
                            ? adminService.listUsersByRole(UserRole.valueOf(roleParam))
                            : adminService.listAllUsers();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(users));
                    break;
                }
                case "/api/admin/donations": {
                    List<Donation> donations = adminService.listAllDonations();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(donations));
                    break;
                }
                default:
                    writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown admin endpoint.");
            }

        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid filter value: " + e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    private void writeError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.getWriter().write(gson.toJson(new ErrorResponse(message)));
    }

    private static class ErrorResponse {
        String error;

        ErrorResponse(String error) {
            this.error = error;
        }
    }
}