package com.sunrise.clinic.repository;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORY / DAO pattern — a persistence abstraction the service layer
 * depends on instead of a concrete database. Two implementations exist behind
 * this interface: an in-memory one (tests + offline demo) and a Firestore one
 * (production). Because services depend on the interface, not the technology,
 * we honour the Dependency-Inversion Principle and can swap storage freely.
 *
 * @param <T>  the entity type
 * @param <ID> the identifier type
 */
public interface Repository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void deleteById(ID id);

    long count();
}
