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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.aries.cdi.container.internal.container.BeanServiceObjectsImpl;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.reference.BeanServiceObjects;
import org.osgi.service.cdi.reference.BindBeanServiceObjects;
import org.osgi.service.log.Logger;

public class BindBeanServiceObjectsImpl<T> implements Binder<T>, BindBeanServiceObjects<T> {

	private final ContainerState _containerState;
	private final Logger _log;

	private final List<ServiceReference<T>> _queue = new CopyOnWriteArrayList<>();
	private final AtomicBoolean _enqueue = new AtomicBoolean(true);
	private volatile Optional<Consumer<BeanServiceObjects<T>>> onAdding = Optional.empty();
	private volatile Optional<Consumer<BeanServiceObjects<T>>> onUpdate = Optional.empty();
	private volatile Optional<Consumer<BeanServiceObjects<T>>> onRemove = Optional.empty();

	private volatile BeanServiceObjects<T> serviceObjects;

	public BindBeanServiceObjectsImpl(ContainerState containerState) {
		_containerState = containerState;
		_log = _containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public BindBeanServiceObjectsImpl<T> addingService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			_queue.add(reference);
			return this;
		}

		BundleContext bundleContext = _containerState.bundleContext();
		serviceObjects = new BeanServiceObjectsImpl<T>(bundleContext.getServiceObjects(reference));

		onAdding.ifPresent(
			c -> {
				try {
					c.accept(serviceObjects);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
			}
		);

		return this;
	}

	@Override
	public BindBeanServiceObjectsImpl<T> modifiedService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			return this; // i.e. do nothing
		}

		onUpdate.ifPresent(
			c -> {
				try {
					c.accept(serviceObjects);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
			}
		);

		return this;
	}

	@Override
	public BindBeanServiceObjectsImpl<T> removedService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			_queue.remove(reference);
			return this;
		}

		onRemove.ifPresent(
			c -> {
				try {
					c.accept(serviceObjects);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
			}
		);

		return this;
	}

	@Override
	public void bind() {
		_enqueue.set(false);
		_queue.removeIf(
			reference -> {
				addingService(reference);
				return true;
			}
		);
	}

	@Override
	public BindBeanServiceObjectsImpl<T> adding(Consumer<BeanServiceObjects<T>> action) {
		onAdding = Optional.ofNullable(action);
		return this;
	}

	@Override
	public BindBeanServiceObjectsImpl<T> modified(Consumer<BeanServiceObjects<T>> consumer) {
		onUpdate = Optional.ofNullable(consumer);
		return this;
	}

	@Override
	public BindBeanServiceObjectsImpl<T> removed(Consumer<BeanServiceObjects<T>> consumer) {
		onRemove = Optional.ofNullable(consumer);
		return this;
	}

}