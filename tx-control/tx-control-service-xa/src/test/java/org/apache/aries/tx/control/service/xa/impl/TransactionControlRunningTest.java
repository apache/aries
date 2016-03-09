package org.apache.aries.tx.control.service.xa.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTED;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLED_BACK;

import java.net.BindException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.xa.XAException;

import org.apache.aries.tx.control.service.xa.impl.TransactionControlImpl;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.ResourceProvider;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionStatus;

@RunWith(MockitoJUnitRunner.class)
public class TransactionControlRunningTest {

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
	public void setUp() throws XAException {
		variables1 = new HashMap<>();
		variables2 = new HashMap<>();

		setupCoordinations();

		txControl = new TransactionControlImpl(new GeronimoTransactionManager(), coordinator);
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
			ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
			Mockito.verify(coordination1, Mockito.atLeast(1)).addParticipant(captor.capture());
			
			for(Participant p : captor.getAllValues()) {
				p.ended(coordination1);
			}
			return null;
		}).when(coordination1).end();
		Mockito.doAnswer(i -> {
			Mockito.when(coordinator.peek()).thenReturn(null);
			ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
			Mockito.verify(coordination1, Mockito.atLeast(1)).addParticipant(captor.capture());
			
			for(Participant p : captor.getAllValues()) {
				p.failed(coordination1);
			}
			return null;
		}).when(coordination1).fail(Mockito.any(Throwable.class));

		Mockito.when(coordination2.getVariables()).thenReturn(variables2);
		Mockito.when(coordination2.getId()).thenReturn(43L);
		Mockito.doAnswer(i -> {
			Mockito.when(coordinator.peek()).thenReturn(coordination1);
			ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
			Mockito.verify(coordination2, Mockito.atLeast(1)).addParticipant(captor.capture());
			
			for(Participant p : captor.getAllValues()) {
				p.ended(coordination2);
			}
			return null;
		}).when(coordination2).end();
		Mockito.doAnswer(i -> {
			Mockito.when(coordinator.peek()).thenReturn(coordination1);
			ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
			Mockito.verify(coordination2, Mockito.atLeast(1)).addParticipant(captor.capture());
			
			for(Participant p : captor.getAllValues()) {
				p.failed(coordination2);
			}
			return null;
		}).when(coordination2).fail(Mockito.any(Throwable.class));
	}

	@Test
	public void testRequired() {

		AtomicReference<TransactionStatus> finalStatus = new AtomicReference<>();
		
		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			txControl.getCurrentContext().postCompletion(finalStatus::set);
			return null;
		});
		
		assertEquals(COMMITTED, finalStatus.get());

	}

	@Test
	public void testRequiredMarkedRollback() {
		
		AtomicReference<TransactionStatus> finalStatus = new AtomicReference<>();
		
		txControl.required(() -> {
			
			assertTrue(txControl.activeTransaction());
			
			txControl.getCurrentContext().postCompletion(finalStatus::set);
			
			txControl.setRollbackOnly();
			return null;
		});
		
		assertEquals(ROLLED_BACK, finalStatus.get());
	}

	@Test
	public void testRequiredUserException() {
		
		AtomicReference<TransactionStatus> finalStatus = new AtomicReference<>();
		
		Exception userEx = new Exception("Bang!");
		
		try {
			txControl.required(() -> {
				
				assertTrue(txControl.activeTransaction());
				
				txControl.getCurrentContext().postCompletion(finalStatus::set);
				
				throw userEx;
			});
			fail("Should not be reached");
		} catch (ScopedWorkException swe) {
			assertSame(userEx, swe.getCause());
		}
		
		assertEquals(ROLLED_BACK, finalStatus.get());
	}

	@Test
	public void testRequiredNoRollbackException() {
		
		AtomicReference<TransactionStatus> finalStatus = new AtomicReference<>();
		
		Exception userEx = new BindException("Bang!");
		
		try {
			txControl.build()
				.noRollbackFor(BindException.class)
				.required(() -> {
					
					assertTrue(txControl.activeTransaction());
					
					txControl.getCurrentContext().postCompletion(finalStatus::set);
					
					throw userEx;
				});
			fail("Should not be reached");
		} catch (ScopedWorkException swe) {
			assertSame(userEx, swe.getCause());
		}
		
		assertEquals(COMMITTED, finalStatus.get());
	}

	@Test
	public void testTwoRequiredsNested() {

		AtomicReference<TransactionStatus> finalStatusOuter = new AtomicReference<>();
		AtomicReference<TransactionStatus> finalStatusInner = new AtomicReference<>();
		
		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());
			
			Object key = txControl.getCurrentContext().getTransactionKey();

			txControl.getCurrentContext().postCompletion(finalStatusOuter::set);
			
			txControl.requiresNew(() -> {
					assertFalse(key.equals(txControl.getCurrentContext().getTransactionKey()));
					
					txControl.getCurrentContext().postCompletion(finalStatusInner::set);
					return null;
				});
			
			return null;
		});
		
		assertEquals(COMMITTED, finalStatusOuter.get());
		assertEquals(COMMITTED, finalStatusInner.get());

	}

	@Test
	public void testTwoRequiredsNestedOuterMarkedRollback() {
		
		AtomicReference<TransactionStatus> finalStatusOuter = new AtomicReference<>();
		AtomicReference<TransactionStatus> finalStatusInner = new AtomicReference<>();
		
		txControl.required(() -> {
			
			assertTrue(txControl.activeTransaction());
			
			Object key = txControl.getCurrentContext().getTransactionKey();
			
			txControl.getCurrentContext().postCompletion(finalStatusOuter::set);
			
			txControl.setRollbackOnly();
			
			txControl.requiresNew(() -> {
				assertFalse(key.equals(txControl.getCurrentContext().getTransactionKey()));
				
				txControl.getCurrentContext().postCompletion(finalStatusInner::set);
				return null;
			});
			
			return null;
		});
		
		assertEquals(ROLLED_BACK, finalStatusOuter.get());
		assertEquals(COMMITTED, finalStatusInner.get());
		
	}

	@Test
	public void testTwoRequiredsNestedInnerMarkedRollback() {
		
		AtomicReference<TransactionStatus> finalStatusOuter = new AtomicReference<>();
		AtomicReference<TransactionStatus> finalStatusInner = new AtomicReference<>();
		
		txControl.required(() -> {
			
			assertTrue(txControl.activeTransaction());
			
			Object key = txControl.getCurrentContext().getTransactionKey();
			
			txControl.getCurrentContext().postCompletion(finalStatusOuter::set);
			
			txControl.requiresNew(() -> {
				assertFalse(key.equals(txControl.getCurrentContext().getTransactionKey()));
				
				txControl.getCurrentContext().postCompletion(finalStatusInner::set);

				txControl.setRollbackOnly();
				
				return null;
			});
			
			return null;
		});
		
		assertEquals(COMMITTED, finalStatusOuter.get());
		assertEquals(ROLLED_BACK, finalStatusInner.get());
		
	}

	@Test
	public void testTwoRequiredsNestedBothMarkedRollback() {
		
		AtomicReference<TransactionStatus> finalStatusOuter = new AtomicReference<>();
		AtomicReference<TransactionStatus> finalStatusInner = new AtomicReference<>();
		
		txControl.required(() -> {
			
			assertTrue(txControl.activeTransaction());
			
			Object key = txControl.getCurrentContext().getTransactionKey();
			
			txControl.getCurrentContext().postCompletion(finalStatusOuter::set);
			
			txControl.setRollbackOnly();

			txControl.requiresNew(() -> {
				assertFalse(key.equals(txControl.getCurrentContext().getTransactionKey()));
				
				txControl.getCurrentContext().postCompletion(finalStatusInner::set);
				
				txControl.setRollbackOnly();
				
				return null;
			});
			
			return null;
		});
		
		assertEquals(ROLLED_BACK, finalStatusOuter.get());
		assertEquals(ROLLED_BACK, finalStatusInner.get());
		
	}
	
	@Test
	public void testTwoRequiredsNestedOuterThrowsException() {
		
		AtomicReference<TransactionStatus> finalStatusOuter = new AtomicReference<>();
		AtomicReference<TransactionStatus> finalStatusInner = new AtomicReference<>();
		
		Exception userEx = new Exception("Bang!");
		
		try {
			txControl.required(() -> {
				
				assertTrue(txControl.activeTransaction());
				
				Object key = txControl.getCurrentContext().getTransactionKey();
				
				txControl.getCurrentContext().postCompletion(finalStatusOuter::set);
				
				txControl.setRollbackOnly();
				
				txControl.requiresNew(() -> {
					assertFalse(key.equals(txControl.getCurrentContext().getTransactionKey()));
					
					txControl.getCurrentContext().postCompletion(finalStatusInner::set);
					return null;
				});
				
				throw userEx;
			});
			fail("Should not be reached");
		} catch (ScopedWorkException swe) {
			assertSame(userEx, swe.getCause());
		}
		
		assertEquals(ROLLED_BACK, finalStatusOuter.get());
		assertEquals(COMMITTED, finalStatusInner.get());
		
	}
	
	@Test
	public void testTwoRequiredsNestedInnerThrowsException() {
		
		AtomicReference<TransactionStatus> finalStatusOuter = new AtomicReference<>();
		AtomicReference<TransactionStatus> finalStatusInner = new AtomicReference<>();
		
		Exception userEx = new Exception("Bang!");
		
		txControl.required(() -> {
			
			assertTrue(txControl.activeTransaction());
			
			Object key = txControl.getCurrentContext().getTransactionKey();
			
			txControl.getCurrentContext().postCompletion(finalStatusOuter::set);
			
			try {
				txControl.requiresNew(() -> {
					assertFalse(key.equals(txControl.getCurrentContext().getTransactionKey()));
					
					txControl.getCurrentContext().postCompletion(finalStatusInner::set);
					
					txControl.setRollbackOnly();
					
					throw userEx;
				});
				fail("Should not be reached!");
			} catch (ScopedWorkException swe) {
				assertSame(userEx, swe.getCause());
			}
			return null;
		});
		
		assertEquals(COMMITTED, finalStatusOuter.get());
		assertEquals(ROLLED_BACK, finalStatusInner.get());
		
	}
	
	@Test
	public void testTwoRequiredsNestedNoRollbackForInnerException() {
		
		AtomicReference<TransactionStatus> finalStatusOuter = new AtomicReference<>();
		AtomicReference<TransactionStatus> finalStatusInner = new AtomicReference<>();
		
		Exception userEx = new BindException("Bang!");
		
		try {
			txControl.required(() -> {
				
				assertTrue(txControl.activeTransaction());
				
				Object key = txControl.getCurrentContext().getTransactionKey();
				
				txControl.getCurrentContext().postCompletion(finalStatusOuter::set);
				
				try {
					txControl.build()
						.noRollbackFor(BindException.class)
						.requiresNew(() -> {
								assertFalse(key.equals(txControl.getCurrentContext().getTransactionKey()));
								
								txControl.getCurrentContext().postCompletion(finalStatusInner::set);
								
								throw userEx;
							});
					fail("Should not be reached!");
				} catch (ScopedWorkException swe) {
					throw swe.as(BindException.class);
				}
				
				return null;
			});
			fail("Should not be reached!");
		} catch (ScopedWorkException swe) {
			assertSame(userEx, swe.getCause());
		}
		
		assertEquals(ROLLED_BACK, finalStatusOuter.get());
		assertEquals(COMMITTED, finalStatusInner.get());
		
	}
	
}
