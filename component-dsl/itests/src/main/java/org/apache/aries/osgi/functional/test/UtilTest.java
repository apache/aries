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

package org.apache.aries.osgi.functional.test;

import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.Arrays;
import java.util.List;

import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.Utils.accumulate;
import static org.apache.aries.osgi.functional.Utils.highest;

/**
 * @author Carlos Sierra Andrés
 */
public class UtilTest {

    static BundleContext bundleContext = FrameworkUtil.getBundle(
        UtilTest.class).getBundleContext();

    @Test
    public void testHighestsPer() {

        OSGi<List<String>> program = just(Arrays.asList(
            "apepe", "aana", "bvicente", "bcarlos", "cpepe", "ctomas"
        )).splitBy(
            x -> x.substring(0, 1),
            p -> accumulate(p).effects(
                t -> System.out.println("Incoming: " + t),
                t -> System.out.println("Leaving: " + t)
            )
        ).effects(
            t -> System.out.println("Incoming TOTAL: " + t),
            t -> System.out.println("Leaving TOTAL: " + t)
        );

        /*OSGi<List<String>> program = republishIf(
            (l1, l2) -> {
                System.out.println("Checking: " + l1 + " : " + l2);
                if (l1 == null) {
                    return true;
                }
                if (l2.isEmpty()) {
                    return true;
                }
                return !l1.subList(0, 1).equals(l2.subList(0, 1));
            },
            highestsPer(
                x -> x.substring(0, 1), Comparator.naturalOrder(),
                ))).
            effects(
                t -> System.out.println("Incoming: " + t),
                t -> System.out.println("Leaving: " + t)
            );
        */
        OSGiResult result = program.run(bundleContext);

        result.close();
    }


}
