package org.apache.aries.transaction.test.impl;

import javax.transaction.Transactional;
import java.sql.SQLException;

/**
 * Created by Maxim Becker on 31.05.15.
 */
public class JtaAnnotatedTestBeanImpl extends TestBeanImpl {

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public void insertRow(String name, int value) throws SQLException {
        super.insertRow(name, value);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public void insertRow(String name, int value, Exception e) throws SQLException {
        super.insertRow(name, value, e);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public void insertRow(String name, int value, boolean delegate) throws SQLException {
        super.insertRow(name, value, delegate);
    }

    @Override
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public int countRows() throws SQLException {
        return super.countRows();
    }
}
