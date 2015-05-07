package org.osgi.util.function;

/**
 * A predicate that accepts a single argument and produces a boolean result.
 * <p>
 * This is a functional interface and can be used as the assignment target for a lambda expression or method reference.
 *
 * @param <T> The type of the predicate input.
 */
//@org.osgi.annotation.versioning.ConsumerType
public interface Predicate<T> {
    /**
     * Evaluates this predicate on the specified argument.
     *
     * @param t The input to this predicate.
     * @return true if the specified argument is accepted by this predicate; false otherwise.
     */
    boolean test(T t);
}
