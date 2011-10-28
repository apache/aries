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
package org.apache.aries.samples.goat.api;

import java.util.List;

//This represents a single dependency between two components
public interface RelationshipInfo {
	
	   //relationships are unique by type&name combined.
	
	   String getType(); //String describing the type of this dependency.		   
	   String getName();  //name of this dependency.
	   
	   //the provider/consumer side of this relationship.
	   ComponentInfo getProvidedBy();	   
	   //consumers can of course, be empty. (thats empty.. NOT null)
	   List<ComponentInfo> getConsumedBy();
	   
	   //relationship aspects are not fully integrated yet.. avoid until stable ;-)
	   List<RelationshipAspect> getRelationshipAspects();   
}
