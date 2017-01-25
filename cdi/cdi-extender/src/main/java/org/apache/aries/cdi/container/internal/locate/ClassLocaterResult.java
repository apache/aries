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

package org.apache.aries.cdi.container.internal.locate;

import java.net.URL;
import java.util.Collection;

public class ClassLocaterResult {

	public ClassLocaterResult(Collection<String> beanClasses, Collection<URL> beanDescriptorURLs) {
		_beanClasses = beanClasses;
		_beanDescriptorURLs = beanDescriptorURLs;
	}

	public Collection<String> getBeanClassNames() {
		return _beanClasses;
	}

	public Collection<URL> getBeanDescriptorURLs() {
		return _beanDescriptorURLs;
	}

	private Collection<String> _beanClasses;
	private Collection<URL> _beanDescriptorURLs;

}