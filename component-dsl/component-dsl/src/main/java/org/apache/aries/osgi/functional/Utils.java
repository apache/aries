package org.apache.aries.osgi.functional;

import org.apache.aries.osgi.functional.internal.ConcurrentDoublyLinkedList;
import org.apache.aries.osgi.functional.internal.HighestRankingTransformer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static org.apache.aries.osgi.functional.OSGi.NOOP;

/**
 * @author Carlos Sierra Andrés
 */
public interface Utils {

    static <T extends Comparable<? super T>> OSGi<T> highest(OSGi<T> program) {
        return program.transform(
            new HighestRankingTransformer<>(Comparator.naturalOrder()));
    }

    static <T> OSGi<T> highest(
        Comparator<? super T> comparator, OSGi<T> program) {

        return program.transform(new HighestRankingTransformer<>(comparator));
    }

    static <T> OSGi<List<T>> accumulate(OSGi<T> program) {
        return program.transform(op -> {
            ConcurrentDoublyLinkedList<T> list =
                new ConcurrentDoublyLinkedList<>();

            AtomicReference<Runnable> terminator = new AtomicReference<>(NOOP);

            return t -> {
                ConcurrentDoublyLinkedList.Node node = list.addLast(t);

                publish(op, list, terminator);

                return () -> {
                    node.remove();

                    publish(op, list, terminator);
                };
            };
        });
    }

    static <T> void publish(Function<List<T>, Runnable> op, ConcurrentDoublyLinkedList<T> list, AtomicReference<Runnable> terminator) {
        Runnable runnable = terminator.get();

        runnable.run();

        terminator.set(op.apply(new ArrayList<>(list)));
    }

    static <T> OSGi<T> republishIf(
        BiPredicate<T, T> refresher, OSGi<T> program) {

        return program.transform(op -> {
            AtomicReference<T> old = new AtomicReference<>();
            AtomicReference<Runnable> terminator = new AtomicReference<>(NOOP);

            return t -> {
                if (refresher.test(old.get(), t)) {
                    terminator.get().run();

                    old.set(t);
                    terminator.set(op.apply(t));
                }

                return () -> {};
            };
        });
    }

}
