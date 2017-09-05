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

package org.apache.aries.cdi.container.internal.reference;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.Context;
import org.apache.aries.cdi.container.internal.model.ServiceEventImpl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.service.cdi.annotations.ServiceEvent;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ReferenceCallback implements ServiceTrackerCustomizer<Object, Object> {

	public static class Builder {

		public Builder(Context context) {
			this(null, null, context);
		}

		public Builder(
			ComponentModel componentModel,
			ContainerState containerState,
			Context context) {

			Objects.requireNonNull(context);
			_componentModel = componentModel;
			_containerState = containerState;
			_context = context;
		}

		public ReferenceCallback build() {
			Objects.requireNonNull(_cardinality);
			Objects.requireNonNull(_collectionType);
			return new ReferenceCallback(_componentModel, _containerState, _context, _name, _cardinality, _collectionType, _onAdd, _onUpdate, _onRemove);
		}

		public Builder cardinality(ReferenceCardinality cardinality) {
			_cardinality = cardinality;
			return this;
		}

		public Builder collectionType(CollectionType collectionType) {
			_collectionType = collectionType;
			return this;
		}

		public Builder name(String name) {
			_name = name;
			return this;
		}

		public Builder onAdd(Consumer<ReferenceCallback> onAdd) {
			_onAdd = onAdd;
			return this;
		}

		public Builder onUpdate(Consumer<ReferenceCallback> onUpdate) {
			_onUpdate = onUpdate;
			return this;
		}

		public Builder onRemove(Consumer<ReferenceCallback> onRemove) {
			_onRemove = onRemove;
			return this;
		}

		private ReferenceCardinality _cardinality;
		private CollectionType _collectionType;
		private ComponentModel _componentModel;
		private ContainerState _containerState;
		private Context _context;
		private String _name;
		private Consumer<ReferenceCallback> _onAdd;
		private Consumer<ReferenceCallback> _onUpdate;
		private Consumer<ReferenceCallback> _onRemove;

	}

	private ReferenceCallback(
		ComponentModel componentModel,
		ContainerState containerState,
		Context context,
		String name,
		ReferenceCardinality cardinality,
		CollectionType collectionType,
		Consumer<ReferenceCallback> onAdd,
		Consumer<ReferenceCallback> onUpdate,
		Consumer<ReferenceCallback> onRemove) {

		_componentModel = Optional.ofNullable(componentModel);
		_containerState = Optional.ofNullable(containerState);
		_context = context;
		_name = Optional.ofNullable(name);
		_cardinality = cardinality;
		_collectionType = collectionType;
		_onAdd = Optional.ofNullable(onAdd);
		_onUpdate = Optional.ofNullable(onUpdate);
		_onRemove = Optional.ofNullable(onRemove);
	}

	Optional<ObserverMethod<ServiceEvent<?>>> observer() {
		return _componentModel.flatMap(
			cm -> _containerState.flatMap(
				cs -> _name.flatMap(
					n -> {
						Map<String, ObserverMethod<ServiceEvent<?>>> map = cs.referenceObservers().computeIfAbsent(
							cm, k -> new LinkedHashMap<>());

						return Optional.ofNullable(map.get(n));
					}
				)
			)
		);
	}

	@Override
	public Object addingService(ServiceReference<Object> reference) {
		_lock.lock();

		try {
			Object instance;

			switch (_collectionType) {
				case PROPERTIES:
					instance = new UnmodifiableMap(reference);
					break;
				case REFERENCE:
					instance = reference;
					break;
				case SERVICEOBJECTS:
					instance = _context.getServiceObjects(reference);
					break;
				case TUPLE:
					instance = new AbstractMap.SimpleImmutableEntry<Map<String, Object>, Object>(
						new UnmodifiableMap(reference), _context.getService(reference));
					break;
				default:
					instance = _context.getService(reference);
					break;
			}

			final Object object = instance;

			if (_queueing.get()) {
				_queue.put(reference, object);
			}
			else {
				_tracked.put(reference, object);

				observer().ifPresent(o -> o.notify(event(object, ServiceEventImpl.Event.ADDING)));
			}

			_onAdd.ifPresent(f -> f.accept(this));

			return instance;
		}
		finally {
			_lock.unlock();
		}
	}

	public void flush() {
		_lock.lock();

		try {
			_queueing.set(false);

			_queue.entrySet().removeIf(
				entry -> {
					_tracked.put(entry.getKey(), entry.getValue());

					observer().ifPresent(o -> o.notify(event(entry.getValue(), ServiceEventImpl.Event.ADDING)));

					return true;
				}
			);
		}
		finally {
			_lock.unlock();
		}
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
		_lock.lock();

		try {
			Object instance = null;

			switch (_collectionType) {
				case PROPERTIES:
					instance = new UnmodifiableMap(reference);
					break;
				case REFERENCE:
					instance = reference;
					break;
				case SERVICEOBJECTS:
					break;
				case TUPLE:
					@SuppressWarnings("unchecked")
					Map.Entry<Map<String, Object>, Object> entry = (Map.Entry<Map<String, Object>, Object>)service;

					instance = new AbstractMap.SimpleImmutableEntry<Map<String, Object>, Object>(
						new UnmodifiableMap(reference), entry.getValue());
					break;
				default:
					break;
			}

			if (instance != null) {
				final Object object = instance;

				if (_queueing.get()) {
					_queue.put(reference, object);
				}
				else {
					_tracked.put(reference, object);

					observer().ifPresent(o -> o.notify(event(object, ServiceEventImpl.Event.MODIFIED)));
				}
			}

			_onUpdate.ifPresent(f -> f.accept(this));
		}
		finally {
			_lock.unlock();
		}
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		_lock.lock();

		try {
			_context.ungetService(reference);

			if (_queueing.get()) {
				_queue.remove(reference);
			}
			else {
				final Object instance = _tracked.remove(reference);

				observer().ifPresent(o -> o.notify(event(instance, ServiceEventImpl.Event.REMOVED)));
			}

			_onRemove.ifPresent(f -> f.accept(this));
		}
		finally {
			_lock.unlock();
		}
	}

	<T> ServiceEvent<T> event(T t, ServiceEventImpl.Event event) {
		return new ServiceEventImpl<T>(t, event);
	}

	public boolean resolved() {
		if (((_cardinality == ReferenceCardinality.MANDATORY) ||
			(_cardinality == ReferenceCardinality.AT_LEAST_ONE))) {

			if (_queue.size() + _tracked.size() >= 1) {
				return true;
			}

			return false;
		}

		return true;
	}

	public Map<ServiceReference<?>, Object> tracked() {
		return _tracked;
	}

	private final ReferenceCardinality _cardinality;
	private final CollectionType _collectionType;
	private final Optional<ComponentModel> _componentModel;
	private final Optional<ContainerState> _containerState;
	private final Context _context;
	private final Lock _lock = new ReentrantLock();
	private final Optional<String> _name;
	private final Optional<Consumer<ReferenceCallback>> _onAdd;
	private final Optional<Consumer<ReferenceCallback>> _onUpdate;
	private final Optional<Consumer<ReferenceCallback>> _onRemove;
	private final AtomicBoolean _queueing = new AtomicBoolean(true);
	private final Map<ServiceReference<?>, Object> _queue = new ConcurrentHashMap<>();
	private final Map<ServiceReference<?>, Object> _tracked = new ConcurrentHashMap<>();

	private static class UnmodifiableMap implements Map<String, Object> {

		public UnmodifiableMap(ServiceReference<?> sr) {
			_sr = sr;;
			_keys = Arrays.asList(_sr.getPropertyKeys());
		}

		@Override
		public int size() {
			return _keys.size();
		}

		@Override
		public boolean isEmpty() {
			return _keys.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return _keys.contains(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return values().contains(value);
		}

		@Override
		public Object get(Object key) {
			return _sr.getProperty((String)key);
		}

		@Override
		public Object put(String key, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(Map<? extends String, ? extends Object> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> keySet() {
			return new HashSet<>(_keys);
		}

		@Override
		public Collection<Object> values() {
			List<Object> values = new ArrayList<>();

			for (String key : _keys) {
				values.add(_sr.getProperty(key));
			}

			return Collections.unmodifiableCollection(values);
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			Set<Map.Entry<String, Object>> entries = new HashSet<>();

			for (String key : _keys) {
				entries.add(new AbstractMap.SimpleImmutableEntry<>(key, _sr.getProperty(key)));
			}

			return Collections.unmodifiableSet(entries);
		}

		private final ServiceReference<?> _sr;
		private List<String> _keys;

	}

}
