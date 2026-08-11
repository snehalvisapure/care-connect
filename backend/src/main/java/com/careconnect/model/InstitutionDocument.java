package com.careconnect.model;

import java.sql.Timestamp;

/**
 * Represents a row in the `institution_documents` table.
 *
 * A verification document belongs to one Institution through institution_id
 * (e.g. registration certificate, PAN card, address proof). An admin reviews
 * these documents when deciding whether to approve or reject an institution
 * (see InstitutionService.approveInstitution / rejectInstitution).
 *
 * document_type is free text (varchar(100) in the schema, not an ENUM), so
 * it's kept as a plain String here - the frontend/service layer is
 * responsible for guiding the user toward consistent values.
 */
public class InstitutionDocument {

    private int documentId;
    private int institutionId;
    private String documentType;
    private String documentPath;
    private Timestamp uploadedAt;

    public InstitutionDocument() {
    }

    /**
     * Used when reading a complete document record from the database.
     */
    public InstitutionDocument(int documentId, int institutionId, String documentType,
                                String documentPath, Timestamp uploadedAt) {
        this.documentId = documentId;
        this.institutionId = institutionId;
        this.documentType = documentType;
        this.documentPath = documentPath;
        this.uploadedAt = uploadedAt;
    }

    /**
     * Used when uploading a new document. documentId and uploadedAt are
     * assigned by the DB.
     */
    public InstitutionDocument(int institutionId, String documentType, String documentPath) {
        this.institutionId = institutionId;
        this.documentType = documentType;
        this.documentPath = documentPath;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(int institutionId) {
        this.institutionId = institutionId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "InstitutionDocument{" +
                "documentId=" + documentId +
                ", institutionId=" + institutionId +
                ", documentType='" + documentType + '\'' +
                ", documentPath='" + documentPath + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
