package com.careconnect.controller;

import com.careconnect.model.User;
import com.careconnect.service.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller (Servlet) for the logged-in user's own profile - Module 1.
 *
 * Endpoints (all require an active session - i.e. the caller already hit
 * POST /api/auth/login):
 *   GET  /api/profile           -> view own profile
 *   PUT  /api/profile           -> update full name / phone / address / profile photo
 *   POST /api/profile/password  -> change password
 *
 * requireLogin() below is the pattern any other controller can copy if it
 * needs to know who's calling (e.g. NeedController checking that the
 * caller's institution owns the need it's editing).
 */
@WebServlet({"/api/profile", "/api/profile/password"})
public class ProfileController extends HttpServlet {

    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    private static class UpdateProfileRequest {
        String fullName;
        String phone;
        String address;
        String profilePhoto;
    }

    private static class ChangePasswordRequest {
        String oldPassword;
        String newPassword;
    }

    private static class MessageResponse {
        String message;
        MessageResponse(String message) { this.message = message; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (!"/api/profile".equals(req.getServletPath())) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown profile endpoint.");
            return;
        }

        Integer userId = requireLogin(req, resp);
        if (userId == null) {
            return; // requireLogin already wrote the 401 response
        }

        try {
            User user = userService.getProfile(userId);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(user));
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (!"/api/profile".equals(req.getServletPath())) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown profile endpoint.");
            return;
        }

        Integer userId = requireLogin(req, resp);
        if (userId == null) {
            return;
        }

        try {
            UpdateProfileRequest body = readJsonBody(req, UpdateProfileRequest.class);
            User updated = userService.updateProfile(userId, body.fullName, body.phone,
                    body.address, body.profilePhoto);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(updated));
        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (!"/api/profile/password".equals(req.getServletPath())) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown profile endpoint.");
            return;
        }

        Integer userId = requireLogin(req, resp);
        if (userId == null) {
            return;
        }

        try {
            ChangePasswordRequest body = readJsonBody(req, ChangePasswordRequest.class);
            userService.changePassword(userId, body.oldPassword, body.newPassword);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(new MessageResponse("Password changed successfully")));
        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    /**
     * Checks for a logged-in session. If present, returns the userId.
     * If absent, writes a 401 JSON error and returns null - callers must
     * check for null and return immediately without writing anything else.
     */
    private Integer requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }

    private <T> T readJsonBody(HttpServletRequest req, Class<T> type) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return gson.fromJson(sb.toString(), type);
    }

    private void writeError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.getWriter().write(gson.toJson(new ErrorResponse(message)));
    }

    private static class ErrorResponse {
        String error;
        ErrorResponse(String error) { this.error = error; }
    }
}
