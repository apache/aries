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

import java.util.Iterator;

/**
 * @author Carlos Sierra Andrés
 */
public class DoublyLinkedList<E> implements Iterable<E> {

    private int size = 0;
    private NodeImpl head;
    private NodeImpl last;

    public DoublyLinkedList() {
        head = last = null;
    }

    public synchronized Node<E> addFirst(E element) {
        if (head == null) {
            head = last = new NodeImpl(element, null, null);
        }
        else {
            head = new NodeImpl(element, null, head);
            head._next._prev = head;
        }

        size++;

        return head;
    }

    public synchronized Node<E> addLast(E element) {
        if (last == null) {
            head = last = new NodeImpl(element, null, null);
        }
        else {
            last = new NodeImpl(element, last, null);
            last._prev._next = last;
        }

        size++;

        return last;
    }

    public synchronized E getFirst() {
        return head.getValue();
    }

    public synchronized Node<E> getFirstNode() {
        return head;
    }

    public synchronized E getLast() {
        return last.getValue();
    }

    public synchronized Node<E> getLastNode() {
        return last;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            NodeImpl current = new NodeImpl(null, null, head);

            @Override
            public boolean hasNext() {
                synchronized (DoublyLinkedList.this) {
                    return current._next != null;
                }
            }

            @Override
            public E next() {
                synchronized (DoublyLinkedList.this) {
                    current = current._next;

                    return current.getValue();
                }
            }

            @Override
            public void remove() {
                synchronized (DoublyLinkedList.this) {
                    NodeImpl next = current._next;

                    current.remove();

                    current = new NodeImpl(null, null, next);
                }
            }

        };
    }

    public synchronized int size() {
        return size;
    }

    public interface Node<E> {
        Node<E> addNext(E element);

        Node<E> addPrevious(E element);

        Node<E> getNext();

        Node<E> getPrevious();

        E getValue();

        boolean remove();
    }

    private class NodeImpl implements Node<E> {

        E _value;
        NodeImpl _prev;
        NodeImpl _next;

        public NodeImpl(E value, NodeImpl prev, NodeImpl next) {
            _value = value;
            _prev = prev;
            _next = next;
        }

        @Override
        public Node<E> addNext(E element) {
            synchronized (DoublyLinkedList.this) {
                _next = new NodeImpl(element, this, _next);

                if (_next._next == null) {
                    last = _next;
                }

                return _next;
            }
        }

        @Override
        public Node<E> addPrevious(E element) {
            synchronized (DoublyLinkedList.this) {
                _prev = new NodeImpl(element, _prev, this);

                if (_prev._prev == null) {
                    head = _prev;
                }

                return _prev;
            }
        }

        @Override
        public Node<E> getNext() {
            synchronized (DoublyLinkedList.this) {
                return _next;
            }
        }

        @Override
        public Node<E> getPrevious() {
            synchronized (DoublyLinkedList.this) {
                return _prev;
            }
        }

        @Override
        public E getValue() {
            return _value;
        }

        @Override
        public boolean remove() {
            synchronized (DoublyLinkedList.this) {
                if (_prev == null && _next == null) {
                    if (head == this || last == this) {
                        head = last = null;

                        size = 0;

                        return true;
                    }

                    return false;
                }

                if (_prev != null) {
                    _prev._next = _next;
                }
                else {
                    head = _next;
                }

                if (_next != null) {
                    _next._prev = _prev;
                }
                else {
                    last = _prev;
                }

                _prev = null;
                _next = null;

                size--;

                return true;
            }
        }

    }

}
