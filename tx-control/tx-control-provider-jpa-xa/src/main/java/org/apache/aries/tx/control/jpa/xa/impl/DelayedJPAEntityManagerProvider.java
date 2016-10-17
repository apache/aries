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
package org.apache.aries.tx.control.jpa.xa.impl;

import java.util.function.Function;

import javax.persistence.EntityManager;

import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAEntityManagerProvider;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;

public class DelayedJPAEntityManagerProvider extends AbstractJPAEntityManagerProvider {
	
	private final Function<ThreadLocal<TransactionControl>, AbstractJPAEntityManagerProvider> wireToTransactionControl;
	
	private final ThreadLocal<TransactionControl> commonStore = new ThreadLocal<>();
	
	private AbstractJPAEntityManagerProvider delegate;
	
	private boolean closed;
	
	public DelayedJPAEntityManagerProvider(Function<ThreadLocal<TransactionControl>, 
			AbstractJPAEntityManagerProvider> wireToTransactionControl) {
		super(null, null);
		this.wireToTransactionControl = wireToTransactionControl;
	}

	@Override
	public EntityManager getResource(TransactionControl txControl) throws TransactionException {
		synchronized (wireToTransactionControl) {
			if(closed) {
				throw new IllegalStateException("This XA JPA resource provider has been closed");
			}
			if(delegate == null) {
				commonStore.set(txControl);
				delegate = wireToTransactionControl.apply(commonStore);
			}
		}
		return delegate.getResource(txControl);
	}
	
	public void close() {
		AbstractJPAEntityManagerProvider toClose = null;
		synchronized (wireToTransactionControl) {
			if(!closed) {
				closed = true;
				toClose = delegate;
				delegate = null;
			}
		}
		
		if(toClose != null) {
			toClose.close();
		}
	}

}
