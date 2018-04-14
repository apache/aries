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

package org.apache.aries.cdi.provider;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

public class CDIProvider implements javax.enterprise.inject.spi.CDIProvider {

	private static class CdiExtenderCDI extends CDI<Object> {

		@Override
		public void destroy(Object instance) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BeanManager getBeanManager() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isAmbiguous() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isUnsatisfied() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<Object> iterator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Instance<Object> select(Annotation... qualifiers) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
			throw new UnsupportedOperationException();
		}

	}

	private static final CDI<Object> _cdi = new CdiExtenderCDI();

	@Override
	public CDI<Object> getCDI() {
		return _cdi;
	}

}
