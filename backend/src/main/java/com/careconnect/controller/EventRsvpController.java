package com.careconnect.controller;

import com.careconnect.model.EventRsvp;
import com.careconnect.model.RsvpResponse;
import com.careconnect.service.EventRsvpService;
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
 * Controller (Servlet) for event RSVP endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic (including the upsert behaviour) lives in EventRsvpService.
 *
 * Endpoints:
 *   GET    /api/rsvps?eventId={id}              -> list RSVPs for an event
 *   GET    /api/rsvps?userId={id}                -> list RSVPs made by a user
 *   GET    /api/rsvps?eventId={id}&userId={id}     -> a specific user's RSVP status for an event
 *   GET    /api/rsvps/count?eventId={id}             -> count of GOING responses for an event
 *   POST   /api/rsvps                                  -> set/change an RSVP (upsert: {eventId, userId, response})
 *   DELETE /api/rsvps?eventId={id}&userId={id}           -> remove an RSVP entirely
 */
@WebServlet("/api/rsvps/*")
public class EventRsvpController extends HttpServlet {

    private final EventRsvpService rsvpService = new EventRsvpService();
    private final Gson gson = new Gson();

    /**
     * DTO matching the JSON body expected on POST /api/rsvps.
     */
    private static class RsvpRequest {
        int eventId;
        int userId;
        String response; // matches RsvpResponse enum name, e.g. "GOING"
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.equals("/count")) {
                // GET /api/rsvps/count?eventId=1
                String eventIdParam = req.getParameter("eventId");
                if (eventIdParam == null) {
                    writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "eventId query parameter is required.");
                    return;
                }
                long count = rsvpService.countGoing(Integer.parseInt(eventIdParam));
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(Collections.singletonMap("going", count)));
                return;
            }

            String eventIdParam = req.getParameter("eventId");
            String userIdParam = req.getParameter("userId");

            if (eventIdParam != null && userIdParam != null) {
                // GET /api/rsvps?eventId=1&userId=1 -> specific status (or null if none)
                EventRsvp status = rsvpService.getRsvpStatus(
                        Integer.parseInt(eventIdParam), Integer.parseInt(userIdParam));
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(status));

            } else if (eventIdParam != null) {
                // GET /api/rsvps?eventId=1 -> all RSVPs for that event
                List<EventRsvp> rsvps = rsvpService.listByEvent(Integer.parseInt(eventIdParam));
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(rsvps));

            } else if (userIdParam != null) {
                // GET /api/rsvps?userId=1 -> all RSVPs made by that user
                List<EventRsvp> rsvps = rsvpService.listByUser(Integer.parseInt(userIdParam));
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(rsvps));

            } else {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Provide eventId and/or userId as query parameters.");
            }

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

        try {
            RsvpRequest body = readJsonBody(req, RsvpRequest.class);
            RsvpResponse response = RsvpResponse.valueOf(body.response);

            EventRsvp result = rsvpService.rsvp(body.eventId, body.userId, response);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(result));

        } catch (JsonSyntaxException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON in request body.");
        } catch (IllegalArgumentException e) {
            // covers both "event not found" and an invalid response enum value
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

        try {
            String eventIdParam = req.getParameter("eventId");
            String userIdParam = req.getParameter("userId");

            if (eventIdParam == null || userIdParam == null) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Both eventId and userId query parameters are required.");
                return;
            }

            rsvpService.removeRsvp(Integer.parseInt(eventIdParam), Integer.parseInt(userIdParam));
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID in query parameter.");
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
