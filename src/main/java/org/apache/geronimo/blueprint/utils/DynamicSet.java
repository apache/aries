/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.blueprint.utils;

import java.util.Set;

/**
 * Same as DynamicCollection but implementing the Set interface, thus not allowing
 * duplicates.
 * Note that the insertion performance of this set is not very good due to the underlying
 * storage as a list which enforce a full traversal of the storage before adding any element. 
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766653 $, $Date: 2009-04-20 13:19:48 +0200 (Mon, 20 Apr 2009) $
 */
public class DynamicSet<E> extends DynamicCollection<E> implements Set<E> {

    @Override
    public boolean add(E o) {
        synchronized (lock) {
            if (!storage.contains(o)) {
                storage.add(o);
                return true;
            } else {
                return false;
            }
        }
    }

}
