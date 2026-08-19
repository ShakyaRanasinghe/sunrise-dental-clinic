package com.sunrise.clinic.billing.data;

import com.sunrise.clinic.platform.data.Repository;

import com.sunrise.clinic.billing.domain.Bill;

import java.util.Optional;

/** Persistence for {@link Bill}s. */
public interface BillRepository extends Repository<Bill, String> {

    Optional<Bill> findByAppointmentNo(String appointmentNo);
}
