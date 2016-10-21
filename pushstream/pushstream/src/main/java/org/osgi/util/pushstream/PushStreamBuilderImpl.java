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
package org.osgi.util.pushstream;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

class PushStreamBuilderImpl<T, U extends BlockingQueue<PushEvent< ? extends T>>>
		extends AbstractBufferBuilder<PushStream<T>,T,U>
		implements PushStreamBuilder<T,U> {

	private final PushStreamProvider	psp;
	private final PushEventSource<T>		eventSource;
	private final Executor					previousExecutor;

	private boolean							unbuffered;

	PushStreamBuilderImpl(PushStreamProvider psp, Executor defaultExecutor,
			PushEventSource<T> eventSource) {
		this.psp = psp;
		this.previousExecutor = defaultExecutor;
		this.eventSource = eventSource;
		this.worker = defaultExecutor;
	}

	@Override
	public PushStreamBuilder<T,U> withBuffer(U queue) {
		unbuffered = false;
		return (PushStreamBuilder<T,U>) super.withBuffer(queue);
	}

	@Override
	public PushStreamBuilder<T,U> withQueuePolicy(
			QueuePolicy<T,U> queuePolicy) {
		unbuffered = false;
		return (PushStreamBuilder<T,U>) super.withQueuePolicy(queuePolicy);
	}

	@Override
	public PushStreamBuilder<T,U> withQueuePolicy(
			QueuePolicyOption queuePolicyOption) {
		unbuffered = false;
		return (PushStreamBuilder<T,U>) super.withQueuePolicy(
				queuePolicyOption);
	}

	@Override
	public PushStreamBuilder<T,U> withPushbackPolicy(
			PushbackPolicy<T,U> pushbackPolicy) {
		unbuffered = false;
		return (PushStreamBuilder<T,U>) super.withPushbackPolicy(
				pushbackPolicy);
	}

	@Override
	public PushStreamBuilder<T,U> withPushbackPolicy(
			PushbackPolicyOption pushbackPolicyOption, long time) {
		unbuffered = false;
		return (PushStreamBuilder<T,U>) super.withPushbackPolicy(
				pushbackPolicyOption, time);
	}

	@Override
	public PushStreamBuilder<T,U> withParallelism(int parallelism) {
		unbuffered = false;
		return (PushStreamBuilder<T,U>) super.withParallelism(parallelism);
	}

	@Override
	public PushStreamBuilder<T,U> withExecutor(Executor executor) {
		unbuffered = false;
		return (PushStreamBuilder<T,U>) super.withExecutor(executor);
	}

	@Override
	public PushStreamBuilder<T,U> unbuffered() {
		unbuffered = true;
		return this;
	}

	@Override
	public PushStream<T> create() {
		if (unbuffered) {
			return psp.createUnbufferedStream(eventSource, previousExecutor);
		} else {
			return psp.createStream(eventSource, concurrency, worker, buffer,
					bufferingPolicy, backPressure);
		}
	}
}
