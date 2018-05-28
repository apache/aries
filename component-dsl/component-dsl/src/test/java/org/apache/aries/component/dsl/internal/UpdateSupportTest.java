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

package org.apache.aries.component.dsl.internal;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Carlos Sierra Andrés
 */
public class UpdateSupportTest {

    @Test
    public void testDefer() {
        List<Integer> list = new ArrayList<>();

        UpdateSupport.runUpdate(() -> {
            list.add(1);

            UpdateSupport.defer(() -> list.add(3));

            list.add(2);
        });

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testDeferStack() {
        List<Integer> list = new ArrayList<>();

        UpdateSupport.runUpdate(() -> {
            list.add(1);

            UpdateSupport.defer(() -> list.add(6));

            UpdateSupport.runUpdate(() -> {
                list.add(2);

                UpdateSupport.runUpdate(() -> {
                    list.add(3);

                    UpdateSupport.defer(() -> list.add(4));
                });

                UpdateSupport.defer(() -> list.add(5));
            });
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), list);
    }


}
