package org.apache.aries.osgi.functional;

import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public interface Transformer<T, R> extends
    Function<Function<R, Runnable>, Function<T, Runnable>> {
}
