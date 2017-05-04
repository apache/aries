/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Sets {

	private Sets() {
		// no instances
	}

	@SafeVarargs
	public static <T> Set<T> hashSet(T ... elements) {
		return hashSet0(new HashSet<>(), elements);
	}

	@SafeVarargs
	public static <T> Set<T> hashSet(Set<T> from, T ... elements) {
		return hashSet0(new HashSet<>(from), elements);
	}

	@SafeVarargs
	public static <T> Set<T> immutableHashSet(T ... elements) {
		return immutableHashSet0(new HashSet<>(), elements);
	}

	@SafeVarargs
	public static <T> Set<T> immutableHashSet(Set<T> from, T ... elements) {
		return immutableHashSet0(new HashSet<>(from), elements);
	}

	@SafeVarargs
	private static <T> Set<T> hashSet0(Set<T> set, T ... elements) {
		for (T t : elements) {
			set.add(t);
		}

		return set;
	}

	@SafeVarargs
	private static <T> Set<T> immutableHashSet0(Set<T> set, T ... elements) {
		return Collections.unmodifiableSet(hashSet0(set, elements));
	}

}