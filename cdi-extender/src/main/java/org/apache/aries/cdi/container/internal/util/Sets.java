package org.apache.aries.cdi.container.internal.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Sets {

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