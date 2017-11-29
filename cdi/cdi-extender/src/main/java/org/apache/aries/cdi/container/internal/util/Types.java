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

package org.apache.aries.cdi.container.internal.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Types {

	public static String getName(InjectionPoint injectionPoint) {
		return getName(injectionPoint.getType());
	}

	public static String getName(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)type;

			Type rawType = pt.getRawType();

			if (rawType instanceof Class) {
				Class<?> clazz = (Class<?>)rawType;

				return clazz.getSimpleName();
			}
			else {
				return rawType.getTypeName();
			}
		}
		else if (type instanceof GenericArrayType) {
			GenericArrayType gat = (GenericArrayType)type;

			Type genericComponentType = gat.getGenericComponentType();

			if (genericComponentType instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType)genericComponentType;

				Type rawType = pt.getRawType();

				if (rawType instanceof Class) {
					Class<?> clazz = (Class<?>)rawType;

					return clazz.getSimpleName();
				}
				else {
					return rawType.getTypeName();
				}
			}
			else if (genericComponentType instanceof Class) {
				Class<?> clazz = (Class<?>)genericComponentType;

				return clazz.getSimpleName();
			}
			else {
				return genericComponentType.getTypeName();
			}
		}
		else if (type instanceof Class) {
			Class<?> clazz = (Class<?>)type;

			String simpleName = clazz.getSimpleName();

			char lowerCase = Character.toLowerCase(simpleName.charAt(0));

			return lowerCase + simpleName.substring(1, simpleName.length());
		}

		return type.getTypeName();
	}

	public static Class<?>[] types(
		ComponentModel componentModel, Class<?> beanClass, ClassLoader classLoader) {

		List<Class<?>> classes = new ArrayList<>();

		if (!componentModel.isService()) {
			return new Class<?>[0];
		}
		else if (!componentModel.getProvides().isEmpty()) {
			for (String provide : componentModel.getProvides()) {
				try {
					classes.add(classLoader.loadClass(provide));
				}
				catch (ReflectiveOperationException roe) {
					if (_log.isWarnEnabled()) {
						_log.warn(
							"CDIe - component {} cannot load provided type {}. Skipping!",
							componentModel.getBeanClass(), provide, roe);
					}
				}
			}
		}
		else {
			Class<?>[] interfaces = beanClass.getInterfaces();

			if (interfaces.length > 0) {
				for (Class<?> iface : interfaces) {
					classes.add(iface);
				}
			}
			else {
				classes.add(beanClass);
			}
		}

		return classes.toArray(new Class[0]);
	}

	public static final Logger _log = LoggerFactory.getLogger(Types.class);

}