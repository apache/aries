package org.apache.aries.transaction.test.impl;

import org.apache.aries.transaction.test.RollbackOnBean;
import org.apache.aries.transaction.test.TestBean;

import javax.transaction.Transactional;
import java.sql.SQLException;

import static javax.transaction.Transactional.TxType;

public class RollbackOnBeanImpl implements RollbackOnBean {

	private TestBean rBean;

	@Override
	@Transactional(value = TxType.REQUIRED)
	public void throwException(String name, int value) throws Exception {
		rBean.insertRow(name, value, null);
		throw new Exception("Test exception");
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
	public void throwExceptionRollbackOnException(String name, int value) throws Exception {
		rBean.insertRow(name, value, null);
		throw new Exception("Test exception");
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
	public void throwRuntimeExceptionRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = SQLException.class)
	public void throwRuntimeExceptionRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = Exception.class)
	public void throwRuntimeExceptionDontRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = SQLException.class)
	public void throwRuntimeExceptionDontRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
	public void throwApplicationExceptionRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = SQLException.class)
	public void throwApplicationExceptionRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = Exception.class)
	public void throwApplicationExceptionDontRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = SQLException.class)
	public void throwApplicationExceptionDontRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = Exception.class, rollbackOn = SQLException.class)
	public void throwExceptionRollbackOnExceptionDontRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = SQLException.class, rollbackOn = Exception.class)
	public void throwExceptionRollbackOnAppExceptionDontRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = SQLException.class, rollbackOn = Exception.class)
	public void throwAppExceptionRollbackOnExceptionDontRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = Exception.class, rollbackOn = SQLException.class)
	public void throwAppExceptionRollbackOnAppExceptionDontRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}


	@Override
	public void setrBean(TestBean rBean) {
		this.rBean = rBean;
	}

}
