package com.careconnect.dao;

import com.careconnect.model.InstitutionDocument;

import java.sql.SQLException;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm InstitutionDocumentDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes institution_id = 1 (from InstitutionDAOTest) already exists.
 */
public class InstitutionDocumentDAOTest {

    public static void main(String[] args) {
        InstitutionDocumentDAO dao = new InstitutionDocumentDAO();

        try {
            // 1. Upload a verification document for institution 1
            InstitutionDocument newDocument = new InstitutionDocument(
                    1,                                  // institution_id (must already exist)
                    "REGISTRATION_CERTIFICATE",          // document_type
                    "/uploads/institutions/1/reg_cert.pdf" // document_path
            );

            boolean inserted = dao.insert(newDocument);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated document_id: " + newDocument.getDocumentId());

            // 2. Upload a second document for the same institution
            InstitutionDocument secondDocument = new InstitutionDocument(
                    1,
                    "PAN_CARD",
                    "/uploads/institutions/1/pan_card.pdf"
            );
            dao.insert(secondDocument);
            System.out.println("Generated document_id (second): " + secondDocument.getDocumentId());

            // 3. Read the first document back by ID to confirm it round-tripped correctly
            InstitutionDocument fetched = dao.findById(newDocument.getDocumentId());
            System.out.println("Fetched back: " + fetched);

            // 4. List all documents for institution 1
            List<InstitutionDocument> documents = dao.findByInstitutionId(1);
            System.out.println("Total documents for institution_id=1: " + documents.size());
            for (InstitutionDocument d : documents) {
                System.out.println("  " + d);
            }

            // 5. Delete the second document, then confirm the count drops
            boolean deleted = dao.delete(secondDocument.getDocumentId());
            System.out.println("Delete successful: " + deleted);

            List<InstitutionDocument> afterDelete = dao.findByInstitutionId(1);
            System.out.println("Total documents for institution_id=1 after delete: " + afterDelete.size());

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}
