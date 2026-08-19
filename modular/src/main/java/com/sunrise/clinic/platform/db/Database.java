package com.sunrise.clinic.platform.db;

import com.sunrise.clinic.platform.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The project's JDBC access point: a small fixed-size connection pool plus the
 * transaction helper the service layer uses.
 *
 * <p>With no framework there is no container-managed datasource and no
 * {@code @Transactional}, so both are provided here:</p>
 *
 * <ul>
 *   <li><b>Pooling</b> — opening a TCP connection and authenticating per request is
 *       slow, so a fixed number of connections are created up front and lent out
 *       through a {@link BlockingQueue}. A caller blocks (briefly) when all are in
 *       use rather than overwhelming MySQL.</li>
 *   <li><b>Transactions</b> — {@link #inTransaction(TransactionalWork)} turns
 *       auto-commit off, runs the work, then commits, or rolls back if anything is
 *       thrown. This is what makes the booking flow atomic and is the explicit,
 *       readable equivalent of the annotation it replaces.</li>
 * </ul>
 */
public final class Database implements AutoCloseable {

    private static final Logger log = Logger.getLogger(Database.class.getName());

    /** Work that runs inside a transaction and returns a result. */
    @FunctionalInterface
    public interface TransactionalWork<T> {
        T run(Connection connection) throws SQLException;
    }

    /**
     * The connection belonging to the transaction the current thread is inside,
     * if any. {@link #borrow()} hands this out so that every DAO call made within
     * {@link #inTransaction} joins the same transaction — the mechanism that
     * replaces the framework's transaction-scoped datasource proxy.
     */
    private final ThreadLocal<Connection> activeTransaction = new ThreadLocal<>();

    private final BlockingQueue<Connection> pool;
    private final int size;
    private final String url;
    private final String user;
    private final String password;
    private volatile boolean closed;

    public Database(AppConfig config) {
        this.url = config.get("db.url", "jdbc:mysql://localhost:3306/sunrise_dental"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        this.user = config.get("db.user", "root");
        this.password = config.get("db.password", "");
        this.size = config.getInt("db.pool.size", 8);
        this.pool = new ArrayBlockingQueue<>(size);

        try {
            // Explicit load keeps the driver discoverable inside a servlet container,
            // where automatic service discovery can be affected by classloader scoping.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is not on the classpath", e);
        }

        for (int i = 0; i < size; i++) {
            pool.add(openConnection());
        }
        log.log(Level.INFO, "database_pool_ready url={0} size={1}", new Object[]{url, size});
    }

    private Connection openConnection() {
        try {
            Connection connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect to the database at " + url, e);
        }
    }

    /**
     * Borrow a connection. The returned object is a wrapper whose
     * {@code close()} returns the connection to the pool instead of closing it,
     * so callers can still use try-with-resources in the usual way.
     */
    public Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("The connection pool has been shut down");
        }
        try {
            Connection connection = pool.poll(10, TimeUnit.SECONDS);
            if (connection == null) {
                throw new SQLException("Timed out waiting for a free database connection");
            }
            if (!connection.isValid(2)) {
                // The server dropped it (idle timeout / restart) — replace it.
                closeQuietly(connection);
                connection = openConnection();
            }
            return PooledConnection.wrap(connection, this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a database connection", e);
        }
    }

    /**
     * The connection a DAO should use for a single statement.
     *
     * <p>Inside {@link #inTransaction} this is the transaction's own connection,
     * wrapped so that closing it is a no-op — the transaction, not the DAO, decides
     * when it ends. Outside a transaction it is an ordinary pooled connection that
     * really does return to the pool on close. Either way the DAO writes the same
     * try-with-resources block and needs to know nothing about transactions.</p>
     */
    public Connection borrow() throws SQLException {
        Connection transactional = activeTransaction.get();
        if (transactional != null) {
            return PooledConnection.nonClosing(transactional);
        }
        return getConnection();
    }

    /**
     * Run {@code work} inside a single transaction, committing on success and
     * rolling back on any failure. Nested calls join the outer transaction rather
     * than starting a second one.
     *
     * @param work the statements to execute atomically
     * @return whatever the work returns
     */
    public <T> T inTransaction(TransactionalWork<T> work) throws SQLException {
        Connection existing = activeTransaction.get();
        if (existing != null) {
            // Already inside a transaction — join it; the outermost call commits.
            return work.run(existing);
        }
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            activeTransaction.set(connection);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    log.log(Level.SEVERE, "rollback_failed", rollbackFailure);
                }
                throw e;
            } finally {
                activeTransaction.remove();
                connection.setAutoCommit(true);
            }
        }
    }

    /** Return a borrowed connection to the pool. Called by {@link PooledConnection#close()}. */
    void release(Connection connection) {
        if (closed) {
            closeQuietly(connection);
            return;
        }
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
            if (!pool.offer(connection)) {
                closeQuietly(connection);
            }
        } catch (SQLException e) {
            log.log(Level.WARNING, "connection_release_failed", e);
            closeQuietly(connection);
        }
    }

    @Override
    public void close() {
        closed = true;
        for (Connection connection : pool) {
            closeQuietly(connection);
        }
        pool.clear();
        log.info("database_pool_closed");
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            log.log(Level.FINE, "connection_close_failed", e);
        }
    }
}
