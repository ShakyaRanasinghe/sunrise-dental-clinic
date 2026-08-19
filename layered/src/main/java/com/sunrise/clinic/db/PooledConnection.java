package com.sunrise.clinic.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a pooled {@link Connection} so that {@code close()} hands it back to the
 * {@link Database} pool instead of really closing it. Every other call passes
 * straight through to the real connection.
 *
 * <p>{@link Connection} declares around fifty methods, so rather than write fifty
 * delegating stubs the wrapper is produced with a JDK dynamic {@link Proxy}: the
 * handler intercepts the two calls whose behaviour must change and forwards the
 * rest. Callers still use try-with-resources exactly as they would with a plain
 * connection.</p>
 */
final class PooledConnection implements InvocationHandler {

    private final Connection delegate;
    private final Database pool;
    private boolean returned;

    private PooledConnection(Connection delegate, Database pool) {
        this.delegate = delegate;
        this.pool = pool;
    }

    /** @return a proxy that behaves like {@code delegate} but pools itself on close. */
    static Connection wrap(Connection delegate, Database pool) {
        return proxy(new PooledConnection(delegate, pool));
    }

    /**
     * @return a proxy that ignores {@code close()} entirely. Used for the connection
     * that belongs to an in-flight transaction, so a DAO's try-with-resources cannot
     * end the transaction early.
     */
    static Connection nonClosing(Connection delegate) {
        return proxy(new PooledConnection(delegate, null));
    }

    private static Connection proxy(PooledConnection handler) {
        return (Connection) Proxy.newProxyInstance(
                PooledConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "close" -> {
                // A transaction-scoped wrapper (pool == null) ignores close outright.
                // Otherwise hand the connection back rather than closing it — idempotent,
                // so a nested try-with-resources cannot return the same one twice.
                if (pool != null && !returned) {
                    returned = true;
                    pool.release(delegate);
                }
                return null;
            }
            case "isClosed" -> {
                return returned || delegate.isClosed();
            }
            default -> {
                if (returned) {
                    throw new SQLException("This connection has already been returned to the pool");
                }
                try {
                    return method.invoke(delegate, args);
                } catch (InvocationTargetException e) {
                    // Unwrap so callers see the real SQLException, not the reflection wrapper.
                    throw e.getCause();
                }
            }
        }
    }
}
