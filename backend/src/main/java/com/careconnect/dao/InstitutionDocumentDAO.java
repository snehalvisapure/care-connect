package com.careconnect.dao;

import com.careconnect.model.InstitutionDocument;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the `institution_documents` table. Core CRUD only.
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 */
public class InstitutionDocumentDAO {

    /**
     * Inserts a new verification document record.
     * Expects document.getInstitutionId(), getDocumentType(), getDocumentPath()
     * to be set. On success, sets the generated document_id back onto the
     * passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     */
    public boolean insert(InstitutionDocument document) throws SQLException {
        String sql = "INSERT INTO institution_documents " +
                "(institution_id, document_type, document_path) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, document.getInstitutionId());
            stmt.setString(2, document.getDocumentType());
            stmt.setString(3, document.getDocumentPath());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        document.setDocumentId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds a single document by its document_id.
     *
     * @return the InstitutionDocument, or null if no row matches.
     */
    public InstitutionDocument findById(int documentId) throws SQLException {
        String sql = "SELECT * FROM institution_documents WHERE document_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, documentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDocument(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns every document uploaded for a given institution, most
     * recently uploaded first.
     */
    public List<InstitutionDocument> findByInstitutionId(int institutionId) throws SQLException {
        String sql = "SELECT * FROM institution_documents WHERE institution_id = ? " +
                "ORDER BY uploaded_at DESC";

        List<InstitutionDocument> documents = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, institutionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapRowToDocument(rs));
                }
            }
        }
        return documents;
    }

    /**
     * Deletes a document by document_id.
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int documentId) throws SQLException {
        String sql = "DELETE FROM institution_documents WHERE document_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, documentId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Maps the current row of a ResultSet to an InstitutionDocument object.
     */
    private InstitutionDocument mapRowToDocument(ResultSet rs) throws SQLException {
        InstitutionDocument document = new InstitutionDocument();
        document.setDocumentId(rs.getInt("document_id"));
        document.setInstitutionId(rs.getInt("institution_id"));
        document.setDocumentType(rs.getString("document_type"));
        document.setDocumentPath(rs.getString("document_path"));

        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        document.setUploadedAt(uploadedAt);

        return document;
    }
}
