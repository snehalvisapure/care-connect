package com.careconnect.controller;

import com.careconnect.model.InstitutionDocument;
import com.careconnect.service.InstitutionDocumentService;
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
 * Controller (Servlet) for institution verification document endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in InstitutionDocumentService.
 *
 * Endpoints:
 *   GET    /api/documents?institutionId={id}                 -> list documents for an institution
 *   GET    /api/documents/{id}                                -> get one document
 *   POST   /api/documents                                     -> upload a new document
 *   DELETE /api/documents/{id}?institutionId={requestingId}   -> delete (ownership-checked)
 */
@WebServlet("/api/documents/*")
public class InstitutionDocumentController extends HttpServlet {

    private final InstitutionDocumentService documentService = new InstitutionDocumentService();
    private final Gson gson = new Gson();

    private static class UploadDocumentRequest {
        int institutionId;
        String documentType;
        String documentPath;
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
                // GET /api/documents?institutionId=1
                List<InstitutionDocument> documents =
                        documentService.getDocumentsForInstitution(Integer.parseInt(institutionIdParam));
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(documents));

            } else {
                // GET /api/documents/{id}
                int documentId = Integer.parseInt(pathInfo.substring(1));
                InstitutionDocument document = documentService.getDocument(documentId);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(document));
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
            UploadDocumentRequest body = readJsonBody(req, UploadDocumentRequest.class);

            InstitutionDocument created = documentService.uploadDocument(
                    body.institutionId, body.documentType, body.documentPath);

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
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Document ID is required in the URL.");
                return;
            }

            String institutionIdParam = req.getParameter("institutionId");
            if (institutionIdParam == null) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "institutionId query parameter is required to verify ownership.");
                return;
            }

            int documentId = Integer.parseInt(pathInfo.substring(1));
            int institutionId = Integer.parseInt(institutionIdParam);

            documentService.deleteDocument(documentId, institutionId);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID in request.");
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            writeError(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
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
