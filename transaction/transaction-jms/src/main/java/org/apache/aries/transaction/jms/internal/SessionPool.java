/**
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
package org.apache.aries.transaction.jms.internal;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XAConnection;
import javax.jms.XASession;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * Represents the session pool for a given JMS connection.
 *
 *
 */
public class SessionPool implements PoolableObjectFactory {
    private ConnectionPool connectionPool;
    private SessionKey key;
    private ObjectPool sessionPool;

    public SessionPool(ConnectionPool connectionPool, SessionKey key, ObjectPool sessionPool) {
        this.connectionPool = connectionPool;
        this.key = key;
        this.sessionPool = sessionPool;
        sessionPool.setFactory(this);
    }

    public void close() throws Exception {
        if (sessionPool != null) {
            sessionPool.close();
        }
        sessionPool = null;
    }

    public PooledSession borrowSession() throws JMSException {
        try {
            Object object = getSessionPool().borrowObject();
            return (PooledSession)object;
        } catch (JMSException e) {
            throw e;
        } catch (Exception e) {
            throw JMSExceptionSupport.create(e);
        }
    }

    public void returnSession(PooledSession session) throws JMSException {
        // lets check if we are already closed
        getConnection();
        try {
            connectionPool.onSessionReturned(session);
            getSessionPool().returnObject(session);
        } catch (Exception e) {
            throw JMSExceptionSupport.create("Failed to return session to pool: " + e, e);
        }
    }

    public void invalidateSession(PooledSession session) throws JMSException {
        try {
            connectionPool.onSessionInvalidated(session);
            getSessionPool().invalidateObject(session);
        } catch (Exception e) {
            throw JMSExceptionSupport.create("Failed to invalidate session: " + e, e);
        }
    }

    // PoolableObjectFactory methods
    // -------------------------------------------------------------------------
    public Object makeObject() throws Exception {
    	if (getConnection() instanceof XAConnection) {
    		return new PooledSession(createXaSession(), this, key.isTransacted());
    	} else {
    		return new PooledSession(createSession(), this, key.isTransacted());
    	}
    }

    public void destroyObject(Object o) throws Exception {
        PooledSession session = (PooledSession)o;
        session.getInternalSession().close();
    }

    public boolean validateObject(Object o) {
        return true;
    }

    public void activateObject(Object o) throws Exception {
    }

    public void passivateObject(Object o) throws Exception {
    }

    // Implemention methods
    // -------------------------------------------------------------------------
    protected ObjectPool getSessionPool() throws JMSException {
        if (sessionPool == null) {
            throw new JMSException("Already closed");
        }
        return sessionPool;
    }

    protected Connection getConnection() throws JMSException {
        return connectionPool.getConnection();
    }

    protected Session createSession() throws JMSException {
        return getConnection().createSession(key.isTransacted(), key.getAckMode());
    }
    
    protected XASession createXaSession() throws JMSException {
        return ((XAConnection)getConnection()).createXASession();
    }

}
