package org.apache.aries.osgi.functional;

import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public interface Transformer<T, R> extends
    Function<Publisher<R>, Publisher<T>> {

    @Override
    default Publisher<T> apply(Publisher<R> pipe) {
        return transform(pipe);
    }

    Publisher<T> transform(Publisher<R> pipe);

}
