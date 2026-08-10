package com.careconnect.service;

import com.careconnect.dao.InstitutionSearchDAO;
import com.careconnect.dao.NeedSearchDAO;
import com.careconnect.model.Institution;
import com.careconnect.model.InstitutionType;
import com.careconnect.model.Need;
import com.careconnect.model.NeedStatus;
import com.careconnect.model.NeedUrgency;
import com.careconnect.model.VerificationStatus;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for Search & Filter (Module 7).
 * Sits between the Controller and the search DAOs - the Controller should
 * call this class, never InstitutionSearchDAO/NeedSearchDAO directly.
 *
 * Currently a thin pass-through, since the actual filtering logic lives in
 * the DAOs' dynamic WHERE clause building. This class exists so future
 * business rules (e.g. "only show APPROVED institutions to the public")
 * have a natural home without touching the Controller or DAO.
 */
public class SearchService {

    private final InstitutionSearchDAO institutionSearchDAO;
    private final NeedSearchDAO needSearchDAO;

    public SearchService() {
        this.institutionSearchDAO = new InstitutionSearchDAO();
        this.needSearchDAO = new NeedSearchDAO();
    }

    /**
     * Searches institutions by any combination of optional filters.
     * Business rule: public search only returns APPROVED institutions,
     * regardless of what verificationStatus filter is passed in - this
     * matches the "discovery-first" principle while still respecting that
     * unverified institutions shouldn't be publicly browsable.
     * Pass includeUnapproved=true to bypass this (e.g. for an admin view).
     */
    public List<Institution> searchInstitutions(String city, InstitutionType institutionType,
                                                 VerificationStatus verificationStatus,
                                                 boolean includeUnapproved) throws SQLException {

        VerificationStatus effectiveStatus = includeUnapproved
                ? verificationStatus
                : VerificationStatus.APPROVED; // public search always forces this

        return institutionSearchDAO.search(city, institutionType, effectiveStatus);
    }

    /**
     * Searches needs by any combination of optional filters.
     */
    public List<Need> searchNeeds(String category, NeedUrgency urgency, Integer institutionId,
                                   NeedStatus status) throws SQLException {
        return needSearchDAO.search(category, urgency, institutionId, status);
    }
}
