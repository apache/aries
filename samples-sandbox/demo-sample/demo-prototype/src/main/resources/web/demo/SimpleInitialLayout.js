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
dojo.provide("demo.SimpleInitialLayout");

dojo.declare("demo.SimpleInitialLayout", [], {
	lastCreatedX:0,
	lastCreatedY:0,
	lastCreatedWidth:0,
	lastCreatedHeight:0,
	surfaceX:0,
	surfaceY:0,
	constructor: function(x,y){
		this.surfaceX=x;
		this.surfaceY=y;
	},
	placeBundle: function(bundle){
		//Move the new bundle so it appears in a nice way
		var end = this.lastCreatedX + this.lastCreatedWidth + 5;
		if (end + bundle.width < this.surfaceX) {
			bundle.moveToNewPlace(end, this.lastCreatedY);
		} else {
			if ((this.lastCreatedY + this.lastCreatedHeight + 5) < this.surfaceY) {
				bundle.moveToNewPlace(5, (this.lastCreatedY
						+ this.lastCreatedHeight + 5));		
			}			
		}	
		this.lastCreatedX = bundle.x;
		this.lastCreatedWidth = bundle.width;
		this.lastCreatedY = bundle.y;
		this.lastCreatedHeight = bundle.height;		
	},
	reset: function(){
		this.lastCreatedX=0;
		this.lastCreatedY=0;
		this.lastCreatedWidth=0;
		this.lastCreatedHeight=0;		
	}
			
});
