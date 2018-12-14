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

import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

public class JndiExtensionFactory implements PrototypeServiceFactory<Object> {

	public JndiExtensionFactory(LoggerFactory loggerFactory) {
		_loggerFactory = loggerFactory;
	}

	@Override
	public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
		return new JndiExtension(_loggerFactory.getLogger(bundle, JndiContext.class.getName(), Logger.class));
	}

	@Override
	public void ungetService(
		Bundle bundle, ServiceRegistration<Object> registration, Object extension) {
	}

	private final LoggerFactory _loggerFactory;

}
