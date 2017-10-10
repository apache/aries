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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
class Tuple<T> implements Event<T>, SentEvent<T> {

	public T t;
	private Deque<Runnable> _closingHandlers = new LinkedList<>();
	private DoublyLinkedList<Tuple<?>> _relatedTuples =
		new DoublyLinkedList<>();
	private AtomicBoolean closed = new AtomicBoolean(false);
	private Event<T> cause = this;

	private Tuple(T t) {
		this(t, new LinkedList<>());
	}

	private Tuple(T t, Deque<Runnable> closingHandlers) {
		this.t = t;

		_closingHandlers = closingHandlers;
	}

	public void addRelatedTuple(Tuple<?> tuple) {
		if (closed.get()) {
			return;
		}

		DoublyLinkedList.Node<Tuple<?>> tupleNode = _relatedTuples.addLast(
			tuple);

		tuple.onTermination(tupleNode::remove);
	}

	public static <T> Tuple<T> create(T t) {
		return new Tuple<>(t);
	}

	@Override
	public T getContent() {
		return t;
	}

	@Override
	public int hashCode() {
		return t.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	public boolean isClosed() {
		return closed.get();
	}

	public <S> Tuple<S> map(Function<? super T, ? extends S> fun) {
		return new Tuple<>(fun.apply(t), _closingHandlers);
	}

	public void onTermination(Runnable terminator) {
		if (closed.get()) {
			return;
		}

		_closingHandlers.push(terminator);
	}

	public void setEvent(Event<T> event) {
		this.cause = event;
	}

	@Override
	public Event<T> getEvent() {
		return cause;
	}

	public void terminate() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}

		Iterator<Tuple<?>> iterator = _relatedTuples.iterator();

		while (iterator.hasNext()) {
			Tuple<?> next = iterator.next();

			iterator.remove();

			try {
				next.terminate();
			}
			catch (Exception e) {
				e.printStackTrace();
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
