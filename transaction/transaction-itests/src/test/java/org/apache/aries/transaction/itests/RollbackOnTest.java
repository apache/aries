package org.apache.aries.transaction.itests;

import org.apache.aries.transaction.test.RollbackOnBean;
import org.apache.aries.transaction.test.TestBean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.util.Filter;

import javax.inject.Inject;
import javax.transaction.Status;
import java.sql.SQLException;

import static junit.framework.Assert.assertEquals;

public class RollbackOnTest extends AbstractIntegrationTest {

	@Inject
	@Filter(timeout = 120000, value = "(tranAttribute=Required)")
	TestBean rBean;

	@Inject
	RollbackOnBean rollbackOnBean;

	@Before
	public void setUp() throws Exception {
		rollbackOnBean.setrBean(rBean);
	}

	//default behavior, doesn't rollback on application exception
	@Test
	public void testNoRollbackOnDefaultAppException() throws Exception {
		tran.begin();
		try {
			rBean.insertRow("test", 1, new SQLException());
		} catch (SQLException e) {
			// Ignore expected
		}
		int txStatus = tran.getStatus();
		tran.rollback();
		Assert.assertEquals("tx was rolled back", Status.STATUS_ACTIVE, txStatus);
	}

	//default behavior, doesn't rollback on exception
	@Test
	public void testNoRollbackOnDefaultException() throws Exception {
		tran.begin();
		try {
			rollbackOnBean.throwException("test", 2);
		} catch (Exception e) {
			// Ignore expected
		}
		int txStatus = tran.getStatus();
		tran.rollback();
		Assert.assertEquals("tx was rolled back", Status.STATUS_ACTIVE, txStatus);
	}

	@Test
	public void testExceptionRollbackOnException() throws Exception {
		tran.begin();
		try {
			rollbackOnBean.throwExceptionRollbackOnException("noAnnotationDefaultException", -1);
		} catch (Exception e) {
			// Ignore expected
		}
		int txStatus = tran.getStatus();
		tran.rollback();
		Assert.assertEquals("tx was not rolled back", Status.STATUS_MARKED_ROLLBACK, txStatus);
	}

	@Test
	public void testNoRollbackOnDefaultRuntimeException() throws Exception {
		tran.begin();
		try {
			rBean.insertRow("test", 1, new RuntimeException());
		} catch (Exception e) {
			// Ignore expected
		}
		int txStatus = tran.getStatus();
		tran.rollback();
		Assert.assertEquals("tx was not rolled back", Status.STATUS_MARKED_ROLLBACK, txStatus);
	}

	//throw Runtime / handle Exception
	@Test
	public void testDontRollbackOnRuntimeException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean.throwRuntimeExceptionDontRollbackOnException("testDontRollbackOnRuntimeException", 3);
		} catch (Exception e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
			//expected
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 0, finalRows - initialRows);
	}

	@Test
	public void testRollbackOnRuntimeException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean.throwRuntimeExceptionRollbackOnException("testRollbackOnRuntimeException", 4);
		} catch (Exception e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
			//expected
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 0, finalRows - initialRows);
	}

	//throw runtime / handle AppException
	@Test
	public void testThrowRuntimeDontRollbackOnAppException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean.throwRuntimeExceptionDontRollbackOnAppException("testThrowRuntimeDontRollbackOnAppException", 5);
			tran.commit();
		} catch (Exception e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
			//expected
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 0, finalRows - initialRows);
	}

	@Test
	public void testThrowRuntimeRollbackOnException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean.throwRuntimeExceptionRollbackOnAppException("testThrowRuntimeRollbackOnException", 6);
		} catch (Exception e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
			//expected
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 0, finalRows - initialRows);
	}


	//throw AppException / handle Exception
	@Test
	public void testThrowAppExceptionRollbackOnException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean.throwApplicationExceptionRollbackOnException("testThrowAppExceptionRollbackOnException", 7);
		} catch (SQLException e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
			//expected
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 0, finalRows - initialRows);
	}

	@Test
	public void testThrowAppExceptionDontRollbackOnException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean.throwApplicationExceptionDontRollbackOnException("testThrowAppExceptionDontRollbackOnException", 8);
		} catch (SQLException e) {
			//ignore
		}
		tran.commit();
		int finalRows = counter.countRows();
		assertEquals("Added rows", 1, finalRows - initialRows);
	}


	//throw AppException / handle AppException
	@Test
	public void testThrowAppExceptionDontRollbackOnAppException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean.throwApplicationExceptionDontRollbackOnAppException("testThrowAppExceptionDontRollbackOnAppException", 9);
		} catch (SQLException e) {
			//ignore
		}
		tran.commit();
		int finalRows = counter.countRows();
		assertEquals("Added rows", 1, finalRows - initialRows);
	}

	@Test
	public void testThrowAppExceptionRollbackOnAppException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean.throwApplicationExceptionRollbackOnAppException("testThrowAppExceptionRollbackOnAppException", 10);
		} catch (SQLException e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
			//expected
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 0, finalRows - initialRows);
	}


	//throw Exception, handle Exception + DontRollBackOn
	@Test
	public void testThrowRuntimeExceptionRollbackExceptionDontRollbackOnAppException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean
					.throwExceptionRollbackOnExceptionDontRollbackOnAppException("testThrowRuntimeExceptionRollbackExceptionDontRollbackOnAppException", 11);
		} catch (Exception e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
			//expected
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 0, finalRows - initialRows);
	}

	@Test
	public void testThrowRuntimeExceptionDontRollbackExceptionRollbackOnAppException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean
					.throwExceptionRollbackOnAppExceptionDontRollbackOnException("testThrowRuntimeExceptionDontRollbackExceptionRollbackOnAppException", 12);
		} catch (Exception e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
			//expected
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 0, finalRows - initialRows);
	}

	@Test
	public void testThrowAppExceptionDontRollbackExceptionRollbackOnAppException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean
					.throwAppExceptionRollbackOnAppExceptionDontRollbackOnException("testThrowAppExceptionDontRollbackExceptionRollbackOnAppException", 13);
		} catch (SQLException e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			//expected
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 1, finalRows - initialRows);
	}

	@Test
	public void testThrowAppExceptionRollbackExceptionDontRollbackOnAppException() throws Exception {
		int initialRows = counter.countRows();
		tran.begin();
		try {
			rollbackOnBean
					.throwAppExceptionRollbackOnExceptionDontRollbackOnAppException("testThrowAppExceptionRollbackExceptionDontRollbackOnAppException", 14);
		} catch (Exception e) {
			//ignore
		}
		try {
			tran.commit();
		} catch (Exception e) {
			//expected
			if (Status.STATUS_ACTIVE == tran.getStatus()) {
				tran.rollback();
			}
		}
		int finalRows = counter.countRows();
		assertEquals("Added rows", 1, finalRows - initialRows);
	}


	@Override
	protected TestBean getBean() {
		return rBean;
	}

}
