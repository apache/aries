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
package org.apache.aries.blueprint.utils;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DynamicCollectionTest {

    protected static final Object O0 = new Object();
    protected static final Object O1 = new Object();
    protected static final Object O2 = new Object();
    protected static final Object O3 = new Object();

    protected DynamicCollection<Object> collection;

    @Before
    public void setUp() {
        collection = new DynamicCollection<Object>();
    }

    @Test
    public void testAddRemove() throws Exception {
        assertEquals(0, collection.size());
        assertTrue(collection.isEmpty());
        collection.add(O0);
        assertEquals(1, collection.size());
        assertFalse(collection.isEmpty());
        assertTrue(collection.contains(O0));
        assertFalse(collection.contains(O1));
        collection.clear();
        assertEquals(0, collection.size());
        collection.add(O0);
        collection.add(O0);
        assertEquals(2, collection.size());
        assertTrue(collection.remove(O0));
        assertEquals(1, collection.size());
        assertTrue(collection.remove(O0));
        assertEquals(0, collection.size());
    }

    @Test
    public void testSimpleIterator() throws Exception {
        collection.add(O0);

        Iterator iterator = collection.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(O0, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAddWhileIterating() throws Exception {
        Iterator iterator = collection.iterator();
        assertFalse(iterator.hasNext());

        collection.add(O0);
        assertTrue(iterator.hasNext());
        assertEquals(O0, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testRemoveElementWhileIterating() throws Exception {
        collection.add(O0);
        collection.add(O1);

        Iterator iterator = collection.iterator();
        assertTrue(iterator.hasNext());
        collection.remove(O0);
        assertEquals(O0, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(O1, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testRemoveElementAfterWhileIterating() throws Exception {
        collection.add(O0);
        collection.add(O1);

        Iterator iterator = collection.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(O0, iterator.next());
        collection.remove(O1);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testRemoveElementBeforeWhileIterating() throws Exception {
        collection.add(O0);
        collection.add(O1);

        Iterator iterator = collection.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(O0, iterator.next());
        collection.remove(O0);
        assertTrue(iterator.hasNext());
        assertEquals(O1, iterator.next());
        assertFalse(iterator.hasNext());
    }

}
