package org.apache.aries.osgi.functional;

import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public interface Transformer<T, R> extends
    Function<Publisher<? super R>, Publisher<? extends T>> {

    @Override
    default Publisher<? extends T> apply(Publisher<? super R> pipe) {
        return transform(pipe);
    }

    Publisher<T> transform(Publisher<? super R> pipe);

}
