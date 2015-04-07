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
package org.apache.aries.transaction.parsing;

import java.lang.reflect.Method;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.transaction.Constants;
import org.apache.aries.transaction.TxComponentMetaDataHelper;
import org.apache.aries.transaction.annotations.Transaction;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class AnnotationParser implements BeanProcessor {

	private final ComponentDefinitionRegistry cdr;
	private final Interceptor interceptor;
	private final TxComponentMetaDataHelper helper;
	
	public AnnotationParser(ComponentDefinitionRegistry cdr, Interceptor i, TxComponentMetaDataHelper helper) {
		this.cdr = cdr;
		this.interceptor = i;
		this.helper = helper;
	}

	public void afterDestroy(Object arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	public Object afterInit(Object arg0, String arg1, BeanCreator arg2,
			BeanMetadata arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	public void beforeDestroy(Object arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	public Object beforeInit(Object arg0, String arg1, BeanCreator arg2,
			BeanMetadata arg3) {
        Class<?> c = arg0.getClass();
        boolean interceptorAssigned = false; 
        while(c != Object.class) {
            for(Method m : c.getDeclaredMethods()) {
                int modifiers = m.getModifiers();
                if((modifiers & Constants.BANNED_MODIFIERS) != 0)
                    throw new IllegalArgumentException(Constants.MESSAGES.getMessage("private.or.static.method", m));
			    Transaction t = m.getAnnotation(Transaction.class);
			    if(t != null && helper.getComponentMethodTxAttribute(arg3, m.getName()) == null) {
			    	helper.setComponentTransactionData(cdr, arg3, t.value(), m.getName());
			    	
			    	outer: if(!!!interceptorAssigned) {
			    		for(Interceptor i : cdr.getInterceptors(arg3)) {
			    			if(i == interceptor) {
			    				interceptorAssigned = true;
			    				break outer;
			    			}
			    		}
			            cdr.registerInterceptorWithComponent(arg3, interceptor);
			            interceptorAssigned = true;
			    	}
			    }
			    
		    }
            c = c.getSuperclass();
		}
		return arg0;
	}

}
