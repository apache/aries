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

package org.apache.aries.cdi.extension.jndi;

import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;

import java.util.Hashtable;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.log.Logger;
import org.osgi.util.promise.Deferred;

public class JndiExtension implements Extension, ObjectFactory {

	public JndiExtension(Logger log) {
		_beanManager = new Deferred<>();
		_jndiContext = new JndiContext(log, _beanManager.getPromise());
	}

	@Override
	public Object getObjectInstance(
			Object obj, Name name, javax.naming.Context context, Hashtable<?, ?> environment)
		throws Exception {

		if (obj == null) {
			return _jndiContext;
		}

		return null;
	}

	void applicationScopedInitialized(
		@Observes @Priority(LIBRARY_AFTER + 800)
		AfterDeploymentValidation adv, BeanManager beanManager) {

		_beanManager.resolve(beanManager);
	}

	private final Deferred<BeanManager> _beanManager;
	private final JndiContext _jndiContext;

}
