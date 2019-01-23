/*
 * Copyright (c) OSGi Alliance (2016, 2019). All Rights Reserved.
 *
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

package org.apache.aries.cdi.extra.propertytypes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.framework.Constants;
import org.osgi.service.cdi.annotations.BeanPropertyType;
import org.osgi.service.cdi.annotations.FactoryComponent;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

/**
 * Component Property Type for the remote service properties for an exported
 * service.
 * <p>
 * This annotation can be used with {@link SingleComponent},
 * {@link FactoryComponent} or {@link Service} to declare the value of
 * the remote service properties for an exported service.
 */
@BeanPropertyType
@Retention(RUNTIME)
@Target({FIELD, METHOD, TYPE})
public @interface ExportedService {

	public static final class Literal extends AnnotationLiteral<ExportedService> implements ExportedService {

		private static final long serialVersionUID = 1L;

		public static final Literal of(
				Class<?>[] service_exported_interfaces,
				String[] service_exported_configs,
				String[] service_exported_intents,
				String[] service_exported_intents_extra,
				String[] service_intents) {

			return new Literal(
				service_exported_interfaces,
				service_exported_configs,
				service_exported_intents,
				service_exported_intents_extra,
				service_intents);
		}

		public Literal(
			Class<?>[] service_exported_interfaces,
			String[] service_exported_configs,
			String[] service_exported_intents,
			String[] service_exported_intents_extra,
			String[] service_intents) {

			this.service_exported_interfaces = service_exported_interfaces;
			this.service_exported_configs = service_exported_configs;
			this.service_exported_intents = service_exported_intents;
			this.service_exported_intents_extra = service_exported_intents_extra;
			this.service_intents = service_intents;
		}

		@Override
		public String[] service_exported_configs() {
			return service_exported_configs;
		}

		@Override
		public String[] service_exported_intents() {
			return service_exported_intents;
		}

		@Override
		public String[] service_exported_intents_extra() {
			return service_exported_intents_extra;
		}

		@Override
		public Class<?>[] service_exported_interfaces() {
			return service_exported_interfaces;
		}

		@Override
		public String[] service_intents() {
			return service_intents;
		}

		private final Class< ? >[] service_exported_interfaces;
		private final String[] service_exported_configs;
		private final String[] service_exported_intents;
		private final String[] service_exported_intents_extra;
		private final String[] service_intents;
	}

	/**
	 * Service property marking the service for export. It defines the
	 * interfaces under which the service can be exported.
	 * <p>
	 * If an empty array is specified, the property is not added to the
	 * component description.
	 *
	 * @return The exported service interfaces.
	 * @see Constants#SERVICE_EXPORTED_INTERFACES
	 */
	Class< ? >[] service_exported_interfaces();

	/**
	 * Service property identifying the configuration types that should be used
	 * to export the service.
	 * <p>
	 * If an empty array is specified, the default value, the property is not
	 * added to the component description.
	 *
	 * @return The configuration types.
	 * @see Constants#SERVICE_EXPORTED_CONFIGS
	 */
	String[] service_exported_configs() default {};

	/**
	 * Service property identifying the intents that the distribution provider
	 * must implement to distribute the service.
	 * <p>
	 * If an empty array is specified, the default value, the property is not
	 * added to the component description.
	 *
	 * @return The intents that the distribution provider must implement to
	 *         distribute the service.
	 * @see Constants#SERVICE_EXPORTED_INTENTS
	 */
	String[] service_exported_intents() default {};

	/**
	 * Service property identifying the extra intents that the distribution
	 * provider must implement to distribute the service.
	 * <p>
	 * If an empty array is specified, the default value, the property is not
	 * added to the component description.
	 *
	 * @return The extra intents that the distribution provider must implement
	 *         to distribute the service.
	 * @see Constants#SERVICE_EXPORTED_INTENTS_EXTRA
	 */
	String[] service_exported_intents_extra() default {};

	/**
	 * Service property identifying the intents that the distribution provider
	 * must implement to distribute the service.
	 * <p>
	 * If an empty array is specified, the default value, the property is not
	 * added to the component description.
	 *
	 * @return The intents that the service implements.
	 * @see Constants#SERVICE_INTENTS
	 */
	String[] service_intents() default {};
}
