package com.careconnect.payment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** GET /api/payments/history : the logged-in user's payments, newest first, as JSON. */
@WebServlet("/api/payments/history")
public class PaymentHistoryServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(PaymentHistoryServlet.class.getName());

    private final PaymentDAO dao = new PaymentDAO();
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = PaymentServletSupport.currentUserId(req);
        if (userId == null) {
            PaymentServletSupport.error(resp, 401, "Not logged in.");
            return;
        }
        try {
            JsonObject out = new JsonObject();
            out.add("payments", gson.toJsonTree(dao.findByUser(userId)));
            PaymentServletSupport.writeJson(resp, 200, out);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error while loading payment history", e);
            PaymentServletSupport.error(resp, 500, "Could not load payment history.");
        }
    }
}
