package org.apache.aries.osgi.functional;

import org.apache.aries.osgi.functional.internal.AccumulateTransformer;
import org.apache.aries.osgi.functional.internal.HighestRankingOSGi;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public interface Utils {

    static <T> OSGi<List<T>> accumulate(OSGi<T> program) {
        return program.transform(new AccumulateTransformer<>());
    }

    static <T> OSGi<T> highest(
        OSGi<T> program, Comparator<? super T> comparator) {

        return highest(program, comparator, __ -> __);
    }

    static <T> OSGi<T> highest(
        OSGi<T> program, Comparator<? super T> comparator,
        Function<OSGi<T>, OSGi<T>> notHighest) {

        return new HighestRankingOSGi<>(program, comparator, notHighest);
    }

    static <T extends Comparable<? super T>> OSGi<T> highest(OSGi<T> program) {
        return highest(program, Comparator.naturalOrder());
    }

}
