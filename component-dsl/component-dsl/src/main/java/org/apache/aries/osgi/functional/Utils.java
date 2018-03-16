package org.apache.aries.osgi.functional;

import org.apache.aries.osgi.functional.internal.HighestRankingOSGi;
import org.apache.aries.osgi.functional.internal.OnlyLastPublisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.aries.osgi.functional.OSGi.all;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.nothing;

/**
 * @author Carlos Sierra Andrés
 */
public interface Utils {

    static <T> OSGi<List<T>> accumulate(OSGi<T> program) {
        return
            just(ArrayList<T>::new).flatMap(list ->
            all(
                just(ArrayList<T>::new),
                program.effects(list::add, list::remove).
                then(just(() -> new ArrayList<>(list)
                ))
            ).transform(
                op -> new OnlyLastPublisher(op, () -> new ArrayList<>(list)))
            );
    }

    static <K, V, T extends Comparable<T>> OSGi<Map<K, V>> accumulateMap(
        OSGi<T> program, Function<T, K> keyFun,
        Function<T, V> valueFun) {

        return just(HashMap::new).flatMap(map ->
            all(
                just(HashMap::new),
                program.splitBy(
                    keyFun,
                    p -> highest(p, Comparator.naturalOrder(), q -> nothing())).
                    effects(
                        t -> map.put(keyFun.apply(t), valueFun.apply(t)),
                        t -> map.remove(keyFun.apply(t))).
                    then(just(() -> new HashMap<>(map)))
            ).transform(
                op -> new OnlyLastPublisher(op, () -> new HashMap<>(map)
            )));
    }

    static <T> OSGi<T> onlyLast(OSGi<T> program) {
        return program.transform(OnlyLastPublisher::new);
    }

    static <T> OSGi<T> highest(
        OSGi<T> program, Comparator<? super T> comparator) {

        return highest(program, comparator, __ -> OSGi.nothing());
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
