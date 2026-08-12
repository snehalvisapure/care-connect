package com.careconnect.controller;

import com.careconnect.model.ItemDonation;
import com.careconnect.model.PickupOrDrop;
import com.careconnect.service.ItemDonationService;
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
import java.util.List;

/**
 * Controller (Servlet) for in-kind (item) donation endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in ItemDonationService.
 *
 * Endpoints:
 *   GET  /api/item-donations?donorId={id}         -> item donations made by a donor
 *   GET  /api/item-donations?institutionId={id}    -> item donations received by an institution
 *   GET  /api/item-donations/{id}                   -> get one item donation
 *   POST /api/item-donations                         -> create a new item donation
 *   POST /api/item-donations/{id}/pickup             -> mark PICKUP-type donation as collected
 *   POST /api/item-donations/{id}/deliver             -> mark delivered (advances the linked need, if any)
 *   POST /api/item-donations/{id}/cancel              -> cancel before delivery
 *
 * Marking a donation DELIVERED is what actually moves a linked need's
 * quantity_received/status forward - see ItemDonationService.markDelivered().
 */
@WebServlet("/api/item-donations/*")
public class ItemDonationController extends HttpServlet {

    private final ItemDonationService itemDonationService = new ItemDonationService();
    private final Gson gson = new Gson();

    private static class CreateItemDonationRequest {
        int donorId;
        int institutionId;
        Integer needId;
        String itemName;
        int quantity;
        PickupOrDrop pickupOrDrop;
        String pickupAddress;
    }

    private static class DeliverRequest {
        String deliveryDate; // "yyyy-MM-dd", optional - defaults to today
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String donorIdParam = req.getParameter("donorId");
                String institutionIdParam = req.getParameter("institutionId");

                List<ItemDonation> itemDonations;
                if (donorIdParam != null) {
                    itemDonations = itemDonationService.getItemDonationsForDonor(Integer.parseInt(donorIdParam));
                } else if (institutionIdParam != null) {
                    itemDonations = itemDonationService.getItemDonationsForInstitution(
                            Integer.parseInt(institutionIdParam));
                } else {
                    writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Either donorId or institutionId query parameter is required.");
                    return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(itemDonations));

            } else {
                // GET /api/item-donations/{id}
                int itemDonationId = Integer.parseInt(pathInfo.substring(1));
                ItemDonation itemDonation = itemDonationService.getItemDonation(itemDonationId);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(itemDonation));
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
                // POST /api/item-donations -> create
                createItemDonation(req, resp);

            } else {
                String[] segments = pathInfo.substring(1).split("/");
                int itemDonationId = Integer.parseInt(segments[0]);

                if (segments.length == 2 && segments[1].equals("pickup")) {
                    ItemDonation updated = itemDonationService.markPickedUp(itemDonationId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(updated));

                } else if (segments.length == 2 && segments[1].equals("deliver")) {
                    DeliverRequest body = readJsonBody(req, DeliverRequest.class);
                    Date deliveryDate = (body != null && body.deliveryDate != null)
                            ? Date.valueOf(body.deliveryDate)
                            : null;
                    ItemDonation updated = itemDonationService.markDelivered(itemDonationId, deliveryDate);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(updated));

                } else if (segments.length == 2 && segments[1].equals("cancel")) {
                    ItemDonation updated = itemDonationService.cancelItemDonation(itemDonationId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(updated));

                } else {
                    writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown action.");
                }
            }

        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID or date in request.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            writeError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    private void createItemDonation(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {

        CreateItemDonationRequest body = readJsonBody(req, CreateItemDonationRequest.class);

        ItemDonation created = itemDonationService.createItemDonation(
                body.donorId, body.institutionId, body.needId, body.itemName,
                body.quantity, body.pickupOrDrop, body.pickupAddress);

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
