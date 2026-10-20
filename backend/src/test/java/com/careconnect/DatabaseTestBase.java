package com.careconnect;

import com.careconnect.dao.InstitutionDAO;
import com.careconnect.util.DBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared base for the tests that talk to the real MySQL database
 * (the one configured in db.properties).
 *
 * - If the database can't be reached, or donor 1 / institution 1 aren't
 *   there, every test in the subclass is SKIPPED (not failed), so
 *   "mvn package" still works on a machine without the database.
 * - Every donation / need a test creates must be registered with
 *   trackDonation() / trackNeed(); they're deleted after each test so the
 *   database is left exactly as it was found. Deleting a donation also
 *   removes its item_donations row (ON DELETE CASCADE).
 *
 * Not named *Test, so Maven's surefire plugin doesn't try to run it directly.
 */
public abstract class DatabaseTestBase {

    /** Existing user_id used as the donor in every test. */
    protected static final int DONOR_ID = 1;

    /** Existing institution_id that receives every test donation. */
    protected static final int INSTITUTION_ID = 1;

    private final List<Integer> donationIds = new ArrayList<>();
    private final List<Integer> needIds = new ArrayList<>();

    @BeforeAll
    public static void requireDatabase() {
        boolean ready;
        try (Connection connection = DBConnection.getConnection()) {
            ready = connection != null && new InstitutionDAO().findById(INSTITUTION_ID) != null;
        } catch (Exception e) {
            ready = false;
        }
        Assumptions.assumeTrue(ready,
                "Skipped: database not reachable, or institution_id=" + INSTITUTION_ID + " does not exist.");
    }

    @AfterEach
    public void deleteRowsCreatedByTest() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            // Donations first: a donation may reference a need.
            deleteEach(connection, "DELETE FROM donations WHERE donation_id = ?", donationIds);
            deleteEach(connection, "DELETE FROM needs WHERE need_id = ?", needIds);
        } finally {
            donationIds.clear();
            needIds.clear();
        }
    }

    /** Registers a donation_id for deletion after the test, and returns it for convenience. */
    protected int trackDonation(int donationId) {
        donationIds.add(donationId);
        return donationId;
    }

    /** Registers a need_id for deletion after the test, and returns it for convenience. */
    protected int trackNeed(int needId) {
        needIds.add(needId);
        return needId;
    }

    private static void deleteEach(Connection connection, String sql, List<Integer> ids) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int id : ids) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
        }
    }
}
