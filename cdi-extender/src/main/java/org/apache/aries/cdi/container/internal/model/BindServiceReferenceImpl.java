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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.reference.BindServiceReference;
import org.osgi.service.log.Logger;

public class BindServiceReferenceImpl<T> implements Binder<T>, BindServiceReference<T> {

	private final ContainerState _containerState;
	private final Logger _log;

	private final List<ServiceReference<T>> _queue = new CopyOnWriteArrayList<>();
	private final AtomicBoolean _enqueue = new AtomicBoolean(true);
	private volatile Optional<Consumer<ServiceReference<T>>> onAdding = Optional.empty();
	private volatile Optional<BiConsumer<ServiceReference<T>, T>> onAddingBi = Optional.empty();
	private volatile Optional<Consumer<ServiceReference<T>>> onUpdate = Optional.empty();
	private volatile Optional<BiConsumer<ServiceReference<T>, T>> onUpdateBi = Optional.empty();
	private volatile Optional<Consumer<ServiceReference<T>>> onRemove = Optional.empty();
	private volatile Optional<BiConsumer<ServiceReference<T>, T>> onRemoveBi = Optional.empty();

	private volatile T service;

	public BindServiceReferenceImpl(ContainerState containerState) {
		_containerState = containerState;
		_log = _containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public BindServiceReferenceImpl<T> addingService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			_queue.add(reference);
			return this;
		}

		BundleContext bundleContext = _containerState.bundleContext();
		service = bundleContext.getService(reference);

		onAdding.ifPresent(
			c -> {
				try {
					c.accept(reference);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
			}
		);
		onAddingBi.ifPresent(
			c -> {
				try {
					c.accept(reference, service);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
			}
		);

		return this;
	}

	@Override
	public BindServiceReferenceImpl<T> modifiedService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			return this; // i.e. do nothing
		}

		onUpdate.ifPresent(
			c -> {
				try {
					c.accept(reference);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
			}
		);
		onUpdateBi.ifPresent(
			c -> {
				try {
					c.accept(reference, service);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
			}
		);

		return this;
	}

	@Override
	public BindServiceReferenceImpl<T> removedService(ServiceReference<T> reference) {
		if (_enqueue.get()) {
			_queue.remove(reference);
			return this;
		}

		onRemove.ifPresent(
			c -> {
				try {
					c.accept(reference);
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR error in {}", this, t));
				}
			}
		);
		onRemoveBi.ifPresent(
			c -> {
				try {
					c.accept(reference, service);
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
	public BindServiceReferenceImpl<T> adding(Consumer<ServiceReference<T>> action) {
		onAdding = Optional.ofNullable(action);
		return this;
	}

	@Override
	public BindServiceReferenceImpl<T> adding(BiConsumer<ServiceReference<T>, T> action) {
		onAddingBi = Optional.ofNullable(action);
		return this;
	}

	@Override
	public BindServiceReferenceImpl<T> modified(Consumer<ServiceReference<T>> consumer) {
		onUpdate = Optional.ofNullable(consumer);
		return this;
	}

	@Override
	public BindServiceReferenceImpl<T> modified(BiConsumer<ServiceReference<T>, T> consumer) {
		onUpdateBi = Optional.ofNullable(consumer);
		return this;
	}

	@Override
	public BindServiceReferenceImpl<T> removed(Consumer<ServiceReference<T>> consumer) {
		onRemove = Optional.ofNullable(consumer);
		return this;
	}

	@Override
	public BindServiceReferenceImpl<T> removed(BiConsumer<ServiceReference<T>, T> consumer) {
		onRemoveBi = Optional.ofNullable(consumer);
		return this;
	}

}