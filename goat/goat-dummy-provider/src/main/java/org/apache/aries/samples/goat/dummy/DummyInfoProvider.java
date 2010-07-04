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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.samples.goat.api.ComponentInfoImpl;

import org.apache.aries.samples.goat.api.ComponentInfo;
import org.apache.aries.samples.goat.api.ComponentInfoProvider;

public class DummyInfoProvider implements ComponentInfoProvider {
	
	ComponentInfoImpl a = new ComponentInfoImpl();
	ComponentInfoImpl b = new ComponentInfoImpl();
	ComponentInfoImpl c = new ComponentInfoImpl();
		
	public DummyInfoProvider(){
				
		a.setId("/root/"+1);
		Map<String,String> props = new HashMap<String,String>();
		props.put("SymbolicName", "Uber.Bundle");
		props.put("Version", "1.0.0");
		props.put("State", "ACTIVE");
		props.put("BundleID", "1");
		a.setComponentProperties(props);
		
		b.setId("/root/"+2);
		props = new HashMap<String,String>();
		props.put("SymbolicName", "Fred");
		props.put("Version", "1.0.0");
		props.put("State", "RESOLVED");
		props.put("BundleID", "2");
		b.setComponentProperties(props);
		
		c.setId("/root/"+3);
		props = new HashMap<String,String>();
		props.put("SymbolicName", "Wilma");
		props.put("Version", "1.0.0");
		props.put("State", "ACTIVE");
		props.put("BundleID", "3");
		c.setComponentProperties(props);
	}

	@Override
	public List<ComponentInfo> getComponents() {
		List<ComponentInfo> result = new ArrayList<ComponentInfo>();
		result.add(a);
		result.add(b);
		result.add(c);
		return result;
	}

	@Override
	public ComponentInfo getComponentForId(String id) {
		if("/root/1".equals(id)) return a;
		if("/root/2".equals(id)) return b;
		if("/root/3".equals(id)) return c;
		return null;
	}

	@Override
	public void registerComponentInfoListener(ComponentInfoListener listener) {
		//no-op
	}

}

