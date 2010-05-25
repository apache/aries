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
//dojo.provide allows pages use all types declared in this resource

var /*goat.elements.ElementRegistry */ registry=null;

dojo.provide("goat.elements.ElementRegistry");

dojo.require("goat.elements.TextComponentProperty");
dojo.require("goat.elements.ComponentContainer");
dojo.require("goat.elements.RelationshipAggregation");
dojo.require("goat.elements.ComponentColorProperty");

dojo.declare("goat.elements.ElementRegistry", [], {
	
constructor : function() {
	if(registry==null){
		registry = this;
	}
},

//getRegistry returns the appropriate registry for the component passed. 
/*goat.elements.ComponentPropertyRegistry */ getRegistry : function(/*goat.Component*/ component, props) {
	
	//we currently just have the one.
	//we might start to tailor this, perhaps if we want totally different rendering rules per component, or component type.
	
	return registry;
},

/*goat.elements.ElementBase*/ getProperty : function(component, /*String*/type, /*Object*/value){
	
	//this sort of resolution needs to be handled by Config.
	if(type=="component.property.State"){
		return new goat.elements.ComponentColorProperty(component,type,value);
	}else if(type.match("^component.property.")=="component.property."){
		return new goat.elements.TextComponentProperty(component,type,value);
	}else if(type.match("^component.container")=="component.container"){
		return new goat.elements.ComponentContainer(component, type, value);
	}else if(type.match("^relationship.aggregation.")=="relationship.aggregation."){
		return new goat.elements.RelationshipAggregation(component, type, value);
	}
	//else.. unknown property.. ignore it.
}


});
