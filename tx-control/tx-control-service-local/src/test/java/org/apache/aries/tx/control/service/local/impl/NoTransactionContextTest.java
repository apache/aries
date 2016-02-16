package org.apache.aries.tx.control.service.local.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;

@RunWith(MockitoJUnitRunner.class)
public class NoTransactionContextTest {

	@Mock
	Coordination coordination;
	@Mock
	XAResource xaResource;
	@Mock
	LocalResource localResource;
	
	Map<Class<?>, Object> variables;
	
	TransactionContext ctx;
	
	@Before
	public void setUp() {
		ctx = new NoTransactionContextImpl(coordination);
		variables = new HashMap<>();
		Mockito.when(coordination.getVariables()).thenReturn(variables);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testGetRollbackOnly() {
		ctx.getRollbackOnly();
	}

	@Test(expected=IllegalStateException.class)
	public void testSetRollbackOnly() {
		ctx.setRollbackOnly();
	}

	@Test
	public void testTransactionKey() {
		assertNull(ctx.getTransactionKey());
	}
	
	@Test
	public void testTransactionStatus() {
		assertEquals(NO_TRANSACTION, ctx.getTransactionStatus());
	}

	@Test
	public void testLocalResourceSupport() {
		assertFalse(ctx.supportsLocal());
	}

	@Test
	public void testXAResourceSupport() {
		assertFalse(ctx.supportsXA());
	}

	@Test(expected=IllegalStateException.class)
	public void testLocalResourceRegistration() {
		ctx.registerLocalResource(localResource);
	}

	@Test(expected=IllegalStateException.class)
	public void testXAResourceRegistration() {
		ctx.registerXAResource(xaResource);
	}

	@Test
	public void testScopedValues() {
		assertNull(ctx.getScopedValue("foo"));
		
		Object value = new Object();
		
		ctx.putScopedValue("foo", value);
		
		assertSame(value, ctx.getScopedValue("foo"));
	}
	
	@Test
	public void testPreCompletionEnd() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> value.compareAndSet(1, 5));
		
		assertEquals(0, value.getAndSet(1));
		
		Participant participant = getParticipant();
		participant.ended(coordination);
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPreCompletionFail() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> value.compareAndSet(1, 5));
		
		assertEquals(0, value.getAndSet(1));
		
		Participant participant = getParticipant();
		participant.failed(coordination);
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletionEnd() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.postCompletion(status -> {
				assertEquals(NO_TRANSACTION, status);
				value.compareAndSet(1, 5);
			});
		
		assertEquals(0, value.getAndSet(1));
		
		Participant participant = getParticipant();
		participant.ended(coordination);
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletionFail() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.postCompletion(status -> {
			assertEquals(NO_TRANSACTION, status);
			value.compareAndSet(1, 5);
		});
		
		assertEquals(0, value.getAndSet(1));
		
		Participant participant = getParticipant();
		participant.failed(coordination);
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletionIsAfterPreCompletionEnd() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> value.compareAndSet(0, 3));
		ctx.postCompletion(status -> {
			assertEquals(NO_TRANSACTION, status);
			value.compareAndSet(3, 5);
		});
		
		Participant participant = getParticipant();
		participant.ended(coordination);
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletionIsAfterPreCompletionFail() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> value.compareAndSet(0, 3));
		ctx.postCompletion(status -> {
			assertEquals(NO_TRANSACTION, status);
			value.compareAndSet(3, 5);
		});
		
		Participant participant = getParticipant();
		participant.failed(coordination);
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletionIsStillCalledAfterPreCompletionException() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> {
				value.compareAndSet(0, 3);
				throw new RuntimeException("Boom!");
			});
		ctx.postCompletion(status -> {
			assertEquals(NO_TRANSACTION, status);
			value.compareAndSet(3, 5);
		});
		
		Participant participant = getParticipant();
		participant.ended(coordination);
		
		assertEquals(5, value.get());
	}

	private Participant getParticipant() {
		ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
		Mockito.verify(coordination).addParticipant(captor.capture());
		
		Participant participant = captor.getValue();
		return participant;
	}

	@Test(expected=IllegalStateException.class)
	public void testPreCompletionAfterEnd() throws Exception {
		
		Mockito.when(coordination.isTerminated()).thenReturn(true);
		ctx.preCompletion(() -> {});
	}

	@Test(expected=IllegalStateException.class)
	public void testPostCompletionAfterEnd() throws Exception {
		Mockito.when(coordination.isTerminated()).thenReturn(true);
		ctx.postCompletion(x -> {});
	}
	
}
