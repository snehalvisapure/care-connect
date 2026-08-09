package com.careconnect.controller;

import com.careconnect.model.VolunteerRegistration;
import com.careconnect.service.VolunteerRegistrationService;
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
import java.util.Collections;
import java.util.List;

/**
 * Controller (Servlet) for volunteer registration (application) endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in VolunteerRegistrationService.
 *
 * Endpoints:
 *   GET    /api/registrations?opportunityId={id}  -> list registrations for an opportunity
 *   GET    /api/registrations?volunteerId={id}     -> list registrations for a volunteer
 *   POST   /api/registrations                       -> apply to an opportunity
 *   POST   /api/registrations/{id}/accept            -> accept an application
 *   POST   /api/registrations/{id}/reject            -> reject an application
 *   POST   /api/registrations/{id}/complete          -> mark participation completed
 *   DELETE /api/registrations/{id}                    -> withdraw an application
 */
@WebServlet("/api/registrations/*")
public class VolunteerRegistrationController extends HttpServlet {

    private final VolunteerRegistrationService registrationService = new VolunteerRegistrationService();
    private final Gson gson = new Gson();

    /**
     * DTO matching the JSON body expected on POST /api/registrations (apply).
     */
    private static class ApplyRequest {
        int volunteerId;
        int opportunityId;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String opportunityIdParam = req.getParameter("opportunityId");
            String volunteerIdParam = req.getParameter("volunteerId");

            List<VolunteerRegistration> registrations;

            if (opportunityIdParam != null) {
                registrations = registrationService.listByOpportunity(Integer.parseInt(opportunityIdParam));
            } else if (volunteerIdParam != null) {
                registrations = registrationService.listByVolunteer(Integer.parseInt(volunteerIdParam));
            } else {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Provide either ?opportunityId= or ?volunteerId= as a query parameter.");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(registrations));

        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID in query parameter.");
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
                // POST /api/registrations -> apply
                ApplyRequest body = readJsonBody(req, ApplyRequest.class);
                VolunteerRegistration created = registrationService.applyToOpportunity(
                        body.volunteerId, body.opportunityId);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write(gson.toJson(created));

            } else {
                // POST /api/registrations/{id}/accept | reject | complete
                String[] segments = pathInfo.substring(1).split("/");
                int registrationId = Integer.parseInt(segments[0]);

                if (segments.length != 2) {
                    writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown action.");
                    return;
                }

                switch (segments[1]) {
                    case "accept":
                        registrationService.acceptRegistration(registrationId);
                        break;
                    case "reject":
                        registrationService.rejectRegistration(registrationId);
                        break;
                    case "complete":
                        registrationService.markCompleted(registrationId);
                        break;
                    default:
                        writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown action.");
                        return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(Collections.singletonMap("status", "success")));
            }

        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID in request.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            // e.g. duplicate application, capacity reached, wrong current status
            writeError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Registration ID required in URL.");
                return;
            }

            int registrationId = Integer.parseInt(pathInfo.substring(1));
            registrationService.withdrawApplication(registrationId);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

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
