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

package org.apache.aries.cdi.test.beans;

import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.service.cdi.annotations.ServiceScope;

@Component(
	service = {BeanService.class, Instance_Optional.class},
	scope = ServiceScope.SINGLETON
)
public class Instance_Optional implements BeanService<Callable<String>> {

	@Override
	public String doSomething() {
		int count = 0;
		for (Iterator<?> iterator = _instance.iterator();iterator.hasNext();) {
			System.out.println(iterator.next());
			count++;
		}
		return String.valueOf(count);
	}

	@Override
	public Callable<String> get() {
		Iterator<Callable<String>> iterator = _instance.iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	@Inject
	@Reference(cardinality = ReferenceCardinality.MULTIPLE)
	Instance<Callable<String>> _instance;

}
