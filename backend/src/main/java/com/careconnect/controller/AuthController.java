package com.careconnect.controller;

import com.careconnect.model.User;
import com.careconnect.model.UserRole;
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
 * Controller (Servlet) for authentication endpoints - Module 1.
 * All business logic and validation lives in UserService.
 *
 * Endpoints:
 *   POST /api/auth/register  -> create a DONOR/VOLUNTEER/INSTITUTION account
 *   POST /api/auth/login     -> verify credentials, start a session
 *   POST /api/auth/logout    -> invalidate the current session
 *
 * On login, the session stores userId/role/fullName. Any other controller
 * that needs to know who's calling (e.g. "only this need's owning
 * institution can edit it") can read those the same way ProfileController
 * does - see requireLogin() there for the pattern.
 *
 * ADMIN accounts are not self-registerable through /api/auth/register on
 * purpose (see UserService.register) - create the first admin row directly
 * in MySQL, hashing its password with PasswordUtil.hashPassword() first.
 */
@WebServlet({"/api/auth/register", "/api/auth/login", "/api/auth/logout"})
public class AuthController extends HttpServlet {

    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    private static class RegisterRequest {
        String fullName;
        String email;
        String password;
        String phone;
        UserRole role;
        String address;
    }

    private static class LoginRequest {
        String email;
        String password;
    }

    private static class MessageResponse {
        String message;
        MessageResponse(String message) { this.message = message; }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getServletPath();

        try {
            switch (servletPath) {
                case "/api/auth/register": {
                    RegisterRequest body = readJsonBody(req, RegisterRequest.class);
                    User created = userService.register(body.fullName, body.email, body.password,
                            body.phone, body.role, body.address);
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write(gson.toJson(created));
                    break;
                }
                case "/api/auth/login": {
                    LoginRequest body = readJsonBody(req, LoginRequest.class);
                    User user;
                    try {
                        user = userService.login(body.email, body.password);
                    } catch (IllegalArgumentException e) {
                        // Wrong credentials specifically map to 401, not 400
                        writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                        return;
                    }

                    HttpSession session = req.getSession(true);
                    session.setAttribute("userId", user.getUserId());
                    session.setAttribute("role", user.getRole());
                    session.setAttribute("fullName", user.getFullName());
                    session.setMaxInactiveInterval(30 * 60); // 30 minutes

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(user));
                    break;
                }
                case "/api/auth/logout": {
                    HttpSession session = req.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(new MessageResponse("Logged out successfully")));
                    break;
                }
                default:
                    writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown auth endpoint.");
            }

        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
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
