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
package org.apache.aries.blueprint.container;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.aries.blueprint.ExtendedReferenceMetadata;
import org.apache.aries.blueprint.di.CollectionRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.ValueRecipe;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A recipe to create an unary OSGi service reference.
 *
 * TODO: check synchronization / thread safety
 *
 * TODO: looks there is a potential problem if the service is unregistered between a call
 *        to ServiceDispatcher#loadObject() and when the actual invocation finish
 *
 * @version $Rev$, $Date$
 */
@SuppressWarnings("rawtypes")
public class ReferenceRecipe extends AbstractServiceReferenceRecipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceRecipe.class);

    private final ReferenceMetadata metadata;
    private Object proxy;

    private final Object monitor = new Object();
    private volatile ServiceReference trackedServiceReference;
    private volatile Object trackedService;
    private Object defaultBean;

    private final Collection<Class<?>> proxyChildBeanClasses;
    private final Collection<WeakReference<Voidable>> proxiedChildren;

    private final boolean staticLifecycle;
    
    public ReferenceRecipe(String name,
                           ExtendedBlueprintContainer blueprintContainer,
                           ReferenceMetadata metadata,
                           ValueRecipe filterRecipe,
                           CollectionRecipe listenersRecipe,
                           List<Recipe> explicitDependencies) {
        super(name, blueprintContainer, metadata, filterRecipe, listenersRecipe, explicitDependencies);
        this.metadata = metadata;
        if (metadata instanceof ExtendedReferenceMetadata) {
            staticLifecycle = ((ExtendedReferenceMetadata) metadata).getLifecycle()
                    == ExtendedReferenceMetadata.LIFECYCLE_STATIC;
            if (!staticLifecycle) {
                proxyChildBeanClasses = ((ExtendedReferenceMetadata) metadata).getProxyChildBeanClasses();
                if (proxyChildBeanClasses != null) {
                    proxiedChildren = new ArrayList<WeakReference<Voidable>>();
                } else {
                    proxiedChildren = null;
                }
            } else {
                proxyChildBeanClasses = null;
                proxiedChildren = null;
            }
        } else {
            staticLifecycle = false;
            proxyChildBeanClasses = null;
            proxiedChildren = null;
        }
    }

    @Override
    protected Object internalCreate() throws ComponentDefinitionException {
        try {
            if (explicitDependencies != null) {
                for (Recipe recipe : explicitDependencies) {
                    recipe.create();
                }
            }
            // Create the proxy
            Set<Class<?>> interfaces = new HashSet<Class<?>>();
            Class<?> clz = getInterfaceClass();
            if (clz != null) interfaces.add(clz);
            
            if (metadata instanceof ExtendedReferenceMetadata) {
                interfaces.addAll(loadAllClasses(((ExtendedReferenceMetadata)metadata).getExtraInterfaces()));
            }

            Object result;
            if (isStaticLifecycle()) {
                result = getService();
            }
            else {
                proxy = createProxy(new ServiceDispatcher(), interfaces);

                // Add partially created proxy to the context
                result = new ServiceProxyWrapper();
            }

            addPartialObject(result);

            // Handle initial references
            createListeners();
            updateListeners();            

            return result;
        } catch (ComponentDefinitionException e) {
            throw e;
        } catch (Throwable t) {
            throw new ComponentDefinitionException(t);
        }
    }

    protected void doStop() {
        synchronized (monitor) {
            unbind();
            monitor.notifyAll();
        }
    }

    protected void retrack() {
        ServiceReference ref = getBestServiceReference();
        if (ref != null) {
            bind(ref);
        } else {
            unbind();
        }
    }

    protected void track(ServiceReference ref) {
        boolean replace;
        if (metadata instanceof ExtendedReferenceMetadata) {
            replace = ((ExtendedReferenceMetadata) metadata).getDamping()
                        == ExtendedReferenceMetadata.DAMPING_GREEDY;
        } else {
            replace = false;
        }
        synchronized (monitor) {
            if (trackedServiceReference == null || replace) {
                retrack();
            }
        }
    }

    protected void untrack(ServiceReference ref) {
        synchronized (monitor) {
            if (trackedServiceReference == ref) {
                retrack();
            }
        }
    }

    @Override
    public boolean isStaticLifecycle() {
        return staticLifecycle;
    }

    protected void bind(ServiceReference ref) {
        LOGGER.debug("Binding reference {} to {}", getName(), ref);
        synchronized (monitor) {
            ServiceReference oldReference = trackedServiceReference;
            trackedServiceReference = ref;
            voidProxiedChildren();
            bind(trackedServiceReference, proxy);
            if (ref != oldReference) {
              if (oldReference != null && trackedService != null) {
                try {
                  blueprintContainer.getBundleContext().ungetService(oldReference);
                } catch (IllegalStateException ise) {
                  // In case the service no longer exists lets just cope and ignore.
                }
              }
              trackedService = null;
            }
            monitor.notifyAll();
        }
    }

    private void unbind() {
        LOGGER.debug("Unbinding reference {}", getName());
        synchronized (monitor) {
            if (trackedServiceReference != null) {
                unbind(trackedServiceReference, proxy);
                ServiceReference oldReference = trackedServiceReference;
                trackedServiceReference = null;
                voidProxiedChildren();
                if(trackedService != null){
                  try {
                    getBundleContextForServiceLookup().ungetService(oldReference);
                  } catch (IllegalStateException ise) {
                    // In case the service no longer exists lets just cope and ignore.
                  }
                  trackedService = null;
                }
                monitor.notifyAll();
            }
        }
    }

    private Object getService() throws InterruptedException {
        synchronized (monitor) {
            if (isStarted() && trackedServiceReference == null && metadata.getTimeout() > 0
                    && metadata.getAvailability() == ServiceReferenceMetadata.AVAILABILITY_MANDATORY) {
                //Here we want to get the blueprint bundle itself, so don't use #getBundleContextForServiceLookup()
                blueprintContainer.getEventDispatcher().blueprintEvent(createWaitingevent());
                monitor.wait(metadata.getTimeout());
            }
            Object result = null;
            if (trackedServiceReference == null) {
                if (isStarted()) {
                  boolean failed = true;
                  if (metadata.getAvailability() == ReferenceMetadata.AVAILABILITY_OPTIONAL && 
                      metadata instanceof ExtendedReferenceMetadata) {
                     if (defaultBean == null) {
                         String defaultBeanId = ((ExtendedReferenceMetadata)metadata).getDefaultBean();
                         if (defaultBeanId != null) {
                           defaultBean = blueprintContainer.getComponentInstance(defaultBeanId);
                           failed = false;
                         }
                     } else {
                         failed = false;
                     }
                     result = defaultBean;
                  } 
                  
                  if (failed) {
                    if (metadata.getAvailability() == ServiceReferenceMetadata.AVAILABILITY_MANDATORY) {
                        LOGGER.info("Timeout expired when waiting for mandatory OSGi service reference {}", getOsgiFilter());
                        throw new ServiceUnavailableException("Timeout expired when waiting for mandatory OSGi service reference: " + getOsgiFilter(), getOsgiFilter());
                    } else {
                        LOGGER.info("No matching service for optional OSGi service reference {}", getOsgiFilter());
                        throw new ServiceUnavailableException("No matching service for optional OSGi service reference: " + getOsgiFilter(), getOsgiFilter());
                    }
                  }
                } else {
                    throw new ServiceUnavailableException("The Blueprint container is being or has been destroyed: " + getOsgiFilter(), getOsgiFilter());
                }
            } else {
            
              if (trackedService == null) {
            	  trackedService = getServiceSecurely(trackedServiceReference);
              }
              
              if (trackedService == null) {
                  throw new IllegalStateException("getService() returned null for " + trackedServiceReference);
              }
              
              result = trackedService;
            }
            return result;
        }
    }

    private BlueprintEvent createWaitingevent() {
        return new BlueprintEvent(BlueprintEvent.WAITING, 
                                  blueprintContainer.getBundleContext().getBundle(), 
                                  blueprintContainer.getExtenderBundle(), 
                                  new String[] { getOsgiFilter() });
    }

    private ServiceReference getServiceReference() throws InterruptedException {
        synchronized (monitor) {
            if (!optional) {
                getService();
            }
            return trackedServiceReference;
        }
    }
    
    private void voidProxiedChildren() {
        if(proxyChildBeanClasses != null) {
            synchronized(proxiedChildren) {
                for(Iterator<WeakReference<Voidable>> it = proxiedChildren.iterator(); it.hasNext();) {
                    Voidable v = it.next().get();
                    if(v == null)
                        it.remove();
                    else
                      v.voidReference();
                }
            }
        }
    }
    
    public void addVoidableChild(Voidable v) {
        if(proxyChildBeanClasses != null) {
            synchronized (proxiedChildren) {
                proxiedChildren.add(new WeakReference<Voidable>(v));
            }
        } else {
            throw new IllegalStateException("Proxying of child beans is disabled for this recipe");
        }
    }
    
    public Collection<Class<?>> getProxyChildBeanClasses() {
        return proxyChildBeanClasses;
    }

    public class ServiceDispatcher implements Callable<Object> {

        public Object call() throws Exception {
            return getService();
        }

    }

    public class ServiceProxyWrapper implements AggregateConverter.Convertible {

        public Object convert(ReifiedType type) throws Exception {
            if (type.getRawClass() == ServiceReference.class) {
                return getServiceReference();
            } else if (type.getRawClass().isInstance(proxy)) {
                return proxy;
            } else {
                throw new ComponentDefinitionException("Unable to convert to " + type);
            }
        }

    }

}
