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

package org.apache.aries.jndi.url;

import org.apache.aries.util.nls.MessageUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.NoSuchComponentException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.naming.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlueprintURLContext implements Context {
    static final Pattern graceP = Pattern.compile(".*;\\s*blueprint.graceperiod\\s*:=\\s*\"?([A-Za-z]+).*");
    static final Pattern timeoutP = Pattern.compile(".*;\\s*blueprint.timeout\\s*:=\\s*\"?([0-9]+).*");
    private static final String BLUEPRINT_NAMESPACE = "blueprint:comp/";
    private static final MessageUtil MESSAGES = MessageUtil.createMessageUtil(BlueprintURLContext.class, "org.apache.aries.jndi.nls.jndiUrlMessages");
    private Bundle _callersBundle;
    private Map<String, Object> _env;
    private NameParser _parser = new BlueprintNameParser();
    private BlueprintName _parentName;

    @SuppressWarnings("unchecked")
    public BlueprintURLContext(Bundle callersBundle, Hashtable<?, ?> env) {
        _callersBundle = callersBundle;
        _env = new HashMap<String, Object>();
        _env.putAll((Map<? extends String, ? extends Object>) env);
        _parentName = null;
    }

    private BlueprintURLContext(Bundle callersBundle, BlueprintName parentName, Map<String, Object> env) {
        _callersBundle = callersBundle;
        _parentName = parentName;
        _env = env;
    }

    /**
     * Look for a BluepintContainer service in a given bundle
     *
     * @param b Bundle to look in
     * @return BlueprintContainer service, or null if none available
     */
    private static ServiceReference findBPCRef(Bundle b) {
        ServiceReference[] refs = b.getRegisteredServices();
        ServiceReference result = null;
        if (refs != null) {
            outer:
            for (ServiceReference r : refs) {
                String[] objectClasses = (String[]) r.getProperty(Constants.OBJECTCLASS);
                for (String objectClass : objectClasses) {
                    if (objectClass.equals(BlueprintContainer.class.getName())) {
                        // Arguably we could put an r.isAssignableTo(jndi-url-bundle, BlueprintContainer.class.getName())
                        // check here. But if you've got multiple, class-space inconsistent instances of blueprint in
                        // your environment, you've almost certainly got other problems.
                        result = r;
                        break outer;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Obtain a BlueprintContainerService for the given bundle. If the service isn't there, wait for up
     * to the blueprint.graceperiod defined for that bundle for one to be published.
     *
     * @param b The Bundle to look in
     * @return BlueprintContainerService instance for that bundle
     * @throws ServiceUnavailableException If no BlueprinContainerService found
     */
    private static ServiceReference getBlueprintContainerRef(Bundle b) throws ServiceUnavailableException {
        ServiceReference result = findBPCRef(b);
        if (result == null) {
            Semaphore s = new Semaphore(0);
            AtomicReference<ServiceReference> bpcRef = new AtomicReference<ServiceReference>();
            ServiceTracker st = new ServiceTracker(b.getBundleContext(), BlueprintContainer.class.getName(),
                    new BlueprintContainerServiceTrackerCustomizer(b, s, bpcRef));
            st.open();

            // Make another check for the BlueprintContainer service in case it came up just before our tracker engaged
            int graceperiod = getGracePeriod(b);
            result = findBPCRef(b);
            if (result == null && graceperiod >= 0) {
                if (graceperiod == 0) { // Wait for an unlimited period
                    try {
                        s.acquire();
                    } catch (InterruptedException ix) {
                    }
                } else {
                    try {
                        s.tryAcquire(graceperiod, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ix) {
                    }
                }
            }
            result = bpcRef.get();
            st.close();
        }
        if (result == null) {
            throw new ServiceUnavailableException(MESSAGES.getMessage("no.blueprint.container", b.getSymbolicName() + '/' + b.getVersion()));
        }
        return result;
    }

    /**
     * Determine the blueprint.timeout set for a given bundle
     *
     * @param b The bundle to inspect
     * @return -1 if blueprint.graceperiod is false, otherwise the value of blueprint.timeout,
     * or 300000 if blueprint.graceperiod is true and no value is given for
     * blueprint.timeout.
     */
    public static int getGracePeriod(Bundle b) {
        int result = 300000;            // Blueprint default
        boolean gracePeriodSet = true;  // Blueprint default
        String bundleSymbolicName = (String) b.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);

        // I'd like to use ManifestHeaderProcessor here but as of December 15th 2010 it lives
        // application-utils, and I don't want to make that a dependency of jndi-url

        Matcher m = graceP.matcher(bundleSymbolicName);
        if (m.matches()) {
            String gracePeriod = m.group(1);
            gracePeriodSet = !gracePeriod.equalsIgnoreCase("false"); // See OSGi Enterprise spec 4.2 section 121.3.2.1 step 6
        }
        if (!gracePeriodSet) {
            result = -1;
        } else {
            m = timeoutP.matcher(bundleSymbolicName);
            if (m.matches()) {
                String timeout = m.group(1);
                try {
                    result = Integer.valueOf(timeout);
                } catch (NumberFormatException nfx) {
                    // Noop: result stays at its default value
                }
            }
        }
        return result;
    }

    @Override
    protected void finalize() throws NamingException {
        close();
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        return _env.put(propName, propVal);
    }

    @Override
    public void bind(Name n, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void bind(String s, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void close() throws NamingException {
        _env = null;
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        String result = prefix + "/" + name;
        String ns = BLUEPRINT_NAMESPACE;
        if (result.startsWith(ns)) {
            ns = "";
        }
        return _parser.parse(ns + result);
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        String result = prefix + "/" + name;
        String ns = BLUEPRINT_NAMESPACE;
        if (result.startsWith(ns)) {
            ns = "";
        }
        _parser.parse(ns + result);
        return result;
    }

    @Override
    public Context createSubcontext(Name n) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Context createSubcontext(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void destroySubcontext(Name n) throws NamingException {
        // No-op we don't support sub-contexts in our context
    }

    @Override
    public void destroySubcontext(String s) throws NamingException {
        // No-op we don't support sub-contexts in our context
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        Hashtable<Object, Object> environment = new Hashtable<Object, Object>();
        environment.putAll(_env);
        return environment;
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NameParser getNameParser(Name n) throws NamingException {
        return _parser;
    }

    @Override
    public NameParser getNameParser(String s) throws NamingException {
        return _parser;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
        NamingEnumeration<NameClassPair> result = new BlueprintComponentNamingEnumeration<NameClassPair>(_callersBundle, new ComponentProcessor<NameClassPair>() {
            @Override
            public NameClassPair get(Binding b) {
                NameClassPair result = new NameClassPair(b.getName(), b.getClassName());
                return result;
            }
        });
        return result;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name)
            throws NamingException {
        NamingEnumeration<Binding> result = new BlueprintComponentNamingEnumeration<Binding>(_callersBundle, new ComponentProcessor<Binding>() {
            @Override
            public Binding get(Binding b) {
                return b;
            }
        });
        return result;
    }

    @Override
    public Object lookup(Name name) throws NamingException, ServiceUnavailableException {
        ServiceReference blueprintContainerRef = getBlueprintContainerRef(_callersBundle);
        Object result;
        try {
            BlueprintContainer blueprintContainer = (BlueprintContainer)
                    _callersBundle.getBundleContext().getService(blueprintContainerRef);
            BlueprintName bpName;
            if (name instanceof BlueprintName) {
                bpName = (BlueprintName) name;
            } else if (_parentName != null) {
                bpName = new BlueprintName(_parentName.toString() + "/" + name.toString());
            } else {
                bpName = (BlueprintName) _parser.parse(name.toString());
            }

            if (bpName.hasComponent()) {
                String componentId = bpName.getComponentId();
                try {
                    result = blueprintContainer.getComponentInstance(componentId);
                } catch (NoSuchComponentException nsce) {
                    throw new NameNotFoundException(nsce.getMessage());
                }
            } else {
                result = new BlueprintURLContext(_callersBundle, bpName, _env);
            }
        } finally {
            _callersBundle.getBundleContext().ungetService(blueprintContainerRef);
        }
        return result;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        if (_parentName != null) {
            name = _parentName.toString() + "/" + name;
        }
        Object result = lookup(_parser.parse(name));
        return result;
    }

    @Override
    public Object lookupLink(Name n) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Object lookupLink(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void rebind(Name n, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void rebind(String s, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return _env.remove(propName);
    }

    @Override
    public void rename(Name nOld, Name nNew) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void rename(String sOld, String sNew) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void unbind(Name n) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void unbind(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }
    // listBindings wants a NamingEnumeration<Binding>
    // list wants a NamingEnumeration<NameClassPair>
    // Both are very similar. As per ServiceRegistryListContext we delegate to a closure to do the final processing
    private interface ComponentProcessor<T> {
        T get(Binding b);
    }

    private static class BlueprintComponentNamingEnumeration<T> implements NamingEnumeration<T> {
        private Binding[] blueprintIdToComponentBindings;
        private int position = 0;
        private ComponentProcessor<T> processor;

        public BlueprintComponentNamingEnumeration(Bundle callersBundle, ComponentProcessor<T> p) throws ServiceUnavailableException {
            ServiceReference blueprintContainerRef = getBlueprintContainerRef(callersBundle);
            try {
                BlueprintContainer blueprintContainer = (BlueprintContainer) callersBundle.getBundleContext().getService(blueprintContainerRef);
                @SuppressWarnings("unchecked")
                Set<String> componentIds = blueprintContainer.getComponentIds();
                blueprintIdToComponentBindings = new Binding[componentIds.size()];
                Iterator<String> idIterator = componentIds.iterator();
                for (int i = 0; i < blueprintIdToComponentBindings.length; i++) {
                    String id = idIterator.next();
                    Object o = blueprintContainer.getComponentInstance(id);
                    blueprintIdToComponentBindings[i] = new Binding(id, o);
                }
                processor = p;
            } finally {
                callersBundle.getBundleContext().ungetService(blueprintContainerRef);
            }
        }


        @Override
        public boolean hasMoreElements() {
            return position < blueprintIdToComponentBindings.length;
        }

        @Override
        public T nextElement() {
            if (!hasMoreElements()) throw new NoSuchElementException();
            Binding bindingToProcess = blueprintIdToComponentBindings[position];
            position++;
            T result = processor.get(bindingToProcess);
            return result;
        }

        @Override
        public T next() throws NamingException {
            return nextElement();
        }

        @Override
        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        @Override
        public void close() throws NamingException {
            // Nothing to do
        }

    }

    private static class BlueprintContainerServiceTrackerCustomizer implements ServiceTrackerCustomizer {
        Bundle bundleToFindBPCServiceIn = null;
        Semaphore semaphore;
        AtomicReference<ServiceReference> atomicRef = null;

        public BlueprintContainerServiceTrackerCustomizer(Bundle b, Semaphore s, AtomicReference<ServiceReference> aref) {
            bundleToFindBPCServiceIn = b;
            semaphore = s;
            atomicRef = aref;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            Object result = null;
            if (bundleToFindBPCServiceIn.equals(reference.getBundle())) {
                atomicRef.set(reference);
                semaphore.release();
                result = reference.getBundle().getBundleContext().getService(reference);
            }
            return result;
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
        }
    }

}
