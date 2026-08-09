package com.careconnect.controller;

import com.careconnect.model.Volunteer;
import com.careconnect.service.VolunteerService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller (Servlet) for volunteer profile endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only
 * (reading requests, writing responses, status codes). All business logic
 * lives in VolunteerService.
 *
 * Endpoints:
 *   GET  /api/volunteers        -> list all volunteer profiles
 *   GET  /api/volunteers/{id}   -> get one volunteer profile by volunteer_id
 *   POST /api/volunteers        -> create a new volunteer profile
 *
 * Uses annotation-based routing (@WebServlet), so no web.xml entry is needed.
 */
@WebServlet("/api/volunteers/*")
public class VolunteerController extends HttpServlet {

    private final VolunteerService volunteerService = new VolunteerService();
    private final Gson gson = new Gson();

    /**
     * Small DTO matching the JSON body expected on POST.
     * Kept separate from the Volunteer model since the incoming request
     * shouldn't be able to set volunteer_id or created_at directly.
     */
    private static class CreateVolunteerRequest {
        int userId;
        String skills;
        String availability;
        String experience;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo(); // null or "/" for the collection, "/{id}" for a single resource

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/volunteers -> list all
                List<Volunteer> volunteers = volunteerService.listAllVolunteers();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(volunteers));

            } else {
                // GET /api/volunteers/{id} -> single volunteer
                int volunteerId = parseIdFromPath(pathInfo);
                Volunteer volunteer = volunteerService.getProfile(volunteerId);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(volunteer));
            }

        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid volunteer ID in URL.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
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

        try {
            CreateVolunteerRequest body = readJsonBody(req, CreateVolunteerRequest.class);

            Volunteer created = volunteerService.createProfile(
                    body.userId, body.skills, body.availability, body.experience);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(gson.toJson(created));

        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
        } catch (IllegalStateException e) {
            // e.g. this user already has a volunteer profile
            writeError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    /**
     * Extracts the numeric ID from a path like "/5" -> 5.
     */
    private int parseIdFromPath(String pathInfo) {
        String idStr = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        return Integer.parseInt(idStr);
    }

    /**
     * Reads and parses the request body as JSON into the given type.
     */
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

    /**
     * Writes a JSON error response: {"error": "message"}
     */
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