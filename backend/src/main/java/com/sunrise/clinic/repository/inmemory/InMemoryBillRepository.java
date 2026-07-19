package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.Bill;
import com.sunrise.clinic.repository.BillRepository;
import com.sunrise.clinic.repository.InMemoryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** In-memory {@link BillRepository}. */
@Repository
@Profile("!firestore")
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
