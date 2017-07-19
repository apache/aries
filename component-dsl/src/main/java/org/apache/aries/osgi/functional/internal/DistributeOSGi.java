/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.apache.aries.osgi.functional.internal;

import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Carlos Sierra Andrés
 */
public class DistributeOSGi extends OSGiImpl<Void> {

    public DistributeOSGi(OSGi<?>... programs) {
        super(bundleContext -> {
            Pipe<Tuple<Void>, Tuple<Void>> added = Pipe.create();

            Consumer<Tuple<Void>> addedSource = added.getSource();

            List<OSGiResult<?>> results = new ArrayList<>();

            Pipe<Tuple<Void>, Tuple<Void>> removed = Pipe.create();

            Consumer<Tuple<Void>> removedSource = removed.getSource();

            return new OSGiResultImpl<>(
                added, removed,
                () -> {
                    results.addAll(
                        Arrays.stream(programs).
                            map(o -> o.run(bundleContext)).
                            collect(Collectors.toList()));

                    addedSource.accept(Tuple.create(null));
                },
                () -> {
                    removedSource.accept(Tuple.create(null));

                    results.forEach(OSGiResult::close);
                }
            );
        });
    }
}
