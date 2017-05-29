package org.apache.aries.transaction.test;

import java.sql.SQLException;

public interface RollbackOnBean {

	void throwException(String name, int value) throws Exception;

	void throwExceptionRollbackOnException(String name, int value) throws Exception;

	void throwRuntimeExceptionRollbackOnException(String name, int value) throws SQLException;

	void throwRuntimeExceptionDontRollbackOnException(String name, int value) throws SQLException;

	void throwRuntimeExceptionDontRollbackOnAppException(String name, int value) throws SQLException;

	void throwRuntimeExceptionRollbackOnAppException(String name, int value) throws SQLException;

	void throwApplicationExceptionRollbackOnException(String name, int value) throws SQLException;

	void throwApplicationExceptionRollbackOnAppException(String name, int value) throws SQLException;

	void throwApplicationExceptionDontRollbackOnException(String name, int value) throws SQLException;

	void throwApplicationExceptionDontRollbackOnAppException(String name, int value) throws SQLException;

	void throwExceptionRollbackOnExceptionDontRollbackOnAppException(String name, int value) throws SQLException;

	void throwExceptionRollbackOnAppExceptionDontRollbackOnException(String name, int value) throws SQLException;

	void throwAppExceptionRollbackOnExceptionDontRollbackOnAppException(String name, int value) throws SQLException;

	void throwAppExceptionRollbackOnAppExceptionDontRollbackOnException(String name, int value) throws SQLException;

	void setrBean(TestBean rBean);
}
