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

package org.apache.aries.cdi.test.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.aries.cdi.test.interfaces.BundleScoped;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.BeanPropertyType;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.ServiceInstance;

@Service
@ServiceInstance(ServiceScope.BUNDLE)
@ServiceBundleScope.Config
public class ServiceBundleScope implements BundleScoped {

	@BeanPropertyType
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Config {
		String fee_fi() default "fee";
		int fo_fum() default 23;
		String key() default "value";
		String simple_annotation() default "blah";
	}

	@Override
	public Object get() {
		return this;
	}

}
