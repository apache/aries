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
package org.apache.aries.samples.blueprint.idverifier.client;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author forrestxm
 *
 */
public class CreditQueryRegistrationListener {
    private static final Logger log = LoggerFactory.getLogger(CreditQueryRegistrationListener.class);
	
	public void reg(CreditRecordOperationImpl svcobject, Map props){
		//svcobject.doAfterReg();
		log.info("********Registered bean "+svcobject.getClass().getName()+" as a service**********");
		log.info("********Start of Printing service properties**************");
		Set keyset = props.keySet();
		Iterator iter = keyset.iterator();
		while(iter.hasNext()){
			Object keyobj = iter.next();
			Object valueobj = props.get(keyobj);
			log.info(keyobj + "=" + valueobj);
		}
		log.info("********End of Printing service properties**************");
	}
	
	public void unreg(CreditRecordOperationImpl svcobject, Map props){
		log.info("********Unregistering service bean "+svcobject.getClass().getName()+"**********");
	}

}
