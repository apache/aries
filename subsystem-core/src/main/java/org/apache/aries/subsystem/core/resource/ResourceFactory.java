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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.osgi.framework.resource.Resource;
import org.osgi.service.subsystem.SubsystemException;

public class ResourceFactory {
	public Resource newResource(URL url) throws IOException, URISyntaxException {
		if (url.getPath().endsWith(".jar"))
			return BundleResource.newInstance(url);
		if (url.getPath().endsWith(".ssa"))
			return SubsystemResource.newInstance(new File(url.toURI()));
		throw new SubsystemException("Unsupported resource type: " + url);
	}
}
