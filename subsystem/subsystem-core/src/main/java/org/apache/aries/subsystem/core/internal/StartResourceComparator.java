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
package org.apache.aries.subsystem.core.internal;

import java.util.Comparator;

import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.osgi.resource.Resource;

public class StartResourceComparator implements Comparator<Resource> {
	private final SubsystemContentHeader header;
	
	public StartResourceComparator(SubsystemContentHeader header) {
		this.header = header;
	}
	@Override
	public int compare(Resource r1, Resource r2) {
		Integer r1StartOrder = getStartOrder(r1);
		Integer r2StartOrder = getStartOrder(r2);
		return r1StartOrder.compareTo(r2StartOrder);
	}
	
	private Integer getStartOrder(Resource r) {
		SubsystemContentHeader.Clause clause = header.getClause(r);
		if (clause == null)
			return -1;
		return clause.getStartOrder();
	}
}
