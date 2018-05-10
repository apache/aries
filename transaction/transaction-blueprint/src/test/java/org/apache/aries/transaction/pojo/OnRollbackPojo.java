package org.apache.aries.transaction.pojo;

import java.sql.BatchUpdateException;
import java.sql.SQLDataException;

import javax.transaction.Transactional;

@Transactional(rollbackOn = SQLDataException.class)
public class OnRollbackPojo {

    @Transactional(rollbackOn = BatchUpdateException.class)
    public void throwBatchUpdateException(String s) throws BatchUpdateException {
        throw new BatchUpdateException();
    }

    public void throwSQLDataException(String s) throws SQLDataException {
        throw new SQLDataException();
    }

}
