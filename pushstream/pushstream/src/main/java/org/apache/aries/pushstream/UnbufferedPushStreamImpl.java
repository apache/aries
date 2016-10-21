/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.pushstream;

import static java.util.Optional.ofNullable;
import static org.apache.aries.pushstream.AbstractPushStreamImpl.State.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushEventConsumer;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;

public class UnbufferedPushStreamImpl<T, U extends BlockingQueue<PushEvent< ? extends T>>>
	extends AbstractPushStreamImpl<T> implements PushStream<T> {
	
	protected final Function<PushEventConsumer<T>,AutoCloseable>	connector;
	
	protected final AtomicReference<AutoCloseable>					upstream	= new AtomicReference<AutoCloseable>();
	
	public UnbufferedPushStreamImpl(PushStreamProvider psp,
			Executor executor, ScheduledExecutorService scheduler,
			Function<PushEventConsumer<T>,AutoCloseable> connector) {
		super(psp, executor, scheduler);
		this.connector = connector;
	}

	@Override
	protected boolean close(PushEvent<T> event) {
		if(super.close(event)) {
			ofNullable(upstream.getAndSet(() -> {
				// This block doesn't need to do anything, but the presence
				// of the Closable is needed to prevent duplicate begins
			})).ifPresent(c -> {
					try {
						c.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			return true;
		}
		return false;
	}

	@Override
	protected boolean begin() {
		if(closed.compareAndSet(BUILDING, STARTED)) {
			AutoCloseable toClose = connector.apply(this::handleEvent);
			if(!upstream.compareAndSet(null,toClose)) {
				//TODO log that we tried to connect twice...
				try {
					toClose.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (closed.get() == CLOSED
					&& upstream.compareAndSet(toClose, null)) {
				// We closed before setting the upstream - close it now
				try {
					toClose.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}
		return false;
	}
}
