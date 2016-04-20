package org.apache.aries.tx.control.service.local.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.transaction.control.TransactionStatus.ACTIVE;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTED;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTING;
import static org.osgi.service.transaction.control.TransactionStatus.MARKED_ROLLBACK;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLED_BACK;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLING_BACK;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
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
import org.osgi.service.transaction.control.TransactionException;

@RunWith(MockitoJUnitRunner.class)
public class TransactionContextTest {

	@Mock
	Coordination coordination;
	@Mock
	XAResource xaResource;
	@Mock
	LocalResource localResource;
	
	Map<Class<?>, Object> variables;
	
	AbstractTransactionContextImpl ctx;
	
	@Before
	public void setUp() {
		ctx = new TransactionContextImpl(coordination, false);
		variables = new HashMap<>();
		Mockito.when(coordination.getVariables()).thenReturn(variables);
	}
	
	@Test
	public void testGetRollbackOnly() {
		assertFalse(ctx.getRollbackOnly());
	}

	@Test
	public void testSetRollbackOnly() {
		ctx.setRollbackOnly();
		assertTrue(ctx.getRollbackOnly());
	}

	@Test
	public void testisReadOnlyFalse() {
		assertFalse(ctx.isReadOnly());
	}

	@Test
	public void testisReadOnlyTrue() {
		ctx = new TransactionContextImpl(coordination, true);
		assertTrue(ctx.isReadOnly());
	}

	@Test
	public void testTransactionKey() {
		Mockito.when(coordination.getId()).thenReturn(42L);
		
		assertNotNull(ctx.getTransactionKey());
	}
	
	@Test
	public void testTransactionStatus() {
		assertEquals(ACTIVE, ctx.getTransactionStatus());
		
		ctx.setRollbackOnly();
		
		assertEquals(MARKED_ROLLBACK, ctx.getTransactionStatus());
	}

	@Test
	public void testLocalResourceSupport() {
		assertTrue(ctx.supportsLocal());
	}

	@Test
	public void testXAResourceSupport() {
		assertFalse(ctx.supportsXA());
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
	public void testPreCompletion() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> {
			assertEquals(ACTIVE, ctx.getTransactionStatus());
			value.compareAndSet(1, 5);
		});
		
		assertEquals(0, value.getAndSet(1));
		
