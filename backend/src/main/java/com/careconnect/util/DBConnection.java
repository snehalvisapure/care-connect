package com.careconnect.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * PLACEHOLDER JDBC connection utility.
 * Created by Member 3 (Nehaa) on 2026-08-07 because the official backend
 * foundation had not been set up yet by Member 1. If/when Member 1 delivers
 * an official version of this class (or equivalent), reconcile the two -
 * DAO classes just need a working getConnection() method with this signature,
 * so swapping this out later should be a drop-in replacement.
 *
 * Reads connection details from src/main/resources/db.properties so
 * credentials aren't hardcoded in source.
 */
public class DBConnection {

    private static final String CONFIG_FILE = "db.properties";

    private static String url;
    private static String username;
    private static String password;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Unable to find " + CONFIG_FILE + " on classpath. " +
                        "Make sure it exists in src/main/resources/");
            }
            props.load(input);
            url = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + CONFIG_FILE, e);
        }
    }

    /**
     * Returns a new JDBC Connection to the careconnect database.
     * Caller is responsible for closing it (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // Quick manual test - run this class directly to confirm the connection works.
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Successfully connected to careconnect database.");
            }
        } catch (SQLException e) {
            System.out.println("Connection failed.");
            e.printStackTrace();
        }
    }
}
