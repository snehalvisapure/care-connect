package com.careconnect.dao;

import com.careconnect.model.Need;
import com.careconnect.model.NeedStatus;
import com.careconnect.model.NeedUrgency;

import java.sql.SQLException;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm NeedDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes institution_id = 1 (from InstitutionDAOTest) already exists.
 */
public class NeedDAOTest {

    public static void main(String[] args) {
        NeedDAO dao = new NeedDAO();

        try {
            // 1. Insert a new need for institution 1 - starts OPEN, 0 received
            Need newNeed = new Need();
            newNeed.setInstitutionId(1);
            newNeed.setCategory("Groceries");
            newNeed.setItemName("Rice (25kg bags)");
            newNeed.setQuantityRequired(20);
            newNeed.setQuantityReceived(0);
            newNeed.setUrgency(NeedUrgency.HIGH);
            newNeed.setDescription("Monthly rice stock for the kitchen.");
            newNeed.setStatus(NeedStatus.OPEN);

            boolean inserted = dao.insert(newNeed);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated need_id: " + newNeed.getNeedId());

            // 2. Read it back by ID to confirm it round-tripped correctly
            Need fetched = dao.findById(newNeed.getNeedId());
            System.out.println("Fetched back: " + fetched);

            // 3. List all needs for institution 1
            List<Need> needsForInstitution = dao.findByInstitutionId(1);
            System.out.println("Total needs for institution_id=1: " + needsForInstitution.size());
            for (Need n : needsForInstitution) {
                System.out.println("  " + n);
            }

            // 4. Simulate a partial donation arriving (raw DAO call - NeedService
            //    is what actually recalculates status in real usage, this just
            //    confirms the DAO-level update works)
            boolean quantityUpdated = dao.updateQuantityReceived(newNeed.getNeedId(), 8);
            boolean statusUpdated = dao.updateStatus(newNeed.getNeedId(), NeedStatus.PARTIALLY_FULFILLED);
            System.out.println("Quantity update successful: " + quantityUpdated);
            System.out.println("Status update successful: " + statusUpdated);

            Need afterPartial = dao.findById(newNeed.getNeedId());
            System.out.println("After partial donation: " + afterPartial);

            // 5. Update the editable fields (category/item/urgency/description)
            afterPartial.setDescription("Updated: still need the remaining bags urgently.");
            boolean editUpdated = dao.update(afterPartial);
            System.out.println("Edit update successful: " + editUpdated);

            // 6. Delete the need, confirm the count drops
            boolean deleted = dao.delete(newNeed.getNeedId());
            System.out.println("Delete successful: " + deleted);

            List<Need> afterDelete = dao.findByInstitutionId(1);
            System.out.println("Total needs for institution_id=1 after delete: " + afterDelete.size());

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}