		ctx.finish();
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPreCompletionFail() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> {
			assertEquals(MARKED_ROLLBACK, ctx.getTransactionStatus());
			value.compareAndSet(1, 5);
		});
		
		assertEquals(0, value.getAndSet(1));
		
		
		ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
		Mockito.verify(coordination).addParticipant(captor.capture());
		
		captor.getValue().failed(coordination);
		
		ctx.finish();

		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletion() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.postCompletion(status -> {
				assertEquals(COMMITTED, status);
				value.compareAndSet(1, 5);
			});
		
		assertEquals(0, value.getAndSet(1));
		
		ctx.finish();
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletionFail() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.postCompletion(status -> {
			assertEquals(ROLLED_BACK, status);
			value.compareAndSet(1, 5);
		});
		
		assertEquals(0, value.getAndSet(1));
		
		ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
		Mockito.verify(coordination).addParticipant(captor.capture());
		
		captor.getValue().failed(coordination);
		
		ctx.finish();
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletionIsAfterPreCompletion() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> {
			assertEquals(ACTIVE, ctx.getTransactionStatus());
			value.compareAndSet(0, 3);
		});
		ctx.postCompletion(status -> {
			assertEquals(COMMITTED, status);
			value.compareAndSet(3, 5);
		});
		
		ctx.finish();
		
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
			assertEquals(ROLLED_BACK, status);
			value.compareAndSet(3, 5);
		});
		
		ctx.finish();
		
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
		
		ctx.finish();
		
		ctx.preCompletion(() -> {});
	}

	@Test(expected=IllegalStateException.class)
	public void testPreCompletionAfterFail() throws Exception {
		
		getParticipant().failed(coordination);
		
		ctx.finish();
		
		ctx.preCompletion(() -> {});
	}

	@Test(expected=IllegalStateException.class)
	public void testPostCompletionAfterEnd() throws Exception {
		
		ctx.finish();
		
		ctx.postCompletion(x -> {});
	}

	@Test(expected=IllegalStateException.class)
	public void testPostCompletionAfterFail() throws Exception {
		
		getParticipant().failed(coordination);
		
		ctx.finish();
		
		ctx.postCompletion(x -> {});
	}

	@Test
	public void testLocalResource() throws Exception {
		ctx.registerLocalResource(localResource);
		
		Mockito.doAnswer(i -> {
			assertEquals(COMMITTING, ctx.getTransactionStatus());
			return null;
		}).when(localResource).commit();
		
		ctx.finish();
		
		Mockito.verify(localResource).commit();
	}
	
	@Test
	public void testLocalResourceEarlyEnd() throws Exception {
		ctx.registerLocalResource(localResource);
		
		Mockito.doAnswer(i -> {
			assertEquals(ROLLING_BACK, ctx.getTransactionStatus());
			return null;
		}).when(localResource).rollback();
		
		getParticipant().ended(coordination);
		
		ctx.finish();
		
		Mockito.verify(localResource).rollback();
	}

	@Test
	public void testLocalResourceRollbackOnly() throws Exception {
		ctx.registerLocalResource(localResource);
		ctx.setRollbackOnly();
		
		Mockito.doAnswer(i -> {
			assertEquals(ROLLING_BACK, ctx.getTransactionStatus());
			return null;
		}).when(localResource).rollback();
		
		ctx.finish();
		
		Mockito.verify(localResource).rollback();
	}

	@Test
	public void testLocalResourceFail() throws Exception {
		ctx.registerLocalResource(localResource);
		
		Mockito.doAnswer(i -> {
			assertEquals(ROLLING_BACK, ctx.getTransactionStatus());
			return null;
		}).when(localResource).rollback();
		
		getParticipant().failed(coordination);
		
		ctx.finish();
		
		Mockito.verify(localResource).rollback();
	}
	
	@Test
	public void testLocalResourcePreCommitException() throws Exception {
		ctx.registerLocalResource(localResource);
		
		Mockito.doAnswer(i -> {
			assertEquals(ROLLING_BACK, ctx.getTransactionStatus());
			return null;
		}).when(localResource).rollback();
		
		ctx.preCompletion(() -> { throw new IllegalArgumentException(); });
		
		ctx.finish();
		
		Mockito.verify(localResource).rollback();
	}

	@Test
	public void testLocalResourcePostCommitException() throws Exception {
		ctx.registerLocalResource(localResource);
		
		Mockito.doAnswer(i -> {
			assertEquals(COMMITTING, ctx.getTransactionStatus());
			return null;
		}).when(localResource).commit();
		
		ctx.postCompletion(i -> { 
				assertEquals(COMMITTED, ctx.getTransactionStatus());
				throw new IllegalArgumentException(); 
			});
		
		ctx.finish();
		
		Mockito.verify(localResource).commit();
	}

	@Test
	public void testLocalResourcesFirstFailsSoRollback() throws Exception {
		
		ctx.registerLocalResource(localResource);

		LocalResource localResource2 = Mockito.mock(LocalResource.class);
		ctx.registerLocalResource(localResource2);
		
		Mockito.doAnswer(i -> {
			assertEquals(COMMITTING, ctx.getTransactionStatus());
			throw new TransactionException("Unable to commit");
		}).when(localResource).commit();

		Mockito.doAnswer(i -> {
			assertEquals(ROLLING_BACK, ctx.getTransactionStatus());
			return null;
		}).when(localResource2).rollback();
		
		ctx.finish();
		
		Mockito.verify(localResource).commit();
		Mockito.verify(localResource2).rollback();
	}
	
}
