package com.sunrise.clinic.domain;

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
    private double consultationFee;
    private double treatmentCost;
    private double serviceCharge;
    private double discount = 0.0;
    private double tax = 0.0;
    private double total;

    // --- revenue split (Admin-visible) ---
    private double dentistEarning;
    private double clinicEarning;
    private double receptionistEarning;
    private Instant issuedAt;
    private String issuedByUid;

    public Bill() {
    }

    public Bill(String id, String appointmentNo, String patientId, String dentistId, String receptionistUid, double consultationFee, double treatmentCost, double serviceCharge, double discount, double tax, double total, double dentistEarning, double clinicEarning, double receptionistEarning, Instant issuedAt, String issuedByUid) {
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

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public double getTreatmentCost() {
        return treatmentCost;
    }

    public void setTreatmentCost(double treatmentCost) {
        this.treatmentCost = treatmentCost;
    }

    public double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDentistEarning() {
        return dentistEarning;
    }

    public void setDentistEarning(double dentistEarning) {
        this.dentistEarning = dentistEarning;
    }

    public double getClinicEarning() {
        return clinicEarning;
    }

    public void setClinicEarning(double clinicEarning) {
        this.clinicEarning = clinicEarning;
    }

    public double getReceptionistEarning() {
        return receptionistEarning;
    }

    public void setReceptionistEarning(double receptionistEarning) {
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
        private double consultationFee;
        private double treatmentCost;
        private double serviceCharge;
        private double discount = 0.0;
        private double tax = 0.0;
        private double total;
        private double dentistEarning;
        private double clinicEarning;
        private double receptionistEarning;
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

        public Builder consultationFee(double consultationFee) {
            this.consultationFee = consultationFee;
            return this;
        }

        public Builder treatmentCost(double treatmentCost) {
            this.treatmentCost = treatmentCost;
            return this;
        }

        public Builder serviceCharge(double serviceCharge) {
            this.serviceCharge = serviceCharge;
            return this;
        }

        public Builder discount(double discount) {
            this.discount = discount;
            return this;
        }

        public Builder tax(double tax) {
            this.tax = tax;
            return this;
        }

        public Builder total(double total) {
            this.total = total;
            return this;
        }

        public Builder dentistEarning(double dentistEarning) {
            this.dentistEarning = dentistEarning;
            return this;
        }

        public Builder clinicEarning(double clinicEarning) {
            this.clinicEarning = clinicEarning;
            return this;
        }

        public Builder receptionistEarning(double receptionistEarning) {
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
