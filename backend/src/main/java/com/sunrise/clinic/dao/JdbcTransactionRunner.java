package com.sunrise.clinic.dao;

import com.sunrise.clinic.db.Database;
import com.sunrise.clinic.repository.TransactionRunner;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * {@link TransactionRunner} backed by a real database transaction.
 *
 * <p>Every repository call made inside {@link #execute(Supplier)} joins the same
 * connection, so the whole unit of work commits once or rolls back entirely.</p>
 */
public class JdbcTransactionRunner implements TransactionRunner {

    private final Database db;

    public JdbcTransactionRunner(Database db) {
        this.db = db;
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        try {
            return db.inTransaction(connection -> work.get());
        } catch (SQLException e) {
            throw new DataAccessException("Transaction failed", e);
        }
    }
}
