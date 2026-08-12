package com.careconnect.controller;

import com.careconnect.model.Donation;
import com.careconnect.service.DonationService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller (Servlet) for monetary donation / pledge endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in DonationService.
 *
 * There's no separate "pledge" resource in the schema - a pledge IS a
 * donation row with payment_status = PENDING, so pledges are just donations
 * filtered by that status (see /api/donations/pending below).
 *
 * Endpoints:
 *   GET  /api/donations?donorId={id}              -> full donation history for a donor
 *   GET  /api/donations?institutionId={id}         -> full donation history for an institution
 *   GET  /api/donations/pending?donorId={id}        -> a donor's pending pledges only
 *   GET  /api/donations/pending?institutionId={id}  -> an institution's pending pledges only
 *   GET  /api/donations/{id}                        -> get one donation
 *   POST /api/donations                              -> create a monetary pledge/donation
 *   POST /api/donations/{id}/confirm                 -> confirm payment (pledge -> completed)
 *   POST /api/donations/{id}/cancel                  -> cancel a pending pledge
 *
 * Note: in-kind (item) donations are a separate resource/table
 * (item_donations) with their own delivery lifecycle - see
 * ItemDonationController (/api/item-donations).
 */
@WebServlet("/api/donations/*")
public class DonationController extends HttpServlet {

    private final DonationService donationService = new DonationService();
    private final Gson gson = new Gson();

    private static class CreateDonationRequest {
        int donorId;
        int institutionId;
        Integer needId;
        BigDecimal amount;
    }

    private static class ConfirmPaymentRequest {
        String transactionId;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleListDonations(req, resp, false);

            } else if (pathInfo.equals("/pending")) {
                handleListDonations(req, resp, true);

            } else {
                // GET /api/donations/{id}
                int donationId = Integer.parseInt(pathInfo.substring(1));
                Donation donation = donationService.getDonation(donationId);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(donation));
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

    private void handleListDonations(HttpServletRequest req, HttpServletResponse resp, boolean pendingOnly)
            throws IOException, SQLException {

        String donorIdParam = req.getParameter("donorId");
        String institutionIdParam = req.getParameter("institutionId");

        List<Donation> donations;

        if (donorIdParam != null) {
            int donorId = Integer.parseInt(donorIdParam);
            donations = pendingOnly
                    ? donationService.getPendingPledgesForDonor(donorId)
                    : donationService.getDonationHistoryForDonor(donorId);

        } else if (institutionIdParam != null) {
            int institutionId = Integer.parseInt(institutionIdParam);
            donations = pendingOnly
                    ? donationService.getPendingPledgesForInstitution(institutionId)
                    : donationService.getDonationHistoryForInstitution(institutionId);

        } else {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Either donorId or institutionId query parameter is required.");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(donations));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // POST /api/donations -> create pledge/donation
                createDonation(req, resp);

            } else {
                String[] segments = pathInfo.substring(1).split("/");
                int donationId = Integer.parseInt(segments[0]);

                if (segments.length == 2 && segments[1].equals("confirm")) {
                    ConfirmPaymentRequest body = readJsonBody(req, ConfirmPaymentRequest.class);
                    Donation confirmed = donationService.confirmPayment(
                            donationId, body != null ? body.transactionId : null);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(confirmed));

                } else if (segments.length == 2 && segments[1].equals("cancel")) {
                    Donation cancelled = donationService.cancelPledge(donationId);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(cancelled));

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

    private void createDonation(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {

        CreateDonationRequest body = readJsonBody(req, CreateDonationRequest.class);

        Donation created = donationService.createPledge(
                body.donorId, body.institutionId, body.needId, body.amount);

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
