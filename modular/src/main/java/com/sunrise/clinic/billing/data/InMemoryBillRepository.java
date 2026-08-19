package com.sunrise.clinic.billing.data;

import com.sunrise.clinic.platform.data.InMemoryRepository;

import com.sunrise.clinic.billing.domain.Bill;

import java.util.Optional;

/** In-memory {@link BillRepository}. */
public class InMemoryBillRepository
        extends InMemoryRepository<Bill, String>
        implements BillRepository {

    @Override
    protected String idOf(Bill entity) {
        return entity.getId();
    }

    @Override
    public Optional<Bill> findByAppointmentNo(String appointmentNo) {
        return store.values().stream()
                .filter(b -> appointmentNo.equals(b.getAppointmentNo()))
                .findFirst();
    }
}
