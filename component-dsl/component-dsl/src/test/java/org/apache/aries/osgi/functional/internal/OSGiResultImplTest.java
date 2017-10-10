/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.osgi.functional.internal;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiResultImplTest {

	@Test
	public void testStartClose() {
		AtomicBoolean started = new AtomicBoolean();
		AtomicBoolean closed = new AtomicBoolean();

		OSGiResultImpl result = new OSGiResultImpl(set(started), set(closed));

		result.start();
		result.close();

		assertEquals(started.get(), closed.get());

		started = new AtomicBoolean();
		closed = new AtomicBoolean();

		result = new OSGiResultImpl(set(started), set(closed));

		result.close();
		result.start();

		assertEquals(started.get(), closed.get());
	}

	@Test
	public void testAsynchronousStartClose() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		AtomicBoolean started = new AtomicBoolean();
		AtomicBoolean closed = new AtomicBoolean();


		OSGiResultImpl result = new OSGiResultImpl(
			() -> {
				try {
					Thread.sleep(1000L);

					started.set(true);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			},
			set(closed)
		);

		executorService.execute(result::start);
		executorService.execute(result::close);

		executorService.awaitTermination(2, TimeUnit.SECONDS);

		assertEquals(started.get(), closed.get());
	}

	@Test
	@Ignore
	public void testAsynchronousManyStartClose() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(50);

		AtomicBoolean[] starteds = Stream.
			generate(AtomicBoolean::new).
			limit(1000).
			toArray(AtomicBoolean[]::new);

		AtomicBoolean[] closeds = Stream.
			generate(AtomicBoolean::new).
			limit(1000).
			toArray(AtomicBoolean[]::new);

		Random random = new Random(System.currentTimeMillis());

		for (int i = 0; i < starteds.length; i++) {
			AtomicBoolean started = starteds[i];
			AtomicBoolean closed = closeds[i];

			OSGiResultImpl result = new OSGiResultImpl(
				() -> {
					ignoreException(() -> Thread.sleep(random.nextInt(10)));

					started.set(true);
				},
				set(closed)
			);

			executorService.execute(
				() -> {
					ignoreException(() -> Thread.sleep(random.nextInt(2)));
					result.start();
				});
			executorService.execute(
				() -> {
					ignoreException(() -> Thread.sleep(random.nextInt(2)));
					result.close();
				});
		}

		executorService.shutdown();

		executorService.awaitTermination(100000, TimeUnit.MILLISECONDS);

		long count = Arrays.stream(starteds).filter(AtomicBoolean::get).count();

		assertTrue(count > 0);

		for (int i = 0; i < closeds.length; i++) {
			assertEquals(starteds[i].get(), closeds[i].get());
		}
	}

	private interface ExceptionalRunnable {
		void run() throws Exception;
	}

	private static void ignoreException(ExceptionalRunnable callable) {
		try {
			callable.run();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Runnable set(AtomicBoolean atomicBoolean) {
		return () -> atomicBoolean.set(true);
	}

}
