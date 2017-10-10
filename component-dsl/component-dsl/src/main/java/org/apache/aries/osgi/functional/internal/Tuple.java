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

import org.apache.aries.osgi.functional.Event;
import org.apache.aries.osgi.functional.SentEvent;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
class Tuple<T> implements Event<T> {

	public final T _t;
	private final Deque<Runnable> _closingHandlers =
		new ConcurrentLinkedDeque<>();
	private final ConcurrentDoublyLinkedList<Tuple<?>> _relatedTuples =
		new ConcurrentDoublyLinkedList<>();
	private AtomicBoolean _closed = new AtomicBoolean();
	private AtomicBoolean _working = new AtomicBoolean();

	private Tuple(T t) {
		_t = t;
	}

	public void addRelatedTuple(Tuple<?> tuple) {
		while (!_working.compareAndSet(false, true)) {
			Thread.yield();
		}
		try {
			if (_closed.get()) {
                tuple.terminate();

                return;
            }

			ConcurrentDoublyLinkedList.Node node = _relatedTuples.addLast(tuple);

			tuple.onTermination(node::remove);
		}
		finally {
			_working.set(false);
		}
	}

	public static <T> Tuple<T> create(T t) {
		return new Tuple<>(t);
	}

	@Override
	public T getContent() {
		return _t;
	}

	@Override
	public int hashCode() {
		return _t.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	public boolean isClosed() {
		return _closed.get();
	}

	public <S> Tuple<S> map(Function<? super T, ? extends S> fun) {
		Tuple<S> tuple = new Tuple<>(fun.apply(_t));

		addRelatedTuple(tuple);

		return tuple;
	}

	public void onTermination(Runnable terminator) {
		while (!_working.compareAndSet(false, true)) {
			Thread.yield();
		}
		try {
			if (_closed.get()) {
                terminator.run();

                return;
            }

			_closingHandlers.push(terminator);
		}
		finally {
			_working.set(false);
		}

	}

	public void terminate() {
		while (!_working.compareAndSet(false, true)) {
		}
		try {
			if (!_closed.compareAndSet(false, true)) {
                return;
            }
		}
		finally {
			_working.set(false);
		}

		Iterator<Tuple<?>> iterator = _relatedTuples.iterator();

		while (iterator.hasNext()) {
			Tuple<?> next = iterator.next();

			iterator.remove();

			try {
				next.terminate();
			}
			catch (Exception e) {
			}
		}

		while (!_closingHandlers.isEmpty()) {
			Runnable pop = _closingHandlers.pop();

			try {
				pop.run();
			}
			catch (Exception e) {
			}
		}

	}

}
