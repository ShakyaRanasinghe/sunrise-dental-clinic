package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.Bill;

import java.util.Optional;

/** Persistence for {@link Bill}s. */
public interface BillRepository extends Repository<Bill, String> {

    Optional<Bill> findByAppointmentNo(String appointmentNo);
}
