package com.careconnect.payment;

import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection helper for the payment module. It reuses the project's own DBConnection,
 * so database settings live only in src/main/resources/db.properties.
 */
final class PaymentDb {

    private PaymentDb() {}

    static Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }
}