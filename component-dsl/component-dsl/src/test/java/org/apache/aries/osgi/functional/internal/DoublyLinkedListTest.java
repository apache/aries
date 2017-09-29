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

package org.apache.aries.osgi.functional.internal;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Carlos Sierra Andrés
 */
public class DoublyLinkedListTest {

    @Test
    public void testAddBeforeFromNode() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        DoublyLinkedList.Node<Integer> first = list.addFirst(5);

        DoublyLinkedList.Node<Integer> previous = first.addPrevious(4);

        assertEquals(previous.getValue(), list.getFirst());
        assertEquals(first.getValue(), list.getLast());

        previous.addPrevious(3);

        assertEquals(3, (int)list.getFirst());
    }

    @Test
    public void testAddFirst() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        DoublyLinkedList.Node<Integer> first = list.addFirst(5);

        assertEquals(first.getValue(), list.getFirst());
        assertEquals(first.getValue(), list.getLast());
    }

    @Test
    public void testAddLast() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        DoublyLinkedList.Node<Integer> last = list.addLast(5);

        assertEquals(last.getValue(), list.getFirst());
        assertEquals(last.getValue(), list.getLast());
    }

    @Test
    public void testAddNextFromNode() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        DoublyLinkedList.Node<Integer> first = list.addFirst(5);

        DoublyLinkedList.Node<Integer> second = first.addNext(6);

        assertEquals(first.getValue(), list.getFirst());
        assertEquals(second.getValue(), list.getLast());

        second.addNext(7);

        assertEquals(7, (int)list.getLast());
    }

    @Test
    public void testGetNextWithOneElement() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        DoublyLinkedList.Node<Integer> last = list.addLast(5);

        last = list.getFirstNode();

        DoublyLinkedList.Node<Integer> next = last.getNext();

        assertNull(next);
    }

    @Test
    public void testIterable() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        list.addFirst(3);
        list.addFirst(2);
        list.addFirst(1);

        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(2, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(3, (int)iterator.next());
    }

    @Test
    public void testIteratorRemoveOnlyOneElement() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        list.addFirst(3);

        Iterator<Integer> iterator = list.iterator();

        iterator.next();

        iterator.remove();

        assertEquals(0, list.size());
    }

    @Test
    public void testRemove() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        list.addFirst(3);
        DoublyLinkedList.Node<Integer> node = list.addFirst(2);
        list.addFirst(1);

        node.remove();

        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(3, (int)iterator.next());
    }

    @Test
    public void testRemoveFirst() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        list.addFirst(3);
        list.addFirst(2);
        DoublyLinkedList.Node<Integer> node = list.addFirst(1);

        assertEquals(list.getFirst(), node.getValue());

        node.remove();

        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(2, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(3, (int)iterator.next());
    }

    @Test
    public void testRemoveFirstFromIterator() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        list.addFirst(3);
        list.addFirst(2);
        list.addFirst(1);

        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1, (int)iterator.next());
        iterator.remove();
        assertTrue(iterator.hasNext());
        assertEquals(2, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(3, (int)iterator.next());

        iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(2, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(3, (int)iterator.next());
    }

    @Test
    public void testRemoveFromIterator() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        list.addFirst(3);
        list.addFirst(2);
        list.addFirst(1);

        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(2, (int)iterator.next());
        iterator.remove();
        assertTrue(iterator.hasNext());
        assertEquals(3, (int)iterator.next());

        iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(3, (int)iterator.next());
    }

    @Test
    public void testRemoveLast() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        DoublyLinkedList.Node<Integer> node = list.addFirst(3);
        list.addFirst(2);
        list.addFirst(1);

        assertEquals(list.getLast(), node.getValue());

        node.remove();

        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(2, (int)iterator.next());
    }

    @Test
    public void testRemoveLastFromIterator() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();

        list.addFirst(3);
        list.addFirst(2);
        list.addFirst(1);

        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(2, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(3, (int)iterator.next());
        iterator.remove();

        iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1, (int)iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(2, (int)iterator.next());
    }

}
