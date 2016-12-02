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

import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
class Tuple<T> {

	public Object original;
	public T t;

	private Tuple(Object original, T t) {
		this.original = original;
		this.t = t;
	}

	public <S> Tuple<S> map(Function<? super T, ? extends S> fun) {
		return new Tuple<>(original, fun.apply(t));
	}

	public static <T> Tuple<T> create(T t) {
		return new Tuple<>(t, t);
	}

}
