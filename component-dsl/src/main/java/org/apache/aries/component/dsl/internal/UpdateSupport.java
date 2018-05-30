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

import java.util.Deque;
import java.util.LinkedList;

/**
 * @author Carlos Sierra Andrés
 */
public class UpdateSupport {

    private static final ThreadLocal<Deque<Deque<Runnable>>>
        deferredTerminatorsStack = ThreadLocal.withInitial(LinkedList::new);
    private static final ThreadLocal<Boolean> isUpdate =
        ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static void defer(Runnable runnable) {
        if (isUpdate.get()) {
            deferredTerminatorsStack.get().peekLast().addLast(runnable);
        }
        else {
            runnable.run();
        }
    }

    public static void runUpdate(Runnable runnable) {
        isUpdate.set(true);

        Deque<Deque<Runnable>> deferred = deferredTerminatorsStack.get();

        deferred.addLast(new LinkedList<>());

        try {
            runnable.run();
        }
        finally {
            isUpdate.set(false);

            Deque<Runnable> terminators =
                deferredTerminatorsStack.get().removeLast();

            for (Runnable terminator : terminators) {
                terminator.run();
            }
        }
    }
}
