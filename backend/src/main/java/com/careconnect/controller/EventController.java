package com.careconnect.controller;

import com.careconnect.model.Event;
import com.careconnect.model.EventCategory;
import com.careconnect.service.EventService;
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
 * Controller (Servlet) for event endpoints.
 * Sits above the Service layer - this class handles HTTP concerns only.
 * All business logic lives in EventService.
 *
 * Endpoints:
 *   GET    /api/events                          -> list upcoming events (public browsing, default)
 *   GET    /api/events?category=FESTIVAL          -> list events filtered by category
 *   GET    /api/events?institutionId={id}          -> list all events for one institution
 *   GET    /api/events/{id}                         -> get one event
 *   POST   /api/events                               -> create a new event
 *   PUT    /api/events/{id}                            -> update an event
 *   DELETE /api/events/{id}                              -> delete an event
 */
@WebServlet("/api/events/*")
public class EventController extends HttpServlet {

    private final EventService eventService = new EventService();
    private final Gson gson = new Gson();

    /**
     * DTO matching the JSON body expected on POST/PUT.
     * Date/time come in as ISO strings since Gson can't map JSON strings
     * directly to java.sql.Date/Time.
     */
    private static class EventRequest {
        int institutionId;
        String title;
        String description;
        String eventDate;  // "yyyy-MM-dd"
        String eventTime;  // "HH:mm:ss", nullable
        String location;
        String category;   // matches EventCategory enum name, e.g. "FESTIVAL"
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String categoryParam = req.getParameter("category");
                String institutionIdParam = req.getParameter("institutionId");

                List<Event> events;
                if (categoryParam != null) {
                    events = eventService.listByCategory(EventCategory.valueOf(categoryParam));
                } else if (institutionIdParam != null) {
                    events = eventService.listByInstitution(Integer.parseInt(institutionIdParam));
                } else {
                    events = eventService.listUpcomingEvents();
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(events));

            } else {
                int eventId = Integer.parseInt(pathInfo.substring(1));
                Event event = eventService.getEvent(eventId);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(event));
            }

        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID in request.");
        } catch (IllegalArgumentException e) {
            // covers both "event not found" and an invalid category enum value
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
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
            EventRequest body = readJsonBody(req, EventRequest.class);

            Date eventDate = Date.valueOf(body.eventDate);
            Time eventTime = body.eventTime != null ? Time.valueOf(body.eventTime) : null;
            EventCategory category = body.category != null
                    ? EventCategory.valueOf(body.category) : EventCategory.OTHER;

            Event created = eventService.createEvent(
                    body.institutionId, body.title, body.description,
                    eventDate, eventTime, body.location, category);

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
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Event ID required in URL.");
                return;
            }

            int eventId = Integer.parseInt(pathInfo.substring(1));
            EventRequest body = readJsonBody(req, EventRequest.class);

            Date eventDate = Date.valueOf(body.eventDate);
            Time eventTime = body.eventTime != null ? Time.valueOf(body.eventTime) : null;
            EventCategory category = body.category != null
                    ? EventCategory.valueOf(body.category) : EventCategory.OTHER;

            eventService.updateEvent(eventId, body.title, body.description,
                    eventDate, eventTime, body.location, category);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(eventService.getEvent(eventId)));

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
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Event ID required in URL.");
                return;
            }

            int eventId = Integer.parseInt(pathInfo.substring(1));
            eventService.deleteEvent(eventId);
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
