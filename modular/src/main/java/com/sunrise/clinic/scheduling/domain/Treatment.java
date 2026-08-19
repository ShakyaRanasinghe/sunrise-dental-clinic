package com.sunrise.clinic.scheduling.domain;

import java.math.BigDecimal;

import java.util.Objects;

/** A treatment offered by the clinic. Admin maintains the catalogue &amp; pricing. */
public class Treatment {
    private String id;
    private String name;
    private String description;
    /** The published price in rupees, before fee and discount. See {@link Dentist}. */
    private BigDecimal baseCost;
    private boolean active = true;

    public Treatment() {
    }

    public Treatment(String id, String name, String description, BigDecimal baseCost, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseCost = baseCost;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
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
        if (!(o instanceof Treatment)) return false;
        return Objects.equals(id, ((Treatment) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Treatment{id=" + id + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private BigDecimal baseCost;
        private boolean active = true;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder baseCost(BigDecimal baseCost) {
            this.baseCost = baseCost;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Treatment build() {
            return new Treatment(id, name, description, baseCost, active);
        }
    }
}
