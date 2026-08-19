package com.sunrise.clinic.platform.data;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORY / DAO pattern — a persistence abstraction the service layer
 * depends on instead of a concrete database. Two implementations exist behind
 * this interface: an in-memory one (unit tests) and a JDBC/MySQL one
 * (the running application). Because services depend on the interface, not the
 * technology, we honour the Dependency-Inversion Principle and can swap storage
 * freely — which is exactly what made the move off the cloud database a
 * change of implementation rather than a rewrite.
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
