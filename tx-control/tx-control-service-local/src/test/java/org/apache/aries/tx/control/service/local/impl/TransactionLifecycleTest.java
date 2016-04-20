package org.apache.aries.tx.control.service.local.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.ResourceProvider;

@RunWith(MockitoJUnitRunner.class)
public class TransactionLifecycleTest {

	@Mock
	ResourceProvider<Object> testProvider;
	@Mock
	LocalResource testResource;

	TransactionControlImpl txControl;

	@Before
	public void setUp() {
		txControl = new TransactionControlImpl();
	}

	@Test
	public void testRequired() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			return null;
		});

	}

	@Test
	public void testNestedRequired() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			Object key = txControl.getCurrentContext().getTransactionKey();
			txControl.getCurrentContext().putScopedValue("visible", Boolean.TRUE);

			txControl.required(() -> {
				assertEquals(key, txControl.getCurrentContext().getTransactionKey());
				assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));
				txControl.getCurrentContext().putScopedValue("visible", Boolean.FALSE);
				return null;
			});

			assertEquals(key, txControl.getCurrentContext().getTransactionKey());
			assertEquals(Boolean.FALSE, txControl.getCurrentContext().getScopedValue("visible"));
			
			return null;
		});

	}

	@Test
	public void testNestedRequiredFromNoTran() {

		txControl.supports(() -> {

			assertFalse(txControl.activeTransaction());

			txControl.getCurrentContext().putScopedValue("invisible", Boolean.TRUE);

			txControl.required(() -> {
				assertTrue(txControl.activeTransaction());
				assertNull(txControl.getCurrentContext().getScopedValue("invisible"));
				txControl.getCurrentContext().putScopedValue("invisible", Boolean.FALSE);
				return null;
			});

			assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("invisible"));
			
			return null;
		});

	}

	@Test
	public void testRequiresNew() {

		txControl.requiresNew(() -> {

			assertTrue(txControl.activeTransaction());

			return null;
		});

	}

	@Test
	public void testNestedRequiresNew() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			Object key = txControl.getCurrentContext().getTransactionKey();
			txControl.getCurrentContext().putScopedValue("invisible", Boolean.TRUE);

			txControl.requiresNew(() -> {
				assertFalse("Parent key " + key + " Child Key " + txControl.getCurrentContext().getTransactionKey(),
						key.equals(txControl.getCurrentContext().getTransactionKey()));
				assertNull(txControl.getCurrentContext().getScopedValue("invisible"));
				txControl.getCurrentContext().putScopedValue("invisible", Boolean.FALSE);
				return null;
			});

			assertEquals(key, txControl.getCurrentContext().getTransactionKey());
			assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("invisible"));
			
			return null;
		});

	}

	@Test
	public void testSupports() {

		txControl.supports(() -> {

			assertFalse(txControl.activeTransaction());

			return null;
		});

	}

	@Test
	public void testNestedSupports() {

		txControl.supports(() -> {

			assertFalse(txControl.activeTransaction());

			txControl.getCurrentContext().putScopedValue("visible", Boolean.TRUE);

			txControl.supports(() -> {
				assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));
				txControl.getCurrentContext().putScopedValue("visible", Boolean.FALSE);
				return null;
			});
			
			assertEquals(Boolean.FALSE, txControl.getCurrentContext().getScopedValue("visible"));

			return null;
		});

	}

	@Test
	public void testNestedSupportsInActiveTran() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			Object key = txControl.getCurrentContext().getTransactionKey();
			txControl.getCurrentContext().putScopedValue("visible", Boolean.TRUE);

			txControl.supports(() -> {
				assertEquals(key, txControl.getCurrentContext().getTransactionKey());
				assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));
				txControl.getCurrentContext().putScopedValue("visible", Boolean.FALSE);
				return null;
			});
			
			assertEquals(key, txControl.getCurrentContext().getTransactionKey());
			assertEquals(Boolean.FALSE, txControl.getCurrentContext().getScopedValue("visible"));

			return null;
		});

	}
	
	@Test
	public void testNotSupported() {

		txControl.notSupported(() -> {

			assertFalse(txControl.activeTransaction());

			return null;
		});

	}

	@Test
	public void testNestedNotSupported() {

		txControl.notSupported(() -> {

			assertFalse(txControl.activeTransaction());

			txControl.getCurrentContext().putScopedValue("visible", Boolean.TRUE);

			txControl.notSupported(() -> {
				assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));
				return null;
			});
			
			assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));

			return null;
		});

	}

	@Test
	public void testNestedNotSupportedInActiveTran() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			Object key = txControl.getCurrentContext().getTransactionKey();
			txControl.getCurrentContext().putScopedValue("invisible", Boolean.TRUE);

			txControl.notSupported(() -> {
				assertFalse(txControl.activeTransaction());
				assertNull(txControl.getCurrentContext().getScopedValue("invisible"));
				txControl.getCurrentContext().putScopedValue("invisible", Boolean.FALSE);
				
				return null;
			});
			
			assertEquals(key, txControl.getCurrentContext().getTransactionKey());
			assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("invisible"));

			return null;
		});

	}

}
