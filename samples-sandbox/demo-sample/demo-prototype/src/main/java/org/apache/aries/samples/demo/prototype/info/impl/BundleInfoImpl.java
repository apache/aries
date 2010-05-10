/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.demo.prototype.info.impl;

import java.util.List;

import org.apache.aries.samples.demo.api.BundleInfo;

public class BundleInfoImpl implements BundleInfo {

	   public static class PackageInfoImpl implements BundleInfo.PackageInfo{
		   public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public List<String[]> getParameters() {
			return parameters;
		}
		public void setParameters(List<String[]> parameters) {
			this.parameters = parameters;
		}
		public long getSuppliedBy() {
			return suppliedBy;
		}
		public void setSuppliedBy(long suppliedBy) {
			this.suppliedBy = suppliedBy;
		}
		public long[] getSupplyCandidates() {
			return supplyCandidates;
		}
		public void setSupplyCandidates(long[] supplyCandidates) {
			this.supplyCandidates = supplyCandidates;
		}
		String name;
		   String version;
		   List<String[]> parameters;
		   long suppliedBy;
		   long[] supplyCandidates;
		   long[] usedBy;
		   

		public long[] getUsedBy() {
			return this.usedBy;
		}
		public void setUsedBy(long[] usedBy){
			this.usedBy = usedBy;		   
		}
	}
	
	
	long bundleId;
	String version;
	String symbolicName;
	int state;
	long[] dependencies;
	PackageInfo[] importedPackages;
	PackageInfo[] exportedPackages;
	String[] registeredServices;
	String[] servicesInUse;
	public long getBundleId() {
		return bundleId;
	}
	public void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getSymbolicName() {
		return symbolicName;
	}
	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	public long[] getDependencies() {
		return dependencies;
	}
	public void setDependencies(long[] dependencies) {
		this.dependencies = dependencies;
	}
	public PackageInfo[] getImportedPackages() {
		return importedPackages;
	}
	public void setImportedPackages(PackageInfo[] importedPackages) {
		this.importedPackages = importedPackages;
	}
	public PackageInfo[] getExportedPackages() {
		return exportedPackages;
	}
	public void setExportedPackages(PackageInfo[] exportedPackages) {
		this.exportedPackages = exportedPackages;
	}
	public String[] getRegisteredServices() {
		return registeredServices;
	}
	public void setRegisteredServices(String[] registeredServices) {
		this.registeredServices = registeredServices;
	}
	public String[] getServicesInUse() {
		return servicesInUse;
	}
	public void setServicesInUse(String[] servicesInUse) {
		this.servicesInUse = servicesInUse;
	}

}
