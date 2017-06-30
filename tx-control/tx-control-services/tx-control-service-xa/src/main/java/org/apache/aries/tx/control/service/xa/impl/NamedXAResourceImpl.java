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
package org.apache.aries.tx.control.service.xa.impl;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.geronimo.transaction.manager.NamedXAResource;
import org.apache.geronimo.transaction.manager.RecoveryWorkAroundTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamedXAResourceImpl implements NamedXAResource, AutoCloseable {

	final Logger logger = LoggerFactory.getLogger(NamedXAResourceImpl.class);
	
	final String name;
	final XAResource xaResource;
	final RecoveryWorkAroundTransactionManager transactionManager;
	final boolean original;

	boolean closed;

	public NamedXAResourceImpl(String name, XAResource xaResource,
			RecoveryWorkAroundTransactionManager transactionManager, boolean original) {
		this.name = name;
		this.xaResource = xaResource;
		this.transactionManager = transactionManager;
		this.original = original;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void close() {
		closed = true;
	}

	private interface XAAction {
		void perform() throws XAException;
	}
	
	private interface XAReturnAction<T> {
		T perform() throws XAException;
	}
	
	private void safeCall(XAAction action) throws XAException {
		checkOpen();
		
		try {
			action.perform();
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	private <T> T safeCall(XAReturnAction<T> action) throws XAException {
		checkOpen();
		try {
			return action.perform();
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	private void checkOpen() throws XAException {
		if(closed) {
			XAException xaException = new XAException("This instance of the resource named " + name + " is no longer available");
			xaException.errorCode = XAException.XAER_RMFAIL;
			throw xaException;
		}
	}

	private XAException handleException(Exception e) throws XAException {
		if(e instanceof XAException) {
			XAException xae = (XAException) e;
			if(xae.errorCode == 0) {
				if(original) {
					// We are the originally enlisted resource, and will play some tricks to attempt recovery 
					if(transactionManager.getNamedResource(name) == null) {
						logger.error("The XA resource named {} threw an XAException but did not set the error code. There is also no RecoverableXAResource available with the name {}. It is not possible to recover from this situation and so the transaction will have to be resolved by an operator.", name, name, xae);
						xae.errorCode = XAException.XAER_RMERR;
					} else {
						logger.warn("The XA resource named {} threw an XAException but did not set the error code. Changing it to be an \"RM_FAIL\" to permit recovery attempts", name, xae);
						xae.errorCode = XAException.XAER_RMFAIL;
					}
				} else {
					logger.warn("The XA resource named {} threw an XAException but did not set the error code. Recovery has already been attempted for this resource and it has not been possible to recover from this situation. The transaction will have to be resolved by an operator.", name, xae);
					xae.errorCode = XAException.XAER_RMERR;
				}
			}
			return xae;
		} else {
			logger.warn("The recoverable XA resource named {} threw an Exception which is not permitted by the interface. Changing it to be a \"Resource Manager Error\" XAException which prevents recovery", name, e);
			XAException xaException = new XAException(XAException.XAER_RMERR);
			xaException.initCause(e);
			return xaException;
		}
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		safeCall(() -> xaResource.commit(xid, onePhase));
	}

	public void end(Xid xid, int flags) throws XAException {
		safeCall(() -> xaResource.end(xid, flags));
	}

	public void forget(Xid xid) throws XAException {
		safeCall(() -> xaResource.forget(xid));
	}

	public int getTransactionTimeout() throws XAException {
		return safeCall(() -> xaResource.getTransactionTimeout());
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		return safeCall(() -> xaResource.isSameRM(xares));
	}

	public int prepare(Xid xid) throws XAException {
		return safeCall(() -> xaResource.prepare(xid));
	}

	public Xid[] recover(int flag) throws XAException {
		return safeCall(() -> xaResource.recover(flag));
	}

	public void rollback(Xid xid) throws XAException {
		safeCall(() -> xaResource.rollback(xid));
	}

	public boolean setTransactionTimeout(int seconds) throws XAException {
		return safeCall(() -> xaResource.setTransactionTimeout(seconds));
	}

	public void start(Xid xid, int flags) throws XAException {
		safeCall(() -> xaResource.start(xid, flags));
	}
	
}
