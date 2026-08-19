package com.sunrise.clinic.domain;

import java.util.Objects;

/** A dentist. {@code consultationFee} feeds the billing + revenue-split calculation. */
public class Dentist {
    private String id;
    private String userUid;   // optional link to UserAccount (role DENTIST)
    private String name;
    private String specialization;
    private double consultationFee = 1500.0;   // Rs
    private boolean active = true;

    public Dentist() {
    }

    public Dentist(String id, String userUid, String name, String specialization, double consultationFee, boolean active) {
        this.id = id;
        this.userUid = userUid;
        this.name = name;
        this.specialization = specialization;
        this.consultationFee = consultationFee;
        this.active = active;
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

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dentist)) return false;
        return Objects.equals(id, ((Dentist) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Dentist{id=" + id + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String userUid;
        private String name;
        private String specialization;
        private double consultationFee = 1500.0;
        private boolean active = true;

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

        public Builder specialization(String specialization) {
            this.specialization = specialization;
            return this;
        }

        public Builder consultationFee(double consultationFee) {
            this.consultationFee = consultationFee;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Dentist build() {
            return new Dentist(id, userUid, name, specialization, consultationFee, active);
        }
    }
}
