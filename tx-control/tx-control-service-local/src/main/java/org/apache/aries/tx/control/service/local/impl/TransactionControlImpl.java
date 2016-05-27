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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.tx.control.service.local.impl;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
import org.apache.aries.tx.control.service.common.impl.AbstractTransactionControlImpl;

public class TransactionControlImpl extends AbstractTransactionControlImpl {

	private static class TxId {
		private final UUID controlId;
		private final long txId;
		
		public TxId(UUID controlId, long txId) {
			this.controlId = controlId;
			this.txId = txId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + controlId.hashCode();
			result = prime * result + (int) (txId ^ (txId >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TxId other = (TxId) obj;
			if (!controlId.equals(other.controlId))
				return false;
			if (txId != other.txId)
				return false;
			return true;
		}
	}
	
	private final UUID txControlId = UUID.randomUUID();
	private final AtomicLong txCounter = new AtomicLong();
	
	@Override
	protected AbstractTransactionContextImpl startTransaction(boolean readOnly) {
		return new TransactionContextImpl(new TxId(txControlId, txCounter.incrementAndGet()), readOnly);
	}
	
}
