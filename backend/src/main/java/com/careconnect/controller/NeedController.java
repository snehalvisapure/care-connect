package com.careconnect.controller;

import com.careconnect.model.Need;
import com.careconnect.model.NeedUrgency;
import com.careconnect.service.NeedService;
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
 * Controller (Servlet) for institution needs/requirements endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in NeedService.
 *
 * Endpoints:
 *   GET    /api/needs?institutionId={id}   -> list needs for an institution
 *   GET    /api/needs/{id}                 -> get one need
 *   POST   /api/needs                      -> post a new need (starts OPEN, quantity_received=0)
 *   PUT    /api/needs/{id}                 -> update editable fields (category, item, quantity, urgency, description)
 *   DELETE /api/needs/{id}                 -> delete a need
 *
 * Note: quantity_received/status are NOT editable through this controller -
 * they only ever change via a donation being marked delivered/confirmed
 * (see ItemDonationController.markDelivered(), which calls
 * NeedService.recordQuantityReceived()). This keeps a single source of
 * truth for fulfillment progress.
 *
 * Note: Search/filter across needs (category, urgency, status) lives in
 * SearchController (/api/search/needs) - Member 3's module. This controller
 * intentionally does not duplicate that.
 */
@WebServlet("/api/needs/*")
public class NeedController extends HttpServlet {

    private final NeedService needService = new NeedService();
    private final Gson gson = new Gson();

    private static class CreateNeedRequest {
        int institutionId;
        String category;
        String itemName;
        int quantityRequired;
        NeedUrgency urgency;
        String description;
    }

    private static class UpdateNeedRequest {
        String category;
        String itemName;
        int quantityRequired;
        NeedUrgency urgency;
        String description;
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
                if (institutionIdParam == null) {
                    writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "institutionId query parameter is required.");
                    return;
                }
                // GET /api/needs?institutionId=1
                List<Need> needs = needService.getNeedsForInstitution(Integer.parseInt(institutionIdParam));
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(needs));

            } else {
                // GET /api/needs/{id}
                int needId = Integer.parseInt(pathInfo.substring(1));
                Need need = needService.getNeed(needId);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(need));
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

        try {
            CreateNeedRequest body = readJsonBody(req, CreateNeedRequest.class);

            Need created = needService.createNeed(body.institutionId, body.category, body.itemName,
                    body.quantityRequired, body.urgency, body.description);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(gson.toJson(created));

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
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Need ID is required in the URL.");
                return;
            }

            int needId = Integer.parseInt(pathInfo.substring(1));
            UpdateNeedRequest body = readJsonBody(req, UpdateNeedRequest.class);

            needService.updateNeed(needId, body.category, body.itemName,
                    body.quantityRequired, body.urgency, body.description);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(needService.getNeed(needId)));

        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
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
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Need ID is required in the URL.");
                return;
            }

            int needId = Integer.parseInt(pathInfo.substring(1));
            needService.deleteNeed(needId);
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
