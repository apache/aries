/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.samples.ariestrader.util;

import java.util.AbstractSequentialList;
import java.util.ListIterator;
public class KeyBlock extends AbstractSequentialList 
{

	// min and max provide range of valid primary keys for this KeyBlock
	private int min = 0;
	private int max = 0;
	private int index = 0;

	/**
	 * Constructor for KeyBlock
	 */
	public KeyBlock() {
		super();
		min = 0;
		max = 0;
		index = min;
	}

	/**
	 * Constructor for KeyBlock 
	 */
	public KeyBlock(int min, int max) {
		super();
		this.min = min;
		this.max = max;
		index = min;
	}

	/**
	 * @see AbstractCollection#size()
	 */
	public int size() {
		return (max - min) + 1;
	}

	/**
	 * @see AbstractSequentialList#listIterator(int)
	 */
	public ListIterator listIterator(int arg0) {
		return new KeyBlockIterator();
	}

	class KeyBlockIterator implements ListIterator {

		/**
		 * @see ListIterator#hasNext()
		 */
		public boolean hasNext() {
			return index <= max;
		}

		/**
		 * @see ListIterator#next()
		 */
		public synchronized Object next() {
			if (index > max)
				throw new java.lang.RuntimeException("KeyBlock:next() -- Error KeyBlock depleted");
			return new Integer(index++);
		}

		/**
		 * @see ListIterator#hasPrevious()
		 */
		public boolean hasPrevious() {
			return index > min;
		}

		/**
		 * @see ListIterator#previous()
		 */
		public Object previous() {
			return new Integer(--index);
		}

		/**
		 * @see ListIterator#nextIndex()
		 */
		public int nextIndex() {
			return index-min;
		}

		/**
		 * @see ListIterator#previousIndex()
		 */
		public int previousIndex() {
			throw new UnsupportedOperationException("KeyBlock: previousIndex() not supported");
		}

		/**
		 * @see ListIterator#add()
		 */
		public void add(Object o) {
			throw new UnsupportedOperationException("KeyBlock: add() not supported");
		}

		/**
		 * @see ListIterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException("KeyBlock: remove() not supported");
		}

		/**
		 * @see ListIterator#set(Object)
		 */
		public void set(Object arg0) {
		}
	}
}