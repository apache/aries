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
// dojo.provide allows pages use all types declared in this resource
dojo.provide("goat.elements.ComponentContainer");

dojo.declare("goat.elements.ComponentContainer", goat.elements.ElementBase, {

	    /*goat.Component[]*/children:null,
	
		constructor : function(component, type, /*goat.Component[]*/children) {
		    this.component=component;
			this.type=type;
			this.children=children;
		},
		getWidth : function() {
			return 250;
		},
		getHeight : function() {
			return 250;
		},
		update: function(children){
			//TODO: better children merging.. handle add/remove/modify
			this.children=children;
		},
		remove: function(){
			
		}
			
	});
