package org.apache.aries.tx.control.service.local.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.osgi.service.transaction.control.TransactionStatus.ACTIVE;
import static org.osgi.service.transaction.control.TransactionStatus.MARKED_ROLLBACK;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.HashMap;
import java.util.Map;

import org.apache.aries.tx.control.service.local.impl.TransactionControlImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.ResourceProvider;

@RunWith(MockitoJUnitRunner.class)
public class TransactionControlStatusTest {

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

	Object resource = new Object();
	Map<Class<?>, Object> variables1;
	Map<Class<?>, Object> variables2;

	@Before
	public void setUp() {

		resource = new Object();
		variables1 = new HashMap<>();
		variables2 = new HashMap<>();

		Mockito.when(coordination1.getVariables()).thenReturn(variables1);
		Mockito.when(coordination2.getVariables()).thenReturn(variables2);
		
		txControl = new TransactionControlImpl(coordinator);
	}

	@Test
	public void testGetRollbackOnlyUnscopedNoCoord() {
		try {
			txControl.getRollbackOnly();
			fail("Should not be able to get rollback only");
		} catch (IllegalStateException e) {

		}
	}

	@Test
	public void testSetRollbackOnlyUnscopedNoCoord() {
		try {
			txControl.setRollbackOnly();
			fail("Should not be able to set rollback only");
		} catch (IllegalStateException e) {

		}
	}

	@Test
	public void testTranChecksUnscopedNoCoord() {
		assertFalse(txControl.activeTransaction());
		assertFalse(txControl.activeScope());
		assertNull(txControl.getCurrentContext());
	}

	private void setupExistingCoordination() {
		Mockito.when(coordinator.peek()).thenReturn(coordination1);
		Mockito.when(coordination1.getVariables()).thenReturn(variables1);
	}
	
	@Test
	public void testGetRollbackOnlyUnscopedWithCoordination() {
		setupExistingCoordination();
		
		try {
			txControl.getRollbackOnly();
			fail("Should not be able to get rollback only");
		} catch (IllegalStateException e) {

		}
	}

	@Test
	public void testSetRollbackOnlyUnscopedWithCoordination() {
		setupExistingCoordination();
		

		try {
			txControl.setRollbackOnly();
			fail("Should not be able to set rollback only");
		} catch (IllegalStateException e) {

		}
	}
	
	@Test
	public void testTranChecksUnscopedWithCoordination() {
		
		setupExistingCoordination();
		
		assertFalse(txControl.activeTransaction());
		assertFalse(txControl.activeScope());
		assertNull(txControl.getCurrentContext());
	}

	private void setupCoordinatorForSingleTransaction() {
		setupCoordinatorForSingleTransaction(null);
	}
	
	private void setupCoordinatorForSingleTransaction(Coordination existing) {
		
		Mockito.when(coordinator.peek()).thenReturn(existing);
		
		Mockito.when(coordinator.begin(Mockito.anyString(), Mockito.anyLong()))
			.then(i -> {
				Mockito.when(coordinator.peek()).thenReturn(coordination1);
				return coordination1;
			});
		
		
		Mockito.doAnswer(i -> Mockito.when(coordinator.peek()).thenReturn(existing))
			.when(coordination1).end();
		Mockito.doAnswer(i -> Mockito.when(coordinator.peek()).thenReturn(existing) != null)
			.when(coordination1).fail(Mockito.any(Throwable.class));
		
		Mockito.when(coordination1.getVariables()).thenReturn(variables1);
	}
	
	@Test
	public void testGetRollbackOnlyScoped() {
		setupCoordinatorForSingleTransaction();
		txControl.notSupported(() -> {
			Mockito.verify(coordination1).addParticipant(Mockito.any(Participant.class));
			try {
				txControl.getRollbackOnly();
				fail("Should not be able to get or set rollback when there is no transaction");
			} catch (IllegalStateException ise) {
			}
			return null;
		});
	}

	@Test
	public void testSetRollbackOnlyScoped() {
		setupCoordinatorForSingleTransaction();
		
		txControl.notSupported(() -> {
			Mockito.verify(coordination1).addParticipant(Mockito.any(Participant.class));
			try {
				txControl.setRollbackOnly();
				fail("Should not be able to get or set rollback when there is no transaction");
			} catch (IllegalStateException ise) {
			}
			return null;
		});
	}

