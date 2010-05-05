/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.demo.prototype;

import java.util.Map;
import java.util.Set;

public class BundleDependencies {
	
	Map<Long, Set<Long>> totalDependencies;
	
	
	public Map<Long, Set<Long>> getTotalDependencies() {
		return totalDependencies;
	}

	public void setTotalDependencies(Map<Long, Set<Long>> totalDependencies) {
		this.totalDependencies = totalDependencies;
	}

	Map<Long, Set<Long>> packageImportDependencies;
	
	public Map<Long, Set<Long>> getPackageImportDependencies() {
		return packageImportDependencies;
	}

	public void setPackageImportDependencies(
			Map<Long, Set<Long>> packageImportDependencies) {
		this.packageImportDependencies = packageImportDependencies;
	}

	public Map<Long, Set<Long>> getPackageExportDependencies() {
		return packageExportDependencies;
	}

	public void setPackageExportDependencies(
			Map<Long, Set<Long>> packageExportDependencies) {
		this.packageExportDependencies = packageExportDependencies;
	}

	Map<Long, Set<Long>> packageExportDependencies;
	
	public BundleDependencies(){		
	}
}
