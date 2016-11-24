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

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
class Pipe<I, O> {

	private Function<I, O> pipe;

	private Pipe(Function<I, O> fun) {
		this.pipe = fun;
	}

	public static <T> Pipe<T, T> create() {
		return new Pipe<>(x -> x);
	}

	public Consumer<I> getSource() {
		return i -> pipe.apply(i);
	}

	<U> Pipe<I, U> map(Function<O, U> fun) {
		this.pipe = (Function)this.pipe.andThen(fun);

		return (Pipe<I, U>)this;
	}

}
