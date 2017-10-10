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
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
class Tuple<T> implements Event<T>, SentEvent<T> {

	public final T _t;
	private final Deque<Runnable> _closingHandlers = new LinkedList<>();
	private final DoublyLinkedList<Tuple<?>> _relatedTuples = new DoublyLinkedList<>();
	private volatile boolean closed = false;
	private Tuple<T> _cause = this;

	private Tuple(T t) {
		_t = t;
	}

	private Tuple(T t, Tuple<T> cause) {
		this(t);
		_cause = cause;
	}

	public void addRelatedTuple(Tuple<?> tuple) {
		DoublyLinkedList.Node<Tuple<?>> tupleNode = _relatedTuples.addLast(
			tuple);

		tuple.onTermination(tupleNode::remove);
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
		return closed;
	}

	public <S> Tuple<S> map(Function<? super T, ? extends S> fun) {
		Tuple<S> tuple = new Tuple<>(fun.apply(_t));

		addRelatedTuple(tuple);

		return tuple;
	}

	public void onTermination(Runnable terminator) {
		_closingHandlers.push(terminator);
	}

	@Override
	public Event<T> getEvent() {
		return _cause;
	}

	public Tuple<T> copy() {
		Tuple<T> copy = new Tuple<>(_t, this);

		addRelatedTuple(copy);

		return copy;
	}

	public void terminate() {
		closed = true;

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
