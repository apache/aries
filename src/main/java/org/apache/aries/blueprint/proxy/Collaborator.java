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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.proxy.InvocationListener;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collaborator which ensures preInvoke and postInvoke occur before and after
 * method invocation
 */
public class Collaborator implements InvocationListener, Serializable {

    /** Serial version UID for this class */
    private static final long serialVersionUID = -58189302118314469L;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Collaborator.class);

    private transient List<Interceptor> interceptors = null;
    private transient ComponentMetadata cm = null;

    public Collaborator(ComponentMetadata cm, List<Interceptor> interceptors) {
        this.cm = cm;
        this.interceptors = interceptors;
    }

    /**
     * Invoke the preCall method on the interceptor
     * 
     * @param o
     *            : The Object being invoked
     * @param m
     *            : method
     * @param parameters
     *            : method paramters
     * @throws Throwable
     */
    public Object preInvoke(Object o, Method m, Object[] parameters)
            throws Throwable {
        Deque<StackElement> stack = new ArrayDeque<StackElement>(interceptors.size());
        if (interceptors != null) {
          try{
            for (Interceptor im : interceptors) {
                Collaborator.StackElement se = new StackElement(im);

                // should we do this before or after the preCall ?
                stack.push(se);

                // allow exceptions to propagate
                se.setPreCallToken(im.preCall(cm, m, parameters));
            }
          } catch (Throwable t) {
            postInvokeExceptionalReturn(stack, o, m, t);
            throw t;
          }
        }
        return stack;
    }

    /**
     * Called when the method is called and returned normally
     */
    public void postInvoke(Object token, Object o, Method method, 
         Object returnType) throws Throwable {
        
        Deque<StackElement> calledInterceptors =
                    (Deque<StackElement>) token;
        if(calledInterceptors != null) {
            while (!calledInterceptors.isEmpty()) {
                Collaborator.StackElement se = calledInterceptors.pop();
                try {
                    se.interceptor.postCallWithReturn(cm, method, returnType, se
                            .getPreCallToken());
                } catch (Throwable t) {
                    LOGGER.debug("postCallInterceptorWithReturn", t);
                    // propagate this to invoke ... further interceptors will be
                    // called via the postCallInterceptorWithException method
                    throw t;
                }
            } // end while
        }
    }

    /**
     * Called when the method is called and returned with an exception
     */
    public void postInvokeExceptionalReturn(Object token, Object o, Method method,
                 Throwable exception) throws Throwable {
        Throwable tobeRethrown = null;
        Deque<StackElement> calledInterceptors =
          (Deque<StackElement>) token;
        while (!calledInterceptors.isEmpty()) {
            Collaborator.StackElement se = calledInterceptors.pop();

            try {
                se.interceptor.postCallWithException(cm, method, exception, se
                        .getPreCallToken());
            } catch (Throwable t) {
                // log the exception
                LOGGER.debug("postCallInterceptorWithException", t);
                if (tobeRethrown == null) {
                    tobeRethrown = t;
                } else {
                  LOGGER.warn("Discarding post-call with interceptor exception", t);
                }
            }

        } // end while

        if (tobeRethrown != null)
            throw tobeRethrown;
    }

    // info to store on interceptor stack during invoke
    private static class StackElement {
        private final Interceptor interceptor;
        private Object preCallToken;

        private StackElement(Interceptor i) {
            interceptor = i;
        }

        private void setPreCallToken(Object preCallToken) {
            this.preCallToken = preCallToken;
        }

        private Object getPreCallToken() {
            return preCallToken;
        }

    }
}