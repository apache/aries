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
dojo.provide("goat.elements.TextComponentProperty");

dojo.declare("goat.elements.TextComponentProperty", goat.elements.ElementBase, {

		value: null,
		text: null,
	
		constructor : function(component, /*String*/ type, /*String*/ value) {
			//console.log("Adding "+type+" "+value);
			//console.log(component);
	
			this.component=component;
			this.type=type;
			this.value=value;
			this.hint="row";
		},
		getWidth : function(){
			var calcWidth=150;
			if(this.value.length > 18){
				var extra = this.value.length - 18;
				extra = extra * 8; //rough estimate of 'per char extra needed over 150px once past 18chars' ;-)
				return calcWidth + extra;	
			}			
			return calcWidth;
		},
		getHeight : function(){
			return 14;
		},
		render : function(){
			if(this.text!=null){
				this.component.group.remove(this.text);
			}
			//console.log("Creating text node");
			//console.log(this);
			
			//text renders with 0,0 being the BOTTOM LEFT of the text object, we want to beleive all our objects have it as TOP LEFT
			//so we add 12 to the ypos, 12 being the point height of our font.
			this.text = this.component.group.createText({x: this.x, y: this.y+12, text: this.value, align: "start"})
			.setFont({family: "times", size: "12pt"})
			.setFill("#000000");		
		},
		update: function(value){
			this.value=value;
		},
		removeSelf: function(){
			if(this.text!=null){
				this.component.group.remove(this.text);
			}			
		}
			
	});
