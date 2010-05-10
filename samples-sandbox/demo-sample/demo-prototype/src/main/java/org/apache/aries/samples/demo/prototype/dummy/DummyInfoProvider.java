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
package org.apache.aries.samples.demo.prototype.dummy;

import org.apache.aries.samples.demo.api.BundleInfo;
import org.apache.aries.samples.demo.api.BundleInfoProvider;
import org.apache.aries.samples.demo.prototype.info.impl.BundleInfoImpl;

public class DummyInfoProvider implements BundleInfoProvider {
	
	BundleInfoImpl a = new BundleInfoImpl();
	BundleInfoImpl b = new BundleInfoImpl();
	BundleInfoImpl c = new BundleInfoImpl();
	
	BundleInfoImpl.PackageInfoImpl p1 = new BundleInfoImpl.PackageInfoImpl();
	BundleInfoImpl.PackageInfoImpl p2 = new BundleInfoImpl.PackageInfoImpl();
	BundleInfoImpl.PackageInfoImpl p3 = new BundleInfoImpl.PackageInfoImpl();
	
	public DummyInfoProvider(){
		
		p1.setName("package.one");
		p1.setSuppliedBy(1);
		p1.setVersion("1.0.0");
		p1.setUsedBy(new long[]{1,2});
		
		p2.setName("package.two");
		p2.setSuppliedBy(1);
		p2.setVersion("2.0.0");
		p2.setUsedBy(new long[]{1,2});
		
		p3.setName("package.three");
		p3.setSuppliedBy(2);
		p3.setVersion("3.0.0");
		p3.setUsedBy(new long[]{3});
		
		a.setBundleId(1);
		a.setSymbolicName("Uber.Bundle");
		a.setImportedPackages(new BundleInfo.PackageInfo[]{p1});
		a.setExportedPackages(new BundleInfo.PackageInfo[]{p1,p2});
		a.setDependencies(new long[]{1,2});
		a.setRegisteredServices(new String[]{});
		a.setServicesInUse(new String[]{});
		a.setVersion("1.0.0");
		a.setState(1);
		
		b.setBundleId(2);
		b.setSymbolicName("Fred");
		b.setImportedPackages(new BundleInfo.PackageInfo[]{p1,p2});
		b.setExportedPackages(new BundleInfo.PackageInfo[]{p3});
		b.setDependencies(new long[]{1,2,3});
		b.setRegisteredServices(new String[]{});
		b.setServicesInUse(new String[]{});
		b.setVersion("1.0.0");
		b.setState(1);
		
		c.setBundleId(3);
		c.setSymbolicName("Wilma");
		c.setImportedPackages(new BundleInfo.PackageInfo[]{p3});
		c.setExportedPackages(new BundleInfo.PackageInfo[]{});
		c.setDependencies(new long[]{2,3});
		c.setRegisteredServices(new String[]{});
		c.setServicesInUse(new String[]{});
		c.setVersion("1.0.0");
		c.setState(1);
	}

	@Override
	public BundleInfo[] getBundles() {
		// TODO Auto-generated method stub
		return new BundleInfo[]{a,b,c};
	}

	@Override
	public BundleInfo getBundleForId(long id) {
		if(id==1) return a;
		if(id==2) return b;
		if(id==3) return c;
		return null;
	}

	@Override
	public void registerBundleInfoListener(BundleInfoListener listener) {
		//no-op
	}

}
