package org.osgi.util.function;

/**
 * A function that accepts a single argument and produces a result.
 * <p>
 * This is a functional interface and can be used as the assignment target for a lambda expression or method reference.
 *
 * @param <T> The type of the function input.
 * @param <R> The type of the function output.
 */
//@org.osgi.annotation.versioning.ConsumerType
public interface Function<T, R> {

    /**
     * Applies this function to the specified argument.
     * @param t The input to this function.
     * @return The output of this function.
     */
    R apply(T t);
}
