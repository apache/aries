/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.util.*;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemEvent;
import org.apache.aries.subsystem.SubsystemException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.service.composite.CompositeBundle;

public class SubsystemImpl implements Subsystem {

    final long id;
    final SubsystemAdminImpl admin;
    final CompositeBundle composite;
    final SubsystemEventDispatcher eventDispatcher;

    public SubsystemImpl(SubsystemAdminImpl admin, CompositeBundle composite, SubsystemEventDispatcher eventDispatcher) {
        this.admin = admin;
        this.composite = composite;
        this.id = composite.getBundleId();
        this.eventDispatcher = eventDispatcher;
    }

    public State getState() {
        switch (composite.getState())
        {
            case Bundle.UNINSTALLED:
                return State.UNINSTALLED;
            case Bundle.INSTALLED:
                return State.INSTALLED;
            case Bundle.RESOLVED:
                return State.RESOLVED;
            case Bundle.STARTING:
                return State.STARTING;
            case Bundle.ACTIVE:
                return State.ACTIVE;
            case Bundle.STOPPING:
                return State.STOPPING;
        }
        throw new SubsystemException("Unable to retrieve subsystem state");
    }

    public void start() throws SubsystemException {
        try {
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.STARTING, System.currentTimeMillis(), this));
            composite.start();
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.STARTED, System.currentTimeMillis(), this));
        } catch (BundleException e) {
            throw new SubsystemException("Unable to start subsystem", e);
        }
    }

    public void stop() throws SubsystemException {
        try {
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.STOPPING, System.currentTimeMillis(), this));
            composite.stop();
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.STOPPED, System.currentTimeMillis(), this));
        } catch (BundleException e) {
            throw new SubsystemException("Unable to stop subsystem", e);
        }
    }

    public long getSubsystemId() {
        return composite.getBundleId();
    }

    public String getLocation() {
        return composite.getLocation();
    }

    public String getSymbolicName() {
        return composite.getSymbolicName();
    }

    public Version getVersion() {
        return composite.getVersion();
    }

    public Map<String, String> getHeaders() {
        return getHeaders(null);
    }

    public Map<String, String> getHeaders(String locale) {
        final Dictionary dict = composite.getHeaders(locale);
        return new DictionaryAsMap(dict);
    }

    public Collection<Bundle> getConstituents() {
        List<Bundle> list = new ArrayList<Bundle>();
        Bundle[] bundles = composite.getSystemBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getBundleId() != 0) {
                list.add(bundle);
            }
        }
        return list;
    }

    private static class DictionaryAsMap extends AbstractMap<String,String> {
        private Dictionary dict;

        private DictionaryAsMap(Dictionary dict) {
            this.dict = dict;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return new AbstractSet<Entry<String, String>>() {
                @Override
                public Iterator<Entry<String, String>> iterator() {
                    final Enumeration e = dict.keys();
                    return new Iterator<Entry<String,String>>() {
                        public boolean hasNext() {
                            return e.hasMoreElements();
                        }

                        public Entry<String, String> next() {
                            Object key = e.nextElement();
                            Object val = dict.get(key);
                            return new SimpleImmutableEntry<String,String>(key != null ? key.toString() : null, val != null ? val.toString() : null);
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
                @Override
                public int size() {
                    return dict.size();
                }
            };
        }
    }
}