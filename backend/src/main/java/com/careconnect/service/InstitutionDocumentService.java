package com.careconnect.service;

import com.careconnect.dao.InstitutionDAO;
import com.careconnect.dao.InstitutionDocumentDAO;
import com.careconnect.model.InstitutionDocument;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for institution verification documents.
 * Sits between the Controller and the DAO - the Controller should call
 * this class, never InstitutionDocumentDAO directly.
 */
public class InstitutionDocumentService {

    private final InstitutionDocumentDAO documentDAO;
    private final InstitutionDAO institutionDAO;

    public InstitutionDocumentService() {
        this.documentDAO = new InstitutionDocumentDAO();
        this.institutionDAO = new InstitutionDAO();
    }

    /**
     * Uploads a new verification document for an institution.
     * Business rules:
     * 1. The institution must actually exist.
     * 2. documentType and documentPath can't be blank - a document record
     *    with no file behind it is useless to an admin reviewing it.
     *
     * Note: this class only stores the path/reference to the uploaded file
     * (e.g. wherever FileStorageUtil or the controller layer saved it) -
     * it doesn't handle the actual file upload/storage itself.
     *
     * @throws IllegalArgumentException if the institution doesn't exist,
     *         or documentType/documentPath is blank.
     */
    public InstitutionDocument uploadDocument(int institutionId, String documentType,
                                               String documentPath) throws SQLException {

        if (institutionDAO.findById(institutionId) == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
        if (documentType == null || documentType.isBlank()) {
            throw new IllegalArgumentException("documentType cannot be blank");
        }
        if (documentPath == null || documentPath.isBlank()) {
            throw new IllegalArgumentException("documentPath cannot be blank");
        }

        InstitutionDocument document = new InstitutionDocument(institutionId, documentType, documentPath);
        documentDAO.insert(document);
        return document;
    }

    /**
     * Fetches a single document by document_id.
     *
     * @throws IllegalArgumentException if no such document exists.
     */
    public InstitutionDocument getDocument(int documentId) throws SQLException {
        InstitutionDocument document = documentDAO.findById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("No document found with document_id=" + documentId);
        }
        return document;
    }

    /**
     * Returns every document uploaded for an institution (e.g. for an admin
     * reviewing a registration, or the institution viewing its own uploads).
     *
     * @throws IllegalArgumentException if the institution doesn't exist.
     */
    public List<InstitutionDocument> getDocumentsForInstitution(int institutionId) throws SQLException {
        if (institutionDAO.findById(institutionId) == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
        return documentDAO.findByInstitutionId(institutionId);
    }

    /**
     * Deletes a document, but only if it actually belongs to the institution
     * making the request. This is the "if appropriate" check - it stops one
     * institution from deleting another institution's uploaded documents.
     *
     * @throws IllegalArgumentException if no such document exists.
     * @throws IllegalStateException if the document belongs to a different institution.
     */
    public void deleteDocument(int documentId, int requestingInstitutionId) throws SQLException {
        InstitutionDocument document = documentDAO.findById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("No document found with document_id=" + documentId);
        }
        if (document.getInstitutionId() != requestingInstitutionId) {
            throw new IllegalStateException(
                    "Document " + documentId + " does not belong to institution " + requestingInstitutionId);
        }

        documentDAO.delete(documentId);
    }
}
