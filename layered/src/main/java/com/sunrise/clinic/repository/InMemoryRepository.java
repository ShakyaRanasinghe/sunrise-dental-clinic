package com.sunrise.clinic.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base in-memory implementation of {@link Repository}, backed by a thread-safe
 * map. Concrete repositories extend this and implement {@link #idOf(Object)}
 * (and any custom query methods). Used by the unit tests, which need no database;
 * the JDBC DAOs in {@code com.sunrise.clinic.dao} implement the same interfaces.
 *
 * @param <T>  entity type
 * @param <ID> identifier type
 */
public abstract class InMemoryRepository<T, ID> implements Repository<T, ID> {

    protected final Map<ID, T> store = new ConcurrentHashMap<>();

    /** @return the identifier of the given entity (defined by each subclass). */
    protected abstract ID idOf(T entity);

    @Override
    public T save(T entity) {
        store.put(idOf(entity), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    /** Test helper: clear all entries. */
    public void clear() {
        store.clear();
    }
}
