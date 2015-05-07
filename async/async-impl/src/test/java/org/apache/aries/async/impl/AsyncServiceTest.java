package org.apache.aries.async.impl;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.log.LogService;
import org.osgi.util.function.Predicate;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(MockitoJUnitRunner.class)
public class AsyncServiceTest {

	public static class DelayedEcho {
		public String echo(String s, int delay) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread interrupted", e);
			}
			if (s == null) throw new NullPointerException("Nothing to echo!");
			return s;
		}
	}
	
	private ExecutorService es;
	
	@Mock
	ServiceTracker<LogService, LogService> serviceTracker;
	
	@Before
	public void start() {
		es = Executors.newFixedThreadPool(3);
	}

	@After
	public void stop() {
		es.shutdownNow();
		try {
			es.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void test() throws InterruptedException {
		DelayedEcho raw = new DelayedEcho();
		
		AsyncService service = new AsyncService(null, es, 
				serviceTracker);
		
		DelayedEcho mediated = service.mediate(raw, DelayedEcho.class);
		
		Promise<String> promise = service.call(mediated.echo("Hello World", 1000));
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		promise.filter(new Predicate<String>() {
				@Override
				public boolean test(String t) {
					return "Hello World".equals(t);
				}
			}).then(new Success<String, Void>() {
				@Override
				public Promise<Void> call(Promise<String> resolved)
						throws Exception {
					latch.countDown();
					return null;
				}
			});
		
		assertTrue(latch.await(5, TimeUnit.SECONDS));
	}

}
