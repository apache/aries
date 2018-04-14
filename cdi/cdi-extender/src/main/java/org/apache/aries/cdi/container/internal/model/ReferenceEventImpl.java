/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.model;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.ReferenceServiceObjectsImpl;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.reference.ReferenceEvent;
import org.osgi.service.cdi.reference.ReferenceServiceObjects;
import org.osgi.service.log.Logger;

public class ReferenceEventImpl<T> implements ParameterizedType, ReferenceEvent<T> {

	private final Type[] _actualTypeArguments;
	private final ContainerState _containerState;
	private final Logger _log;
	private final Type _rawType = ReferenceEvent.class;

	private final List<ServiceReference<T>> _queue = new CopyOnWriteArrayList<>();
	private final AtomicBoolean _enqueue = new AtomicBoolean(true);
	private List<Consumer<T>> onAdding = new CopyOnWriteArrayList<>();
	private List<Consumer<ServiceReference<T>>> onAddingServiceReference = new CopyOnWriteArrayList<>();
	private List<Consumer<ReferenceServiceObjects<T>>> onAddingServiceObjects = new CopyOnWriteArrayList<>();
	private List<Consumer<Map<String, ?>>> onAddingProperties = new CopyOnWriteArrayList<>();
	private List<Consumer<Entry<Map<String, ?>, T>>> onAddingTuple = new CopyOnWriteArrayList<>();
	private List<Consumer<T>> onUpdate = new CopyOnWriteArrayList<>();
	private List<Consumer<ServiceReference<T>>> onUpdateServiceReference = new CopyOnWriteArrayList<>();
	private List<Consumer<ReferenceServiceObjects<T>>> onUpdateServiceObjects = new CopyOnWriteArrayList<>();
	private List<Consumer<Map<String, ?>>> onUpdateProperties = new CopyOnWriteArrayList<>();
	private List<Consumer<Entry<Map<String, ?>, T>>> onUpdateTuple = new CopyOnWriteArrayList<>();
	private List<Consumer<T>> onRemove = new CopyOnWriteArrayList<>();
	private List<Consumer<ServiceReference<T>>> onRemoveServiceReference = new CopyOnWriteArrayList<>();
	private List<Consumer<ReferenceServiceObjects<T>>> onRemoveServiceObjects = new CopyOnWriteArrayList<>();
	private List<Consumer<Map<String, ?>>> onRemoveProperties = new CopyOnWriteArrayList<>();
	private List<Consumer<Entry<Map<String, ?>, T>>> onRemoveTuple = new CopyOnWriteArrayList<>();

	private volatile T service;
	private volatile ReferenceServiceObjects<T> serviceObjects;

	public ReferenceEventImpl(ContainerState containerState, Class<T> serviceClass) {
		_containerState = containerState;
		_actualTypeArguments = new Type[] {serviceClass};
		_log = _containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public Type getRawType() {
		return _rawType;
	}

	@Override
	public Type[] getActualTypeArguments() {
		return _actualTypeArguments;
	}

	@Override
	public Type getOwnerType() {
		return null;
	}

	public ReferenceEventImpl<T> addingService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			_queue.add(reference);
			return this;
		}

		BundleContext bundleContext = _containerState.bundleContext();
		service = bundleContext.getService(reference);
		serviceObjects = new ReferenceServiceObjectsImpl<T>(bundleContext.getServiceObjects(reference));
		Map<String, Object> map = Maps.of(reference.getProperties());
		Entry<Map<String, ?>, T> tuple = new AbstractMap.SimpleImmutableEntry<>(map, service);

		onAdding.removeIf(
			c -> {
				try {
					c.accept(service);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onAddingServiceReference.removeIf(
			c -> {
				try {
					c.accept(reference);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onAddingServiceObjects.removeIf(
			c -> {
				try {
					c.accept(serviceObjects);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onAddingProperties.removeIf(
			c -> {
				try {
					c.accept(map);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onAddingTuple.removeIf(
			c -> {
				try {
					c.accept(tuple);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);

		return this;
	}

	public ReferenceEventImpl<T> modifiedService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			return this; // i.e. do nothing
		}

		Map<String, Object> map = Maps.of(reference.getProperties());
		Entry<Map<String, ?>, T> tuple = new AbstractMap.SimpleImmutableEntry<>(map, service);

		onUpdate.removeIf(
			c -> {
				try {
					c.accept(service);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onUpdateServiceReference.removeIf(
			c -> {
				try {
					c.accept(reference);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onUpdateServiceObjects.removeIf(
			c -> {
				try {
					c.accept(serviceObjects);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onUpdateProperties.removeIf(
			c -> {
				try {
					c.accept(map);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onUpdateTuple.removeIf(
			c -> {
				try {
					c.accept(tuple);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);

		return this;
	}

	public ReferenceEventImpl<T> removedService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			_queue.remove(reference);
			return this;
		}

		Map<String, Object> map = Maps.of(reference.getProperties());
		Entry<Map<String, ?>, T> tuple = new AbstractMap.SimpleImmutableEntry<>(map, service);

		onRemove.removeIf(
			c -> {
				try {
					c.accept(service);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onRemoveServiceReference.removeIf(
			c -> {
				try {
					c.accept(reference);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onRemoveServiceObjects.removeIf(
			c -> {
				try {
					c.accept(serviceObjects);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onRemoveProperties.removeIf(
			c -> {
				try {
					c.accept(map);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);
		onRemoveTuple.removeIf(
			c -> {
				try {
					c.accept(tuple);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
				return true;
			}
		);

		return this;
	}

	public boolean flush() {
		_enqueue.set(false);
		_queue.removeIf(
			reference -> {
				addingService(reference);
				return true;
			}
		);
		return true;
	}

	@Override
	public void onAdding(Consumer<T> action) {
		onAdding.add(action);
	}

	@Override
	public void onAddingServiceReference(Consumer<ServiceReference<T>> consumer) {
		onAddingServiceReference.add(consumer);
	}

	@Override
	public void onAddingServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		onAddingServiceObjects.add(consumer);
	}

	@Override
	public void onAddingProperties(Consumer<Map<String, ?>> consumer) {
		onAddingProperties.add(consumer);
	}

	@Override
	public void onAddingTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		onAddingTuple.add(consumer);
	}

	@Override
	public void onUpdate(Consumer<T> consumer) {
		onUpdate.add(consumer);
	}

	@Override
	public void onUpdateServiceReference(Consumer<ServiceReference<T>> consumer) {
		onUpdateServiceReference.add(consumer);
	}

	@Override
	public void onUpdateServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		onUpdateServiceObjects.add(consumer);
	}

	@Override
	public void onUpdateProperties(Consumer<Map<String, ?>> consumer) {
		onUpdateProperties.add(consumer);
	}

	@Override
	public void onUpdateTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		onUpdateTuple.add(consumer);
	}

	@Override
	public void onRemove(Consumer<T> consumer) {
		onRemove.add(consumer);
	}

	@Override
	public void onRemoveServiceReference(Consumer<ServiceReference<T>> consumer) {
		onRemoveServiceReference.add(consumer);
	}

	@Override
	public void onRemoveServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		onRemoveServiceObjects.add(consumer);
	}

	@Override
	public void onRemoveProperties(Consumer<Map<String, ?>> consumer) {
		onRemoveProperties.add(consumer);
	}

	@Override
	public void onRemoveTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		onRemoveTuple.add(consumer);
	}

}