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
package org.apache.aries.samples.goat.dummy;

import java.util.ArrayList;
import java.util.List;

import org.apache.aries.samples.goat.api.RelationshipInfoImpl;

import org.apache.aries.samples.goat.api.ComponentInfo;
import org.apache.aries.samples.goat.api.ComponentInfoProvider;
import org.apache.aries.samples.goat.api.RelationshipInfo;
import org.apache.aries.samples.goat.api.RelationshipInfoProvider;

public class DummyRelationshipProvider implements RelationshipInfoProvider {

	ComponentInfoProvider cip = null;
	
	public DummyRelationshipProvider(ComponentInfoProvider cip){
		this.cip = cip;
	}
	
	@Override
	public List<RelationshipInfo> getRelationships() {
		
		ArrayList<RelationshipInfo> ris = new ArrayList<RelationshipInfo>();
		
		ComponentInfo ci1 = cip.getComponentForId("/root/1");
		ComponentInfo ci2 = cip.getComponentForId("/root/2");
		ComponentInfo ci3 = cip.getComponentForId("/root/3");
		
		RelationshipInfoImpl ri1 = new RelationshipInfoImpl();
		RelationshipInfoImpl ri2 = new RelationshipInfoImpl();
		RelationshipInfoImpl ri3 = new RelationshipInfoImpl();
		RelationshipInfoImpl ri4 = new RelationshipInfoImpl();
		RelationshipInfoImpl ri5 = new RelationshipInfoImpl();
		RelationshipInfoImpl ri6 = new RelationshipInfoImpl();
		ris.add(ri1);
		ris.add(ri2);
		ris.add(ri3);
		ris.add(ri4);
		ris.add(ri5);
		ris.add(ri6);
		
		ri1.setName("i.am.exported.by.1.and.used.by.2.and.3");
		ri1.setProvidedBy(ci1);
		ArrayList<ComponentInfo> c = new ArrayList<ComponentInfo>();
		c.add(ci2);
		c.add(ci3);
		ri1.setConsumedBy(c);
		ri1.setType("Package"); 
		
		ri2.setName("i.am.exported.by.1.and.used.by.3");
		ri2.setProvidedBy(ci1);
		c = new ArrayList<ComponentInfo>();
		c.add(ci3);
		ri2.setConsumedBy(c);
		ri2.setType("Package"); 
		
		ri3.setName("i.am.exported.by.2.and.used.by.3");
		ri3.setProvidedBy(ci2);
		c = new ArrayList<ComponentInfo>();
		c.add(ci3);
		ri3.setConsumedBy(c);
		ri3.setType("Package"); 
		
		ri4.setName("i.am.exported.by.3.and.used.by.2");
		ri4.setProvidedBy(ci3);
		c = new ArrayList<ComponentInfo>();
		c.add(ci2);
		ri4.setConsumedBy(c);
		ri4.setType("Package"); 

		ri5.setName("i.am.a.funky.service.from.3.used.by.2");
		ri5.setProvidedBy(ci3);
		c = new ArrayList<ComponentInfo>();
		c.add(ci2);
		ri5.setConsumedBy(c);
		ri5.setType("Service");
		
		ri6.setName("i.am.a.funky.service.from.1.used.by.2");
		ri6.setProvidedBy(ci1);
		c = new ArrayList<ComponentInfo>();
		c.add(ci2);
		ri6.setConsumedBy(c);
		ri6.setType("Service");
		return ris;
	}

	@Override
	public void registerRelationshipInfoListener(RelationshipInfoListener listener) {
		// TODO Auto-generated method stub

	}

}
