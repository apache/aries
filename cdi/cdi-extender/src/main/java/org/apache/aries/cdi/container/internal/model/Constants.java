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

package org.apache.aries.cdi.container.internal.model;

import java.util.Set;

import org.apache.aries.cdi.container.internal.util.Sets;

public class Constants {

	private Constants() {
		// no instances
	}

	public static final String CDI10_URI = "http://www.osgi.org/xmlns/cdi/v1.0.0";
	public static final Set<String> CDI_URIS = Sets.immutableHashSet(CDI10_URI);

	public static final String BEAN_ELEMENT = "bean";
	public static final String CLASS_ATTRIBUTE = "class";
	public static final String NAME_ATTRIBUTE = "name";
	public static final String QUALIFIER_BLACKLIST_ELEMENT = "qualifier-blacklist";
	public static final String QUALIFIER_ELEMENT = "qualifier";

}
