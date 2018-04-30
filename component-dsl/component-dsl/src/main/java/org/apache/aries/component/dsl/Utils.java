/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.component.dsl;

import org.apache.aries.component.dsl.internal.HighestRankingOSGi;
import org.apache.aries.component.dsl.internal.OnlyLastPublisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.aries.component.dsl.OSGi.just;

/**
 * @author Carlos Sierra Andrés
 */
public interface Utils {

    static <T> OSGi<List<T>> accumulate(OSGi<T> program) {
        return
            OSGi.just(ArrayList<T>::new).flatMap(list ->
            OSGi.all(
                OSGi.just(ArrayList<T>::new),
                program.effects(list::add, list::remove).
                then(OSGi.just(() -> new ArrayList<>(list)
                ))
            ).transform(
                op -> new OnlyLastPublisher(op, () -> new ArrayList<>(list)))
            );
    }

    static <K, V, T extends Comparable<T>> OSGi<Map<K, V>> accumulateInMap(
        OSGi<T> program, Function<T, OSGi<K>> keyFun,
        Function<T, OSGi<V>> valueFun) {

        return OSGi.just(HashMap<K, V>::new).flatMap(map ->
            OSGi.all(
                OSGi.just(HashMap::new),
                program.splitBy(
                    keyFun,
                    (k, p) ->
                        highest(p, Comparator.naturalOrder(), q -> OSGi.nothing()).
                            flatMap(t ->
                                valueFun.apply(t).effects(
                                    v -> map.put(k, v),
                                    __ -> map.remove(k)
                                )
                            )
                ).then(OSGi.just(() -> new HashMap<>(map)))
            ).transform(
                op -> new OnlyLastPublisher(op, () -> new HashMap<>(map))
            )
        );
    }

    static <T> OSGi<T> onlyLast(OSGi<T> program) {
        return program.transform(o -> new OnlyLastPublisher<>(o));
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
