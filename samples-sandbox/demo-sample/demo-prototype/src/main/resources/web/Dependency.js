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
dojo.provide("Dependency");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.Moveable");
dojo.require("dojo.data.ItemFileWriteStore");

dojo.declare("Dependency", null, {	
	
	//object properties	
	surface: null,
	fromBundle: null,
	toBundle: null,
	name: null,
	visible: null,
	
	//gfx objects
	line: null,
	
constructor: function(surface, name, fromBundle, toBundle) {
	this.surface=surface;
	this.name=name;
	this.fromBundle=fromBundle;
	this.toBundle=toBundle;

	this.updateVisibility();
	this.updateLine();          
},
updateVisibility: function(){
	this.visible = (!this.fromBundle.hidden) && (!this.toBundle.hidden);
	
	if(!this.visibile){
		if(this.line==null){
			this.line = this.surface.createLine({x1: -1000, y1: -1000, x2: -1000, y2: -1000})
		            .setStroke('#808080');
		}else{
			this.line.setShape({x1: -1000, y1: -1000, x2: -1000, y2: -1000});
		}
	}else{
		updateLine();
	}
},
updateLine: function(){
	if(this.visible){
		var fromx = this.fromBundle.x + (this.fromBundle.width / 2);
		var fromy = this.fromBundle.y + (this.fromBundle.height / 2);
		var tox = this.toBundle.x + (this.toBundle.width / 2);
		var toy = this.toBundle.y + (this.toBundle.height / 2);
		if(this.line==null){
			this.line = this.surface.createLine({x1: fromx, y1: fromy, x2: tox, y2: toy})
		            .setStroke('#808080');
		}else{
			this.line.setShape({x1: fromx, y1: fromy, x2: tox, y2: toy});
		}
	}
	this.line.moveToBack();
}

});
