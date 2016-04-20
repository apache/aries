package org.apache.aries.tx.control.service.common.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.transaction.control.LocalResource;

@RunWith(MockitoJUnitRunner.class)
public class NoTransactionContextTest {

	@Mock
	XAResource xaResource;
	@Mock
	LocalResource localResource;
	
	AbstractTransactionContextImpl ctx;
	
	@Before
	public void setUp() {
		ctx = new NoTransactionContextImpl();
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
	public void testPreCompletion() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> value.compareAndSet(1, 5));
		
		assertEquals(0, value.getAndSet(1));
		
		ctx.finish();
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPreCompletionFail() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> value.compareAndSet(1, 5));
		
		assertEquals(0, value.getAndSet(1));
		
		ctx.recordFailure(new Exception());
		
		ctx.finish();
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletion() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.postCompletion(status -> {
				assertEquals(NO_TRANSACTION, status);
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
			assertEquals(NO_TRANSACTION, status);
			value.compareAndSet(1, 5);
		});
		
		assertEquals(0, value.getAndSet(1));
		
		ctx.recordFailure(new Exception());
		
		ctx.finish();
		
		assertEquals(5, value.get());
	}

	@Test
	public void testPostCompletionIsAfterPreCompletion() throws Exception {
		
		AtomicInteger value = new AtomicInteger(0);
		
		ctx.preCompletion(() -> value.compareAndSet(0, 3));
		ctx.postCompletion(status -> {
			assertEquals(NO_TRANSACTION, status);
			value.compareAndSet(3, 5);
		});
		
		ctx.finish();
		
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
		
		ctx.recordFailure(new Exception());
		
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
			assertEquals(NO_TRANSACTION, status);
			value.compareAndSet(3, 5);
		});
		
		ctx.finish();
		
		assertEquals(5, value.get());
	}

	@Test(expected=IllegalStateException.class)
	public void testPreCompletionAfterEnd() throws Exception {
		ctx.finish();
		ctx.preCompletion(() -> {});
	}

	@Test(expected=IllegalStateException.class)
	public void testPostCompletionAfterEnd() throws Exception {
		ctx.finish();
		ctx.postCompletion(x -> {});
	}
	
}
