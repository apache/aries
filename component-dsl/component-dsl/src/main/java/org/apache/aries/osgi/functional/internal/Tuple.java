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

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
class Tuple<T> implements Event<T> {

	public T t;
	private Deque<Runnable> _closingHandlers = new LinkedList<>();

	private Tuple(T t) {
		this(t, new LinkedList<>());
	}

	private Tuple(T t, Deque<Runnable> closingHandlers) {
		this.t = t;
		_closingHandlers = closingHandlers;
	}

	public <S> Tuple<S> map(Function<? super T, ? extends S> fun) {
		return new Tuple<>(fun.apply(t), _closingHandlers);
	}

	public static <T> Tuple<T> create(T t) {
		return new Tuple<>(t);
	}

	@Override
	public T getContent() {
		return t;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Tuple<?> tuple = (Tuple<?>) o;

		return t.equals(tuple.t);
	}

	@Override
	public int hashCode() {
		return t.hashCode();
	}

	public void onTermination(Runnable terminator) {
		_closingHandlers.push(terminator);
	}

	public void terminate() {
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
