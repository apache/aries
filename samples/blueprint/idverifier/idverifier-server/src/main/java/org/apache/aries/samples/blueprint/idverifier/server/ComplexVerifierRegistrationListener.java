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
package org.apache.aries.samples.blueprint.idverifier.server;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author forrestxm
 *
 */
public class ComplexVerifierRegistrationListener {
	
	public void reg(PersonIDVerifierComplexImpl svcobject, Map props){
		//svcobject.doAfterReg();
		System.out.println("********Registered bean "+svcobject.getClass().getName()+" as a service**********");
		System.out.println("********Print service properties**************");
		Set keyset = props.keySet();
		Iterator iter = keyset.iterator();
		while(iter.hasNext()){
			Object keyobj = iter.next();
			Object valueobj = props.get(keyobj);
			System.out.println(keyobj + "=" + valueobj);			
		}
		
	}
	public void unreg(PersonIDVerifierComplexImpl svcobject, Map props){
		System.out.println("********Unregistering service bean "+svcobject.getClass().getName()+"**********");
	}

}
