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
dojo.provide("goat.elements.ElementBase");

dojo.declare("goat.elements.ElementBase", [], {
	
	    type: null,
	    component: null,
	    x: 0,
	    y: 0,
		width: 0,
		height: 0,
		hint: "top",

		constructor : function(component) {
			//not really called (naughty sub-classes dont chain their constructors..)
	        
	        //DO NOT put things onscreen during the constructor, delay that until 'render' is invoked if possible.
		},
		update: function(/*Object*/ value){
			//called to tell this element, that its value should be updated to the passed arg.
		},
		getWidth: function() {
			//called to obtain the width of this element for layout purposes
			return -1;
		},
		getHeight: function() {
			//called to obtain the height of this element for layout purposes
			return -1;
		},
		render: function() {
			//called to add this element to the screen at this.x, this.y
		},
		remove: function() {
			//called to remove this element from the screen
		}
		
			
	});
