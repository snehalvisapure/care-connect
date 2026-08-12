package com.careconnect.controller;

import com.careconnect.model.Institution;
import com.careconnect.model.InstitutionDashboard;
import com.careconnect.model.InstitutionType;
import com.careconnect.service.DashboardService;
import com.careconnect.service.InstitutionService;
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
 * Controller (Servlet) for institution registration/profile endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in InstitutionService / DashboardService.
 *
 * Endpoints:
 *   GET    /api/institutions                      -> list all institutions
 *   GET    /api/institutions?approvedOnly=true     -> list only APPROVED institutions
 *   GET    /api/institutions?userId={id}           -> the institution registered by this user (or null)
 *   GET    /api/institutions/{id}                  -> get one institution
 *   GET    /api/institutions/{id}/dashboard        -> aggregated dashboard summary
 *   POST   /api/institutions                       -> register a new institution
 *   POST   /api/institutions/{id}/approve          -> admin approves a PENDING institution
 *   POST   /api/institutions/{id}/reject           -> admin rejects a PENDING institution
 *   PUT    /api/institutions/{id}                  -> update editable profile fields
 *   DELETE /api/institutions/{id}                  -> delete an institution
 *
 * Note: Search/filter across institutions (city, type, verificationStatus)
 * lives in SearchController (/api/search/institutions) - Member 3's module.
 * This controller intentionally does not duplicate that.
 */
@WebServlet("/api/institutions/*")
public class InstitutionController extends HttpServlet {

    private final InstitutionService institutionService = new InstitutionService();
    private final DashboardService dashboardService = new DashboardService();
    private final Gson gson = new Gson();

    private static class RegisterInstitutionRequest {
        int userId;
        String institutionName;
        InstitutionType institutionType;
        String registrationNumber;
        String description;
        String address;
        String city;
        String state;
        String pincode;
        String contactPerson;
    }

    private static class UpdateInstitutionRequest {
        String institutionName;
        InstitutionType institutionType;
        String registrationNumber;
        String description;
        String address;
        String city;
        String state;
        String pincode;
        String contactPerson;
    }

    private static class RejectInstitutionRequest {
        String rejectionReason;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String userIdParam = req.getParameter("userId");
                String approvedOnlyParam = req.getParameter("approvedOnly");

                if (userIdParam != null) {
                    // GET /api/institutions?userId=1
                    Institution institution = institutionService.getInstitutionByUserId(
                            Integer.parseInt(userIdParam));
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(institution));

                } else if ("true".equalsIgnoreCase(approvedOnlyParam)) {
                    // GET /api/institutions?approvedOnly=true
                    List<Institution> institutions = institutionService.listApprovedInstitutions();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(institutions));

                } else {
                    // GET /api/institutions
                    List<Institution> institutions = institutionService.listAllInstitutions();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(institutions));
                }

            } else {
                String[] segments = pathInfo.substring(1).split("/");
                int institutionId = Integer.parseInt(segments[0]);

                if (segments.length == 1) {
                    // GET /api/institutions/{id}
                    Institution institution = institutionService.getInstitution(institutionId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(institution));

                } else if (segments.length == 2 && segments[1].equals("dashboard")) {
                    // GET /api/institutions/{id}/dashboard
                    InstitutionDashboard dashboard = dashboardService.getDashboard(institutionId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(dashboard));

                } else {
                    writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint.");
                }
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
                // POST /api/institutions -> register
                registerInstitution(req, resp);

            } else {
                String[] segments = pathInfo.substring(1).split("/");
                int institutionId = Integer.parseInt(segments[0]);

                if (segments.length == 2 && segments[1].equals("approve")) {
                    institutionService.approveInstitution(institutionId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(institutionService.getInstitution(institutionId)));

                } else if (segments.length == 2 && segments[1].equals("reject")) {
                    RejectInstitutionRequest body = readJsonBody(req, RejectInstitutionRequest.class);
                    institutionService.rejectInstitution(institutionId, body != null ? body.rejectionReason : null);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(institutionService.getInstitution(institutionId)));

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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Institution ID is required in the URL.");
                return;
            }

            int institutionId = Integer.parseInt(pathInfo.substring(1));
            UpdateInstitutionRequest body = readJsonBody(req, UpdateInstitutionRequest.class);

            institutionService.updateProfile(institutionId, body.institutionName, body.institutionType,
                    body.registrationNumber, body.description, body.address, body.city,
                    body.state, body.pincode, body.contactPerson);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(institutionService.getInstitution(institutionId)));

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

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Institution ID is required in the URL.");
                return;
            }

            int institutionId = Integer.parseInt(pathInfo.substring(1));
            institutionService.deleteInstitution(institutionId);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID in request.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    private void registerInstitution(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {

        RegisterInstitutionRequest body = readJsonBody(req, RegisterInstitutionRequest.class);

        Institution created = institutionService.registerInstitution(body.userId, body.institutionName,
                body.institutionType, body.registrationNumber, body.description, body.address,
                body.city, body.state, body.pincode, body.contactPerson);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(gson.toJson(created));
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
