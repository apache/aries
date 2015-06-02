package org.apache.aries.transaction.test.impl;

import org.apache.aries.transaction.annotations.Transaction;
import org.apache.aries.transaction.annotations.TransactionPropagationType;

import java.sql.SQLException;

/**
 * Created by Maxim Becker on 31.05.15.
 */
public class AnnotatedTestBeanImpl extends TestBeanImpl {

    @Override
    @Transaction(TransactionPropagationType.Mandatory)
    public void insertRow(String name, int value) throws SQLException {
        super.insertRow(name, value);
    }

    @Override
    @Transaction(TransactionPropagationType.Mandatory)
    public void insertRow(String name, int value, Exception e) throws SQLException {
        super.insertRow(name, value, e);
    }

    @Override
    @Transaction(TransactionPropagationType.Mandatory)
    public void insertRow(String name, int value, boolean delegate) throws SQLException {
        super.insertRow(name, value, delegate);
    }

    @Override
    @Transaction(TransactionPropagationType.NotSupported)
    public int countRows() throws SQLException {
        return super.countRows();
    }
}
