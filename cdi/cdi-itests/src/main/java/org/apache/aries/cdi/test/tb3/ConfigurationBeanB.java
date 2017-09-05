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

package org.apache.aries.cdi.test.tb3;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.ConfigurationPolicy;
import org.osgi.service.cdi.annotations.ServiceScope;

@Component(
	property = "bean=B",
	serviceScope = ServiceScope.SINGLETON
)
public class ConfigurationBeanB implements BeanService<Callable<int[]>> {

	@Override
	public String doSomething() {
		return (String)config.get("color");
	}

	@Override
	public Callable<int[]> get() {
		return new Callable<int[]>() {
			@Override
			public int[] call() throws Exception {
				return (int[])config.get("ports");
			}
		};
	}

	@Configuration(
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		value = {"org.apache.aries.cdi.test.tb3.ConfigurationBeanA", "$"}
	)
	@Inject
	Map<String, Object> config;

}
