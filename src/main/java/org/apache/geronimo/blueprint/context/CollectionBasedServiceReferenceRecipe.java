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
package org.apache.geronimo.blueprint.context;

import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

import net.sf.cglib.proxy.Dispatcher;

import org.apache.geronimo.blueprint.BlueprintContextEventSender;
import org.apache.geronimo.blueprint.Destroyable;
import org.apache.geronimo.blueprint.utils.DynamicCollection;
import org.apache.geronimo.blueprint.utils.DynamicList;
import org.apache.geronimo.blueprint.utils.DynamicSet;
import org.apache.geronimo.blueprint.utils.DynamicSortedList;
import org.apache.geronimo.blueprint.utils.DynamicSortedSet;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.RecipeHelper;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.reflect.RefCollectionMetadata;

/**
 * A recipe to create a managed collection of service references
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class CollectionBasedServiceReferenceRecipe extends AbstractServiceReferenceRecipe {

    private final RefCollectionMetadata metadata;
    private final Recipe comparatorRecipe;
    private ManagedCollection collection;

    public CollectionBasedServiceReferenceRecipe(BlueprintContext blueprintContext,
                                                 BlueprintContextEventSender sender,
                                                 RefCollectionMetadata metadata,
                                                 Recipe listenersRecipe,
                                                 Recipe comparatorRecipe) {
        super(blueprintContext, sender, metadata, listenersRecipe);
        this.metadata = metadata;
        this.comparatorRecipe = comparatorRecipe;
    }

    public boolean canCreate(Type type) {
        return true;
    }

    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Comparator comparator = null;
        try {
            if (comparatorRecipe != null) {
                comparator = (Comparator) comparatorRecipe.create(proxyClassLoader);
            } else if (metadata.getOrderingBasis() != 0) {
                comparator = new NaturalOrderComparator();
            }
            boolean orderReferences = metadata.getOrderingBasis() == RefCollectionMetadata.ORDERING_BASIS_SERVICE_REFERENCE;
            boolean memberReferences = isReferenceCollection(expectedType) || metadata.getMemberType() == RefCollectionMetadata.MEMBER_TYPE_SERVICE_REFERENCE;
            if (metadata.getCollectionType() == List.class) {
                if (comparator != null) {
                    collection = new ManagedSortedList(memberReferences, orderReferences, comparator);
                } else {
                    collection = new ManagedList(memberReferences);
                }
            } else if (metadata.getCollectionType() == Set.class) {
                if (comparator != null) {
                    collection = new ManagedSortedSet(memberReferences, orderReferences, comparator);
                } else {
                    collection = new ManagedSet(memberReferences);
                }
            } else {
                throw new IllegalArgumentException("Unsupported collection type " + metadata.getCollectionType().getName());
            }

            // Create the listeners and initialize them
            createListeners();

            // Add the created proxy to the context
            if (getName() != null) {
                ExecutionContext.getContext().addObject(getName(), collection);
            }

            // Start tracking the service
            tracker.registerServiceListener(this);
            retrack();
            
            return collection;
        } catch (Throwable t) {
            throw new ConstructionException(t);
        }
    }
    
    private boolean isReferenceCollection(Type expectedType) {
        Class componentType = getComponentType(expectedType);
        return ServiceReference.class.equals(componentType);
    }
    
    private Class getComponentType(Type expectedType) {
        Type[] typeParameters = RecipeHelper.getTypeParameters(Collection.class, expectedType);
        Class componentType = Object.class;
        if (typeParameters != null && typeParameters.length == 1 && typeParameters[0] instanceof Class) {
            componentType = (Class) typeParameters[0];
        }
        return componentType;
    }
    
    public void stop() {
        super.stop();
        List<ServiceDispatcher> dispatchers = new ArrayList<ServiceDispatcher>(collection.getDispatchers());
        for (ServiceDispatcher dispatcher : dispatchers) {
            untrack(dispatcher.reference);
        }
    }

    private void retrack() {
        List<ServiceReference> refs = tracker.getServiceReferences();
        if (refs != null) {
            for (ServiceReference ref : refs) {
                track(ref);
            }
        }
    }

    protected void track(ServiceReference reference) {
        try {
            ServiceDispatcher dispatcher = new ServiceDispatcher(reference);
            dispatcher.proxy = createProxy(dispatcher, Arrays.asList((String[]) reference.getProperty(Constants.OBJECTCLASS)));
            synchronized (collection) {
                collection.addDispatcher(dispatcher);
            }
            for (Listener listener : listeners) {
                listener.bind(dispatcher.reference, dispatcher.proxy);
            }
        } catch (Throwable t) {
            t.printStackTrace(); // TODO: log
        }
    }

    protected void untrack(ServiceReference reference) {
        ServiceDispatcher dispatcher = collection.findDispatcher(reference);
        if (dispatcher != null) {
            for (Listener listener : listeners) {
                listener.unbind(dispatcher.reference, dispatcher.proxy);
            }
            synchronized (collection) {
                collection.removeDispatcher(dispatcher);
            }
            dispatcher.destroy();
        }
    }

    public static class NaturalOrderComparator implements Comparator<Comparable> {

        public int compare(Comparable o1, Comparable o2) {
            return o1.compareTo(o2);
        }

    }


    public static class ServiceDispatcher implements Dispatcher, Destroyable {

        public ServiceReference reference;
        public Object service;
        public Object proxy;

        public ServiceDispatcher(ServiceReference reference) throws Exception {
            this.reference = reference;
            this.service = reference.getBundle().getBundleContext().getService(reference);
        }

        public void destroy() {
            if (reference != null) {
                reference.getBundle().getBundleContext().ungetService(reference);
                service = null;
                proxy = null;
            }
        }

        public Object loadObject() throws Exception {
            if (service == null) {
                throw new ServiceUnregisteredException();
            }
            return service;
        }
    }

    public static class DispatcherComparator implements Comparator<ServiceDispatcher> {

        private final Comparator comparator;
        private final boolean orderingReferences;

        public DispatcherComparator(Comparator comparator, boolean orderingReferences) {
            this.comparator = comparator;
            this.orderingReferences = orderingReferences;
        }

        public int compare(ServiceDispatcher d1, ServiceDispatcher d2) {
            return comparator.compare(getOrdering(d1), getOrdering(d2));
        }

        protected Object getOrdering(ServiceDispatcher d) {
            return orderingReferences ? d.reference : d.proxy;
        }

    }

    public static class ManagedCollection extends AbstractCollection {

        protected final boolean references;
        protected final DynamicCollection<ServiceDispatcher> dispatchers;

        public ManagedCollection(boolean references, DynamicCollection<ServiceDispatcher> dispatchers) {
            this.references = references;
            this.dispatchers = dispatchers;
        }

        public boolean addDispatcher(ServiceDispatcher dispatcher) {
            return dispatchers.add(dispatcher);
        }

        public boolean removeDispatcher(ServiceDispatcher dispatcher) {
            return dispatchers.remove(dispatcher);
        }

        public DynamicCollection<ServiceDispatcher> getDispatchers() {
            return dispatchers;
        }

        public ServiceDispatcher findDispatcher(ServiceReference reference) {
            for (ServiceDispatcher dispatcher : dispatchers) {
                if (dispatcher.reference == reference) {
                    return dispatcher;
                }
            }
            return null;
        }

        public Iterator iterator() {
            return new ManagedIterator(dispatchers.iterator());
        }

        public int size() {
            return dispatchers.size();
        }

        protected Object getMember(ServiceDispatcher d) {
            return references ? d.reference : d.proxy;
        }

        @Override
        public boolean add(Object o) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        @Override
        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("This collection is read only");
        }

        @Override
        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        @Override
        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        public class ManagedIterator implements Iterator {

            private final Iterator<ServiceDispatcher> iterator;

            public ManagedIterator(Iterator<ServiceDispatcher> iterator) {
                this.iterator = iterator;
            }

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Object next() {
                return getMember(iterator.next());
            }

            public void remove() {
                throw new UnsupportedOperationException("This collection is read only");
            }
        }

    }


    public static class ManagedList extends ManagedCollection implements List, RandomAccess {

        protected DynamicList<ServiceDispatcher> dispatchers;

        public ManagedList(boolean references) {
            this(references,  new DynamicList<ServiceDispatcher>());
        }

        protected ManagedList(boolean references, DynamicList<ServiceDispatcher> dispatchers) {
            super(references, dispatchers);
            this.dispatchers = dispatchers;
        }

        @Override
        public int size() {
            return dispatchers.size();
        }

        public Object get(int index) {
            return getMember(dispatchers.get(index));
        }

        public int indexOf(Object o) {
            if (o == null) {
                throw new NullPointerException();
            }
            ListIterator e = listIterator();
            while (e.hasNext()) {
                if (o.equals(e.next())) {
                    return e.previousIndex();
                }
            }
            return -1;
        }

        public int lastIndexOf(Object o) {
            if (o == null) {
                throw new NullPointerException();
            }
            ListIterator e = listIterator(size());
            while (e.hasPrevious()) {
                if (o.equals(e.previous())) {
                    return e.nextIndex();
                }
            }
            return -1;
        }

        public ListIterator listIterator() {
            return listIterator(0);
        }

        public ListIterator listIterator(int index) {
            return new ManagedListIterator(dispatchers.listIterator(index));
        }

        public List<ServiceDispatcher> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException("Not implemented");
        }

        public Object set(int index, Object element) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        public void add(int index, Object element) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        public Object remove(int index) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        public boolean addAll(int index, Collection c) {
            throw new UnsupportedOperationException("This collection is read only");
        }

        public class ManagedListIterator implements ListIterator {

            protected final ListIterator<ServiceDispatcher> iterator;

            public ManagedListIterator(ListIterator<ServiceDispatcher> iterator) {
                this.iterator = iterator;
            }

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Object next() {
                return getMember(iterator.next());
            }

            public boolean hasPrevious() {
                return iterator.hasPrevious();
            }

            public Object previous() {
                return getMember(iterator.previous());
            }

            public int nextIndex() {
                return iterator.nextIndex();
            }

            public int previousIndex() {
                return iterator.previousIndex();
            }

            public void remove() {
                throw new UnsupportedOperationException("This collection is read only");
            }

            public void set(Object o) {
                throw new UnsupportedOperationException("This collection is read only");
            }

            public void add(Object o) {
                throw new UnsupportedOperationException("This collection is read only");
            }
        }

    }

    public static class ManagedSortedList extends ManagedList {

        public ManagedSortedList(boolean references, boolean orderingReferences, Comparator comparator) {
            super(references, new DynamicSortedList<ServiceDispatcher>(new DispatcherComparator(comparator, orderingReferences)));
        }

    }

    public static class ManagedSet extends ManagedCollection implements Set {

        public ManagedSet(boolean references) {
            this(references, new DynamicSet<ServiceDispatcher>());
        }

        protected ManagedSet(boolean references, DynamicSet<ServiceDispatcher> dispatchers) {
            super(references, dispatchers);
        }

    }

    public static class ManagedSortedSet extends ManagedSet implements SortedSet {

        protected final DynamicSortedSet<ServiceDispatcher> dispatchers;
        protected final Comparator comparator;

        public ManagedSortedSet(boolean references, boolean orderingReferences, Comparator comparator) {
            super(references, new DynamicSortedSet<ServiceDispatcher>(new DispatcherComparator(comparator, orderingReferences)));
            this.dispatchers = (DynamicSortedSet<ServiceDispatcher>) super.dispatchers;
            this.comparator =  comparator;
        }

        public Comparator comparator() {
            return comparator;
        }

        public SortedSet subSet(Object fromElement, Object toElement) {
            throw new UnsupportedOperationException("Not implemented");
        }

        public SortedSet headSet(Object toElement) {
            throw new UnsupportedOperationException("Not implemented");
        }

        public SortedSet tailSet(Object fromElement) {
            throw new UnsupportedOperationException("Not implemented");
        }

        public Object first() {
            return getMember(dispatchers.first());
        }

        public Object last() {
            return getMember(dispatchers.last());
        }

    }

}
