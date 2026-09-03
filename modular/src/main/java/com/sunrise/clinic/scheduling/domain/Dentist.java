package com.sunrise.clinic.scheduling.domain;

import java.math.BigDecimal;

import java.util.Objects;

/** A dentist. {@code consultationFee} feeds the billing + revenue-split calculation. */
public class Dentist {
    private String id;
    private String userUid;   // optional link to UserAccount (role DENTIST)
    private String name;
    private String specialization;
    /**
     * The dentist's own public telephone number, shown to patients on the clinic's
     * public "Our dentists" page (GAP-FTB-04). Optional: a dentist leaves it blank if
     * they prefer the clinic front desk to take calls on their behalf.
     */
    private String phone;
    /**
     * The dentist's own consultation fee, in rupees.
     *
     * <p>{@code BigDecimal}, not {@code double}. The column is {@code DECIMAL(10,2)}
     * and {@code fn_calculate_bill} computes in DECIMAL, but every money field in
     * {@code layered/} was a {@code double} - a type that cannot represent 0.01
     * exactly, so a total assembled from several such values drifts from the one the
     * database computes for the same inputs. Changed here because this step defines
     * the outward contract for the field; {@code Bill}'s fourteen money fields follow
     * when billing is migrated.</p>
     */
    private BigDecimal consultationFee = new BigDecimal("1500.00");   // Rs
    private boolean active = true;

    public Dentist() {
    }

    public Dentist(String id, String userUid, String name, String specialization, BigDecimal consultationFee, boolean active) {
        this(id, userUid, name, specialization, null, consultationFee, active);
    }

    public Dentist(String id, String userUid, String name, String specialization,
                   String phone, BigDecimal consultationFee, boolean active) {
        this.id = id;
        this.userUid = userUid;
        this.name = name;
        this.specialization = specialization;
        this.phone = phone;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
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
        private String phone;
        private BigDecimal consultationFee = new BigDecimal("1500.00");
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

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder consultationFee(BigDecimal consultationFee) {
            this.consultationFee = consultationFee;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Dentist build() {
            return new Dentist(id, userUid, name, specialization, phone, consultationFee, active);
        }
    }
}
