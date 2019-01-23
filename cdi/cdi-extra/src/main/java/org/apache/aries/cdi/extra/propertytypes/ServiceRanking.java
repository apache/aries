/*
 * Copyright (c) OSGi Alliance (2017, 2019). All Rights Reserved.
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
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.framework.Constants;
import org.osgi.service.cdi.annotations.BeanPropertyType;

/**
 * Bean Property Type for the {@code service.ranking} service property.
 * <p>
 * This annotation can be used as defined by {@link BeanPropertyType} to declare
 * the value of the {@link Constants#SERVICE_RANKING} service property.
 */
@BeanPropertyType
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER })
public @interface ServiceRanking {

	public static final class Literal extends AnnotationLiteral<ServiceRanking> implements ServiceRanking {

		private static final long serialVersionUID = 1L;

		public static final Literal of(int value) {
			return new Literal(value);
		}

		private Literal(int value) {
			_value = value;
		}

		@Override
		public int value() {
			return _value;
		}

		private final int _value;
	}

	/**
	 * Service property identifying a service's ranking.
	 *
	 * @return The service ranking.
	 * @see Constants#SERVICE_RANKING
	 */
	int value();
}
