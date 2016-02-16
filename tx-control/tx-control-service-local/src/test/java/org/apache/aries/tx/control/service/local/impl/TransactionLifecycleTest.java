package org.apache.aries.tx.control.service.local.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.ResourceProvider;

@RunWith(MockitoJUnitRunner.class)
public class TransactionLifecycleTest {

	@Mock
	Coordinator coordinator;
	@Mock
	Coordination coordination1;
	@Mock
	Coordination coordination2;

	@Mock
	ResourceProvider<Object> testProvider;
	@Mock
	LocalResource testResource;

	TransactionControlImpl txControl;

	Map<Class<?>, Object> variables1;
	Map<Class<?>, Object> variables2;

	@Before
	public void setUp() {
		variables1 = new HashMap<>();
		variables2 = new HashMap<>();

		setupCoordinations();

		txControl = new TransactionControlImpl(coordinator);
	}

	/**
	 * Allow up to two Coordinations to be happening
	 */
	private void setupCoordinations() {
		Mockito.when(coordinator.begin(Mockito.anyString(), Mockito.anyLong())).then(i -> {
			Mockito.when(coordinator.peek()).thenReturn(coordination1);
			return coordination1;
		}).then(i -> {
			Mockito.when(coordinator.peek()).thenReturn(coordination2);
			return coordination2;
		}).thenThrow(new IllegalStateException("Only two coordinations at a time in the test"));

		Mockito.when(coordination1.getVariables()).thenReturn(variables1);
		Mockito.when(coordination1.getId()).thenReturn(42L);
		Mockito.doAnswer(i -> {
			Mockito.when(coordinator.peek()).thenReturn(null);
			return null;
		}).when(coordination1).end();
		Mockito.doAnswer(i -> {
			Mockito.when(coordinator.peek()).thenReturn(null);
			return null;
		}).when(coordination1).fail(Mockito.any(Throwable.class));

		Mockito.when(coordination2.getVariables()).thenReturn(variables2);
		Mockito.when(coordination2.getId()).thenReturn(43L);
		Mockito.doAnswer(i -> {
			Mockito.when(coordinator.peek()).thenReturn(coordination1);
			return null;
		}).when(coordination2).end();
		Mockito.doAnswer(i -> {
			Mockito.when(coordinator.peek()).thenReturn(coordination1);
			return null;
		}).when(coordination2).fail(Mockito.any(Throwable.class));
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
