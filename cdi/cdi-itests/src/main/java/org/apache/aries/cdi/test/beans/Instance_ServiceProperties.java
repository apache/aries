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
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.SingletonScoped;
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.service.cdi.annotations.ServiceScope;

@Component(
	service = {BeanService.class, Instance_ServiceProperties.class},
	scope = ServiceScope.SINGLETON
)
public class Instance_ServiceProperties implements BeanService<Map<String, Object>> {

	@Override
	public String doSomething() {
		int count = 0;
		for (Iterator<?> iterator = _instance.iterator();iterator.hasNext();) {
			iterator.next();
			count++;
		}
		return String.valueOf(count);
	}

	@Override
	public Map<String, Object> get() {
		return _instance.iterator().next();
	}

	@Inject
	@Reference(service = SingletonScoped.class)
	Instance<Map<String, Object>> _instance;

}
