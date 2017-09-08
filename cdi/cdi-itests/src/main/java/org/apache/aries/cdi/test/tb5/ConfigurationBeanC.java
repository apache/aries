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

package org.apache.aries.cdi.test.tb5;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.ConfigurationPolicy;
import org.osgi.service.cdi.annotations.ServiceScope;

@Component(property = "bean=C", scope = ServiceScope.SINGLETON)
public class ConfigurationBeanC implements BeanService<Callable<int[]>> {

	@Override
	public String doSomething() {
		return config.color();
	}

	@Override
	public Callable<int[]> get() {
		return new Callable<int[]>() {
			@Override
			public int[] call() throws Exception {
				return config.ports();
			}
		};
	}

	@Configuration(value = "foo.bar")
	@Inject
	Config config;

}
