package com.careconnect.controller;

import com.careconnect.model.Institution;
import com.careconnect.model.InstitutionType;
import com.careconnect.model.Need;
import com.careconnect.model.NeedStatus;
import com.careconnect.model.NeedUrgency;
import com.careconnect.model.VerificationStatus;
import com.careconnect.service.SearchService;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller (Servlet) for Search & Filter endpoints (Module 7).
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in SearchService.
 *
 * Endpoints:
 *   GET /api/search/institutions?city=&type=&verificationStatus=&includeUnapproved=
 *   GET /api/search/needs?category=&urgency=&institutionId=&status=
 *
 * All query parameters are optional - omit any filter you don't want applied.
 * By default, institution search only returns APPROVED institutions
 * (public/discovery-first browsing). Pass includeUnapproved=true to see all
 * (e.g. for an admin view).
 */
@WebServlet("/api/search/*")
public class SearchController extends HttpServlet {

    private final SearchService searchService = new SearchService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if ("/institutions".equals(pathInfo)) {
                searchInstitutions(req, resp);
            } else if ("/needs".equals(pathInfo)) {
                searchNeeds(req, resp);
            } else {
                writeError(resp, HttpServletResponse.SC_NOT_FOUND,
                        "Unknown search endpoint. Use /api/search/institutions or /api/search/needs.");
            }

        } catch (IllegalArgumentException e) {
            // covers invalid enum values passed as query params
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid filter value: " + e.getMessage());
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            e.printStackTrace();
        }
    }

    private void searchInstitutions(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {

        String city = req.getParameter("city");

        String typeParam = req.getParameter("type");
        InstitutionType institutionType = typeParam != null ? InstitutionType.valueOf(typeParam) : null;

        String statusParam = req.getParameter("verificationStatus");
        VerificationStatus verificationStatus = statusParam != null
                ? VerificationStatus.valueOf(statusParam) : null;

        boolean includeUnapproved = "true".equalsIgnoreCase(req.getParameter("includeUnapproved"));

        List<Institution> results = searchService.searchInstitutions(
                city, institutionType, verificationStatus, includeUnapproved);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(results));
    }

    private void searchNeeds(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {

        String category = req.getParameter("category");

        String urgencyParam = req.getParameter("urgency");
        NeedUrgency urgency = urgencyParam != null ? NeedUrgency.valueOf(urgencyParam) : null;

        String institutionIdParam = req.getParameter("institutionId");
        Integer institutionId = institutionIdParam != null ? Integer.parseInt(institutionIdParam) : null;

        String statusParam = req.getParameter("status");
        NeedStatus status = statusParam != null ? NeedStatus.valueOf(statusParam) : null;

        List<Need> results = searchService.searchNeeds(category, urgency, institutionId, status);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(results));
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
