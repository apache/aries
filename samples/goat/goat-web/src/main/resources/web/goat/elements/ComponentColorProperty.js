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
dojo.provide("goat.elements.ComponentColorProperty");

dojo.declare("goat.elements.ComponentColorProperty", [goat.elements.ElementBase], {

	value: null,

constructor : function(component, /*String*/ type, /*String*/ value) {
	this.component=component;
	this.value=value;
	this.type=type;
	this.hint="none";
},
getWidth: function() {
	return 0;
},
getHeight: function() {
	return 0;
},
render: function(){
	if(this.type=="component.property.State"){
		if(this.value!="ACTIVE"){
			//nasty reading directly into the comp like this, but it will do for now.
			this.component.outline.setFill({type: "linear",  x1: 0, y1: 0, x2: 150, y2: 80,
				colors: [{ offset: 0, color: "#808080" },{ offset: 1, color: "#ffffff" } ]});
		}
	}	
},
update: function(value){
	this.value=value;
},
remove: function(){
	//no op, we only exist due to the color of the owning component..
}

});
