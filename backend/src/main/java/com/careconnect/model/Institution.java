package com.careconnect.model;

import java.sql.Timestamp;

/**
 * Represents a row in the `institutions` table.
 *
 * An Institution profile belongs to a User through user_id.
 * The institution must be approved by an administrator before
 * it can be considered verified.
 */
public class Institution {

    private int institutionId;
    private int userId;
    private String institutionName;
    private InstitutionType institutionType;
    private String registrationNumber;
    private String description;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String contactPerson;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private Timestamp createdAt;

    public Institution() {
    }

    /**
     * Used when reading a complete institution record from the database.
     */
    public Institution(int institutionId, int userId, String institutionName,
                       InstitutionType institutionType, String registrationNumber,
                       String description, String address, String city,
                       String state, String pincode, String contactPerson,
                       VerificationStatus verificationStatus, String rejectionReason,
                       Timestamp createdAt) {

        this.institutionId = institutionId;
        this.userId = userId;
        this.institutionName = institutionName;
        this.institutionType = institutionType;
        this.registrationNumber = registrationNumber;
        this.description = description;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.contactPerson = contactPerson;
        this.verificationStatus = verificationStatus;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    /**
     * Used when registering a new institution.
     * institutionId and createdAt are assigned by the DB.
     * verificationStatus always starts at PENDING - an admin must approve it later.
     */
    public Institution(int userId, String institutionName,
                       InstitutionType institutionType, String registrationNumber,
                       String description, String address, String city,
                       String state, String pincode, String contactPerson) {

        this.userId = userId;
        this.institutionName = institutionName;
        this.institutionType = institutionType;
        this.registrationNumber = registrationNumber;
        this.description = description;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.contactPerson = contactPerson;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(int institutionId) {
        this.institutionId = institutionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Institution{" +
                "institutionId=" + institutionId +
                ", userId=" + userId +
                ", institutionName='" + institutionName + '\'' +
                ", institutionType=" + institutionType +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", verificationStatus=" + verificationStatus +
                ", createdAt=" + createdAt +
                '}';
    }
}
