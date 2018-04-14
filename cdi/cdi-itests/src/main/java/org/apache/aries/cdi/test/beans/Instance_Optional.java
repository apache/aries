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

import java.util.Collection;
import java.util.concurrent.Callable;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;

@Component
@Service({BeanService.class, Instance_Optional.class})
@SuppressWarnings("rawtypes")
public class Instance_Optional implements BeanService<Callable<String>> {

	@Override
	public String doSomething() {
		int count = 0;
		Collection<Callable> callables = _instance.get();
		for (Callable callable : callables) {
			System.out.println(callable);
			count++;
		}
		return String.valueOf(count);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Callable<String> get() {
		Collection<Callable> iterator = _instance.get();
		return iterator.iterator().next();
	}

	@Inject
	@Reference
	Instance<Collection<Callable>> _instance;

}
