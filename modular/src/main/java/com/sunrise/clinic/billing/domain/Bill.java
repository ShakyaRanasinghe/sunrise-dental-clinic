package com.sunrise.clinic.billing.domain;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.Objects;

/**
 * A patient bill/receipt. Carries both the customer-facing total and the
 * internal 3-way revenue split (dentist / clinic / receptionist) computed by
 * {@code RevenueSplitStrategy}. The split fields are visible to Admin only
 * (each staff member sees their own earning).
 */
public class Bill {
    private String id;
    private String appointmentNo;
    private String patientId;
    private String dentistId;
    private String receptionistUid;   // who handled it (earns the service charge)

    // --- line items ---
    private BigDecimal consultationFee;
    private BigDecimal treatmentCost;
    private BigDecimal serviceCharge;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
    private BigDecimal total;

    // --- revenue split (Admin-visible) ---
    private BigDecimal dentistEarning;
    private BigDecimal clinicEarning;
    private BigDecimal receptionistEarning;
    private Instant issuedAt;
    private String issuedByUid;

    public Bill() {
    }

    public Bill(String id, String appointmentNo, String patientId, String dentistId, String receptionistUid, BigDecimal consultationFee, BigDecimal treatmentCost, BigDecimal serviceCharge, BigDecimal discount, BigDecimal tax, BigDecimal total, BigDecimal dentistEarning, BigDecimal clinicEarning, BigDecimal receptionistEarning, Instant issuedAt, String issuedByUid) {
        this.id = id;
        this.appointmentNo = appointmentNo;
        this.patientId = patientId;
        this.dentistId = dentistId;
        this.receptionistUid = receptionistUid;
        this.consultationFee = consultationFee;
        this.treatmentCost = treatmentCost;
        this.serviceCharge = serviceCharge;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.dentistEarning = dentistEarning;
        this.clinicEarning = clinicEarning;
        this.receptionistEarning = receptionistEarning;
        this.issuedAt = issuedAt;
        this.issuedByUid = issuedByUid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDentistId() {
        return dentistId;
    }

    public void setDentistId(String dentistId) {
        this.dentistId = dentistId;
    }

    public String getReceptionistUid() {
        return receptionistUid;
    }

    public void setReceptionistUid(String receptionistUid) {
        this.receptionistUid = receptionistUid;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public void setTreatmentCost(BigDecimal treatmentCost) {
        this.treatmentCost = treatmentCost;
    }

    public BigDecimal getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(BigDecimal serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getDentistEarning() {
        return dentistEarning;
    }

    public void setDentistEarning(BigDecimal dentistEarning) {
        this.dentistEarning = dentistEarning;
    }

    public BigDecimal getClinicEarning() {
        return clinicEarning;
    }

    public void setClinicEarning(BigDecimal clinicEarning) {
        this.clinicEarning = clinicEarning;
    }

    public BigDecimal getReceptionistEarning() {
        return receptionistEarning;
    }

    public void setReceptionistEarning(BigDecimal receptionistEarning) {
        this.receptionistEarning = receptionistEarning;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getIssuedByUid() {
        return issuedByUid;
    }

    public void setIssuedByUid(String issuedByUid) {
        this.issuedByUid = issuedByUid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bill)) return false;
        return Objects.equals(id, ((Bill) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Bill{id=" + id + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String appointmentNo;
        private String patientId;
        private String dentistId;
        private String receptionistUid;
        private BigDecimal consultationFee;
        private BigDecimal treatmentCost;
        private BigDecimal serviceCharge;
        private BigDecimal discount = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;
        private BigDecimal total;
        private BigDecimal dentistEarning;
        private BigDecimal clinicEarning;
        private BigDecimal receptionistEarning;
        private Instant issuedAt;
        private String issuedByUid;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder appointmentNo(String appointmentNo) {
            this.appointmentNo = appointmentNo;
            return this;
        }

        public Builder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder dentistId(String dentistId) {
            this.dentistId = dentistId;
            return this;
        }

        public Builder receptionistUid(String receptionistUid) {
            this.receptionistUid = receptionistUid;
            return this;
        }

        public Builder consultationFee(BigDecimal consultationFee) {
            this.consultationFee = consultationFee;
            return this;
        }

        public Builder treatmentCost(BigDecimal treatmentCost) {
            this.treatmentCost = treatmentCost;
            return this;
        }

        public Builder serviceCharge(BigDecimal serviceCharge) {
            this.serviceCharge = serviceCharge;
            return this;
        }

        public Builder discount(BigDecimal discount) {
            this.discount = discount;
            return this;
        }

        public Builder tax(BigDecimal tax) {
            this.tax = tax;
            return this;
        }

        public Builder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public Builder dentistEarning(BigDecimal dentistEarning) {
            this.dentistEarning = dentistEarning;
            return this;
        }

        public Builder clinicEarning(BigDecimal clinicEarning) {
            this.clinicEarning = clinicEarning;
            return this;
        }

        public Builder receptionistEarning(BigDecimal receptionistEarning) {
            this.receptionistEarning = receptionistEarning;
            return this;
        }

        public Builder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Builder issuedByUid(String issuedByUid) {
            this.issuedByUid = issuedByUid;
            return this;
        }

        public Bill build() {
            return new Bill(id, appointmentNo, patientId, dentistId, receptionistUid, consultationFee, treatmentCost, serviceCharge, discount, tax, total, dentistEarning, clinicEarning, receptionistEarning, issuedAt, issuedByUid);
        }
    }
}