	@Test
	public void testTranChecksScoped() {
		
		setupCoordinatorForSingleTransaction();
		txControl.notSupported(() -> {
			assertFalse(txControl.activeTransaction());
			assertTrue(txControl.activeScope());
			assertNotNull(txControl.getCurrentContext());
			assertEquals(NO_TRANSACTION, txControl.getCurrentContext().getTransactionStatus());
			
			return null;
		});
	}

	@Test
	public void testGetRollbackOnlyScopedExistingCoordination() {
		setupCoordinatorForSingleTransaction(coordination2);
		txControl.notSupported(() -> {
			Mockito.verify(coordination1).addParticipant(Mockito.any(Participant.class));
			try {
				txControl.getRollbackOnly();
				fail("Should not be able to get or set rollback when there is no transaction");
			} catch (IllegalStateException ise) {
			}
			return null;
		});
	}
	
	@Test
	public void testSetRollbackOnlyScopedExistingCoordination() {
		setupCoordinatorForSingleTransaction(coordination2);
		
		txControl.notSupported(() -> {
			Mockito.verify(coordination1).addParticipant(Mockito.any(Participant.class));
			try {
				txControl.setRollbackOnly();
				fail("Should not be able to get or set rollback when there is no transaction");
			} catch (IllegalStateException ise) {
			}
			return null;
		});
	}
	
	@Test
	public void testTranChecksScopedExistingCoordination() {
		
		setupCoordinatorForSingleTransaction(coordination2);
		txControl.notSupported(() -> {
			assertFalse(txControl.activeTransaction());
			assertTrue(txControl.activeScope());
			assertNotNull(txControl.getCurrentContext());
			assertEquals(NO_TRANSACTION, txControl.getCurrentContext().getTransactionStatus());
			
			return null;
		});
	}

	@Test
	public void testGetRollbackOnlyActive() {
		setupCoordinatorForSingleTransaction();
		txControl.required(() -> {
			Mockito.verify(coordination1).addParticipant(Mockito.any(Participant.class));
			assertFalse(txControl.getRollbackOnly());
			return null;
		});
	}
	
	@Test
	public void testSetRollbackOnlyActive() {
		setupCoordinatorForSingleTransaction();
		
		txControl.required(() -> {
			Mockito.verify(coordination1).addParticipant(Mockito.any(Participant.class));
			assertFalse(txControl.getRollbackOnly());
			txControl.setRollbackOnly();
			assertTrue(txControl.getRollbackOnly());
			
			return null;
		});
	}
	
	@Test
	public void testTranChecksActive() {
		
		setupCoordinatorForSingleTransaction();
		txControl.required(() -> {
			assertTrue(txControl.activeTransaction());
			assertTrue(txControl.activeScope());
			assertNotNull(txControl.getCurrentContext());
			assertEquals(ACTIVE, txControl.getCurrentContext().getTransactionStatus());

			txControl.setRollbackOnly();
			assertEquals(MARKED_ROLLBACK, txControl.getCurrentContext().getTransactionStatus());
			
			return null;
		});
	}
	
	@Test
	public void testGetRollbackOnlyActiveExistingCoordination() {
		setupCoordinatorForSingleTransaction(coordination2);
		txControl.required(() -> {
			Mockito.verify(coordination1).addParticipant(Mockito.any(Participant.class));
			assertFalse(txControl.getRollbackOnly());
			return null;
		});
	}
	
	@Test
	public void testSetRollbackOnlyActiveExistingCoordination() {
		setupCoordinatorForSingleTransaction(coordination2);
		
		txControl.required(() -> {
			Mockito.verify(coordination1).addParticipant(Mockito.any(Participant.class));
			assertFalse(txControl.getRollbackOnly());
			txControl.setRollbackOnly();
			assertTrue(txControl.getRollbackOnly());
			
			return null;
		});
	}
	
	@Test
	public void testTranChecksActiveExistingCoordination() {
		
		setupCoordinatorForSingleTransaction(coordination2);
		txControl.required(() -> {
			assertTrue(txControl.activeTransaction());
			assertTrue(txControl.activeScope());
			assertNotNull(txControl.getCurrentContext());
			assertEquals(ACTIVE, txControl.getCurrentContext().getTransactionStatus());

			txControl.setRollbackOnly();
			assertEquals(MARKED_ROLLBACK, txControl.getCurrentContext().getTransactionStatus());
			
			return null;
		});
	}
	
}
