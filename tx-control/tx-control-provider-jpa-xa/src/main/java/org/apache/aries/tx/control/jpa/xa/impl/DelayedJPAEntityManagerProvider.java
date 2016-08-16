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

import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;

public class DelayedJPAEntityManagerProvider implements JPAEntityManagerProvider {
	
	private final Function<ThreadLocal<TransactionControl>, JPAEntityManagerProvider> wireToTransactionControl;
	
	private final ThreadLocal<TransactionControl> commonStore = new ThreadLocal<>();
	
	private JPAEntityManagerProvider delegate;
	
	public DelayedJPAEntityManagerProvider(Function<ThreadLocal<TransactionControl>, 
			JPAEntityManagerProvider> wireToTransactionControl) {
		this.wireToTransactionControl = wireToTransactionControl;
	}

	@Override
	public EntityManager getResource(TransactionControl txControl) throws TransactionException {
		synchronized (wireToTransactionControl) {
			if(delegate == null) {
				commonStore.set(txControl);
				delegate = wireToTransactionControl.apply(commonStore);
			}
		}
		return delegate.getResource(txControl);
	}
	
	

}
