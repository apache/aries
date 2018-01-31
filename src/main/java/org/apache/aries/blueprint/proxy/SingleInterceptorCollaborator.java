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
package org.apache.aries.blueprint.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.proxy.InvocationListener;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collaborator which ensures preInvoke and postInvoke occur before and after
 * method invocation
 */
public class SingleInterceptorCollaborator implements InvocationListener, Serializable {

    /** Serial version UID for this class */
    private static final long serialVersionUID = -58189302118314469L;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Collaborator.class);

    private transient Interceptor interceptor;
    private transient ComponentMetadata cm;

    private static final Object NON_INVOKED = new Object();

    public SingleInterceptorCollaborator(ComponentMetadata cm, Interceptor interceptor) {
        this.cm = cm;
        this.interceptor = interceptor;
    }

    /**
     * Invoke the preCall method on the interceptor
     */
    public Object preInvoke(Object o, Method m, Object[] parameters)
            throws Throwable {
        Object callToken = NON_INVOKED;
        try {
            callToken = interceptor.preCall(cm, m, parameters);
        } catch (Throwable t) {
            // using null token here to be consistent with what Collaborator does
            postInvokeExceptionalReturn(null, o, m, t);
            throw t;
        }
        return callToken;
    }

    /**
     * Called when the method is called and returned normally
     */
    public void postInvoke(Object token, Object o, Method method,
                           Object returnType) throws Throwable {

        if (token != NON_INVOKED) {
            try {
                interceptor.postCallWithReturn(cm, method, returnType, token);
            } catch (Throwable t) {
                LOGGER.debug("postCallInterceptorWithReturn", t);
                throw t;
            }
        }
    }

    /**
     * Called when the method is called and returned with an exception
     */
    public void postInvokeExceptionalReturn(Object token, Object o, Method method,
                                            Throwable exception) throws Throwable {
        try {
            interceptor.postCallWithException(cm, method, exception, token);
        } catch (Throwable t) {
            // log the exception
            LOGGER.debug("postCallInterceptorWithException", t);
            throw t;
        }
    }
}