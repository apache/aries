package org.apache.aries.jpa.container.weaving.impl;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

/**
 * DummyDataSource to be able to create the EMF before DataSource is ready
 */
public final class DummyDataSource implements DataSource {
    
    /**
     * Simply tries to avoid that calling code runs into NPE
     */
    private final class DummyHandler implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ClassLoader classLoader = this.getClass().getClassLoader();
            if (method.getReturnType() == DatabaseMetaData.class) {
                Class<?>[] ifAr = new Class[] {
                    DatabaseMetaData.class
                };
                return Proxy.newProxyInstance(classLoader, ifAr, this);
            }
            if (method.getReturnType() == int.class) {
                return new Integer(0);
            }
            if (method.getReturnType() == boolean.class) {
                return new Boolean(false);
            }
            if (method.getReturnType() == String.class) {
                return "";
            }
            if (method.getReturnType() == ResultSet.class) {
                Class<?>[] ifAr = new Class[] {
                    ResultSet.class
                };
                return Proxy.newProxyInstance(classLoader, ifAr, this);
            }
            return null;
        }
    }
    
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        InvocationHandler handler = new DummyHandler();
        ClassLoader classLoader = this.getClass().getClassLoader();
        return (Connection)Proxy.newProxyInstance(classLoader, new Class[] {
            Connection.class
        }, handler);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }
}
