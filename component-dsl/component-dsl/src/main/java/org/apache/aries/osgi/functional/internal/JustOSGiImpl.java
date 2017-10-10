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


import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Carlos Sierra Andrés
 */
public class JustOSGiImpl<T> extends OSGiImpl<T> {

	public JustOSGiImpl(Collection<T> t) {
		this(() -> t);
	}

	public JustOSGiImpl(Supplier<Collection<T>> t) {
		super((bundleContext, op) -> {

			Collection<Tuple<T>> references =
				t.get().stream().map(Tuple::create).collect(
					Collectors.toList());

			return new OSGiResultImpl(
				() -> references.forEach(op),
				() -> references.forEach(Tuple::terminate));
		});

	}

	public JustOSGiImpl(T t) {
		this(() -> Collections.singletonList(t));
	}

}
