/*
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
package org.apache.aries.subsystem.core.resource;

import org.apache.aries.subsystem.core.internal.AriesSubsystem;
import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.osgi.framework.wiring.Resource;
import org.osgi.framework.wiring.ResourceConstants;
import org.osgi.service.subsystem.SubsystemException;

public class RuntimeResourceFactoryImpl implements RuntimeResourceFactory {
	@Override
	public RuntimeResource create(Resource resource, ResourceListener listener, AriesSubsystem subsystem) throws SubsystemException {
		if (resource instanceof RuntimeResource)
			return (RuntimeResource)resource;
		String type = ResourceHelper.getTypeAttribute(resource);
		if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			return new BundleRuntimeResource((BundleResource)resource, listener, subsystem);
		// TODO Add to constants.
		if ("osgi.subsystem".equals(type))
			return new SubsystemRuntimeResource((SubsystemResource)resource, listener, subsystem);
		throw new SubsystemException("Unsupported resource type: " + type);
	}
}
