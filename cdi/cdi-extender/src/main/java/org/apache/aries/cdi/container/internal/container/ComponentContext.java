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

package org.apache.aries.cdi.container.internal.container;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.aries.cdi.container.internal.model.ExtendedActivationDTO;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.runtime.dto.ActivationDTO;

public class ComponentContext implements Context {

	@SuppressWarnings("unchecked")
	public void destroy() {
		if (!isActive()) return;

		Map<Class<?>, BeanInstance<?>> map = _beans.computeIfAbsent(
			_componentModel.get(), k -> new ConcurrentHashMap<>());

		if (map == null) {
			return;
		}

		for (BeanInstance<?> beanInstance : map.values()) {
			beanInstance.getBean().destroy(beanInstance.getInstance(), beanInstance.getCreationalContext());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Contextual<T> contextual) {
		if (!isActive()) return null;

		@SuppressWarnings("rawtypes")
		Bean bean = (Bean)contextual;
		Class<?> beanClass = bean.getBeanClass();

		Map<Class<?>, BeanInstance<?>> map = _beans.computeIfAbsent(
			_componentModel.get(), k -> new ConcurrentHashMap<>());

		if (map.containsKey(beanClass)) {
			return (T)map.get(beanClass).getInstance();
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
		if (!isActive()) return null;

		@SuppressWarnings("rawtypes")
		Bean bean = (Bean)contextual;
		Class<?> beanClass = bean.getBeanClass();

		Map<Class<?>, BeanInstance<?>> map = _beans.computeIfAbsent(
			_componentModel.get(), k -> new ConcurrentHashMap<>());

		if (map.containsKey(beanClass)) {
			return (T)map.get(beanClass).getInstance();
		}

		T instance = (T) bean.create(creationalContext);

		map.put(beanClass, new BeanInstance<>(bean, creationalContext, instance));

		return instance;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return ComponentScoped.class;
	}

	@Override
	public boolean isActive() {
		return _componentModel.get() != null;
	}

	private static final Map<ActivationDTO, Map<Class<?>, BeanInstance<?>>> _beans = new ConcurrentHashMap<>();
	private static final ThreadLocal<ExtendedActivationDTO> _componentModel = new ThreadLocal<>();

	public static class With implements AutoCloseable {

		public With(ExtendedActivationDTO activationDTO) {
			_componentModel.set(activationDTO);
		}

		@Override
		public void close() {
			_componentModel.set(null);
		}

		public static ExtendedActivationDTO current() {
			return _componentModel.get();
		}

	}

	@SuppressWarnings("rawtypes")
	private class BeanInstance<T> {

		public BeanInstance(Bean<T> bean, CreationalContext<T> creationalContext, T instance) {
			_bean = bean;
			_creationalContext = creationalContext;
			_instance = instance;
		}

		public Bean getBean() {
			return _bean;
		}

		public CreationalContext<T> getCreationalContext() {
			return _creationalContext;
		}

		public T getInstance() {
			return _instance;
		}

		private final Bean<T> _bean;
		private final CreationalContext<T> _creationalContext;
		private final T _instance;

	}

}