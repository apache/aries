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
package org.apache.aries.jndi.url;

import org.apache.aries.jndi.services.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import javax.naming.*;
import java.util.Map;
import java.util.NoSuchElementException;

public class ServiceRegistryListContext extends AbstractServiceRegistryContext implements Context {
    /**
     * The osgi lookup name
     **/
    private OsgiName parentName;

    public ServiceRegistryListContext(BundleContext callerContext, Map<String, Object> env, OsgiName validName) {
        super(callerContext, env);
        parentName = validName;
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        if (!!!"".equals(name)) throw new NameNotFoundException(name);
        final ServiceReference[] refs = getServiceRefs();
        return new ServiceNamingEnumeration<NameClassPair>(callerContext, refs, new ThingManager<NameClassPair>() {
            public NameClassPair get(BundleContext ctx, ServiceReference ref) {
                Object service = ctx.getService(ref);
                String className = (service != null) ? service.getClass().getName() : null;
                ctx.ungetService(ref);
                return new NameClassPair(serviceId(ref), className, true);
            }

            public void release(BundleContext ctx, ServiceReference ref) {
            }
        });
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        if (!!!"".equals(name)) throw new NameNotFoundException(name);
        final ServiceReference[] refs = getServiceRefs();
        return new ServiceNamingEnumeration<Binding>(callerContext, refs, new ThingManager<Binding>() {
            public Binding get(BundleContext ctx, ServiceReference ref) {
                Object service = ServiceHelper.getService(ctx, ref);
                return new Binding(serviceId(ref), service, true);
            }

            public void release(BundleContext ctx, ServiceReference ref) {
                ctx.ungetService(ref);
            }
        });
    }

    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    public Object lookup(String name) throws NamingException {
        Object result = ServiceHelper.getService(callerContext, parentName, name, false, env, true);
        if (result == null) {
            throw new NameNotFoundException(name.toString());
        }
        return result;
    }

    private String serviceId(ServiceReference ref) {
        return String.valueOf(ref.getProperty(Constants.SERVICE_ID));
    }

    private ServiceReference[] getServiceRefs() throws NamingException {
        return ServiceHelper.getServiceReferences(callerContext, parentName.getInterface(), parentName.getFilter(), parentName.getServiceName(), env);
    }

    private interface ThingManager<T> {
        public T get(BundleContext ctx, ServiceReference ref);

        public void release(BundleContext ctx, ServiceReference ref);
    }

    private static class ServiceNamingEnumeration<T> implements NamingEnumeration<T> {
        private BundleContext ctx;
        private ServiceReference[] refs;
        private int position = 0;
        private ThingManager<T> mgr;
        private T last;

        private ServiceNamingEnumeration(BundleContext context, ServiceReference[] theRefs, ThingManager<T> manager) {
            ctx = context;
            refs = (theRefs != null) ? theRefs : new ServiceReference[0];
            mgr = manager;
        }

        public void close() throws NamingException {
            mgr.release(ctx, refs[position - 1]);
            last = null;
        }

        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        public T next() throws NamingException {
            return nextElement();
        }

        public boolean hasMoreElements() {
            return position < refs.length;
        }

        public T nextElement() {
            if (!!!hasMoreElements()) throw new NoSuchElementException();

            if (position > 0) mgr.release(ctx, refs[position - 1]);

            last = mgr.get(ctx, refs[position++]);

            return last;
        }

    }

}