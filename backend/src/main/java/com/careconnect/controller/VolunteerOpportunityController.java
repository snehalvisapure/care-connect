package com.careconnect.controller;

import com.careconnect.model.VolunteerOpportunity;
import com.careconnect.service.VolunteerOpportunityService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;

/**
 * Controller (Servlet) for volunteer opportunity endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in VolunteerOpportunityService.
 *
 * Endpoints:
 *   GET  /api/opportunities                     -> list OPEN opportunities (public browsing)
 *   GET  /api/opportunities?institutionId={id}   -> list ALL opportunities for one institution
 *   GET  /api/opportunities/{id}                 -> get one opportunity
 *   POST /api/opportunities                      -> create a new opportunity
 *   POST /api/opportunities/{id}/close           -> close an opportunity
 *   POST /api/opportunities/{id}/complete        -> mark an opportunity completed
 */
@WebServlet("/api/opportunities/*")
public class VolunteerOpportunityController extends HttpServlet {

    private final VolunteerOpportunityService opportunityService = new VolunteerOpportunityService();
    private final Gson gson = new Gson();

    /**
     * DTO matching the JSON body expected on POST /api/opportunities.
     * Dates/times come in as ISO strings (e.g. "2026-09-15", "10:00:00")
     * since Gson can't map JSON strings directly to java.sql.Date/Time.
     */
    private static class CreateOpportunityRequest {
        int institutionId;
        String title;
        String description;
        String requiredSkills;
        String eventDate;   // "yyyy-MM-dd"
        String startTime;   // "HH:mm:ss", nullable
        String endTime;     // "HH:mm:ss", nullable
        String location;
        Integer maxVolunteers;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String institutionIdParam = req.getParameter("institutionId");

                if (institutionIdParam != null) {
                    // GET /api/opportunities?institutionId=1 -> all opportunities for that institution
                    int institutionId = Integer.parseInt(institutionIdParam);
                    List<VolunteerOpportunity> opportunities = opportunityService.listByInstitution(institutionId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(opportunities));
                } else {
                    // GET /api/opportunities -> public browsing, OPEN only
                    List<VolunteerOpportunity> opportunities = opportunityService.listOpenOpportunities();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(opportunities));
                }

            } else {
                // GET /api/opportunities/{id}
                int opportunityId = parseIdFromPath(pathInfo, 1)[0];
                VolunteerOpportunity opportunity = opportunityService.getOpportunity(opportunityId);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(opportunity));
            }

        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID in request.");
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

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // POST /api/opportunities -> create
                createOpportunity(req, resp);

            } else {
                // POST /api/opportunities/{id}/close or /{id}/complete
                String[] segments = pathInfo.substring(1).split("/");
                int opportunityId = Integer.parseInt(segments[0]);

                if (segments.length == 2 && segments[1].equals("close")) {
                    opportunityService.closeOpportunity(opportunityId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(opportunityService.getOpportunity(opportunityId)));

                } else if (segments.length == 2 && segments[1].equals("complete")) {
                    opportunityService.markCompleted(opportunityId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(opportunityService.getOpportunity(opportunityId)));

                } else {
                    writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown action.");
                }
            }

        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID in request.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            writeError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    private void createOpportunity(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {

        CreateOpportunityRequest body = readJsonBody(req, CreateOpportunityRequest.class);

        Date eventDate = Date.valueOf(body.eventDate);
        Time startTime = body.startTime != null ? Time.valueOf(body.startTime) : null;
        Time endTime = body.endTime != null ? Time.valueOf(body.endTime) : null;

        VolunteerOpportunity created = opportunityService.createOpportunity(
                body.institutionId, body.title, body.description, body.requiredSkills,
                eventDate, startTime, endTime, body.location, body.maxVolunteers);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(gson.toJson(created));
    }

    /**
     * Extracts numeric ID(s) from the start of a path like "/2/close" -> [2].
     * expectedCount is how many leading numeric segments to parse (usually 1).
     */
    private int[] parseIdFromPath(String pathInfo, int expectedCount) {
        String[] segments = pathInfo.substring(1).split("/");
        int[] ids = new int[expectedCount];
        for (int i = 0; i < expectedCount; i++) {
            ids[i] = Integer.parseInt(segments[i]);
        }
        return ids;
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

        ErrorResponse(String error) {
            this.error = error;
        }
    }
}
