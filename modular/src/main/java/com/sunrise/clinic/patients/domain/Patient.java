package com.sunrise.clinic.patients.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A registered patient.
 *
 * <p>{@code userUid} is <b>optional</b> and links to the {@code user_account} of a
 * patient who uses the portal. It is null for a walk-in - someone the clinic treats
 * who has never signed in - which is why the class diagram draws that association
 * as optional (ASM-04).</p>
 *
 * <p>This class stays inside the module. What crosses the boundary to a screen or a
 * client is {@link PatientResponse}, which omits {@code userUid}: address, contact
 * number and date of birth are personal data, and the account identifier is
 * internal.</p>
 */
public class Patient {
    private String id;
    private String userUid;   // link to UserAccount (role PATIENT)
    private String name;
    private String address;
    private String contactNumber;
    private String email;
    private LocalDate dob;
    // GAP-PAT-27: the patient's own diagnosis / dental-history details, declared
    // from the profile. Free text; null when the patient has written nothing.
    private String diagnosisDetails;

    public Patient() {
    }

    public Patient(String id, String userUid, String name, String address, String contactNumber, String email, LocalDate dob) {
        this(id, userUid, name, address, contactNumber, email, dob, null);
    }

    public Patient(String id, String userUid, String name, String address, String contactNumber, String email, LocalDate dob, String diagnosisDetails) {
        this.id = id;
        this.userUid = userUid;
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.email = email;
        this.dob = dob;
        this.diagnosisDetails = diagnosisDetails;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getDiagnosisDetails() {
        return diagnosisDetails;
    }

    public void setDiagnosisDetails(String diagnosisDetails) {
        this.diagnosisDetails = diagnosisDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient)) return false;
        return Objects.equals(id, ((Patient) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Patient{id=" + id + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String userUid;
        private String name;
        private String address;
        private String contactNumber;
        private String email;
        private LocalDate dob;
        private String diagnosisDetails;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder userUid(String userUid) {
            this.userUid = userUid;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder contactNumber(String contactNumber) {
            this.contactNumber = contactNumber;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder dob(LocalDate dob) {
            this.dob = dob;
            return this;
        }

        public Builder diagnosisDetails(String diagnosisDetails) {
            this.diagnosisDetails = diagnosisDetails;
            return this;
        }

        public Patient build() {
            return new Patient(id, userUid, name, address, contactNumber, email, dob, diagnosisDetails);
        }
    }
}
