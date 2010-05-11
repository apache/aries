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
dojo.provide("demo.Dependency");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.Moveable");
dojo.require("dojo.data.ItemFileWriteStore");

//Dependency
// represents a line between two bundles.
//
//TODO: 
// - have bundle manage these, not index.html
// - add methods for pulse & glow.
// - add methods for line offset adjust & reset
//   - this will allow lines to move to rows of a twistie, then snap back
// - for services, will need to target the little triangle..
dojo.declare("demo.Dependency", [], {	
	
	//object properties	
	surface: null,
	fromBundle: null,
	toBundle: null,
	name: null,
	type: null,
	visible: null,
	typeOffset: 0,
	
	//gfx objects
	line: null,
	
	//internals
	stroke: null,
	
	//am I deleted?
	removed: false,
	
constructor: function(surface, name, type, fromBundle, toBundle) {
	this.surface=surface;
	this.name=name;
	this.fromBundle=fromBundle;
	this.toBundle=toBundle;
    this.type=type;
	this.stroke = '#000000';
    this.setStroke();
	this.updateVisibility();
	this.updateLine();   
    //this.line.connect("onmouseenter",dojo.hitch(this,"onMouseEnter"));
    //this.line.connect("onmouseleave",dojo.hitch(this,"onMouseLeave"));
},
setStroke: function(){	
	//Fixed to make all lines go over each other
	if(this.type=="packageImport"){
		this.typeOffset=0;
		this.stroke = '#80F080';
	}else if(this.type=="packageExport"){
		this.stroke = '#80F080';
		this.typeOffset=-0;
	}else if(this.type=="serviceExport"){
		this.stroke = '#80F080';
		this.typeOffset=0;
	}else if(this.type=="serviceImport"){
		this.stroke = '#8080F0';
		this.typeOffset=0;
	}
},
updateVisibility: function(){
	this.visible = (!this.fromBundle.hidden) && (!this.toBundle.hidden);
	
	if(!this.visible){
		if(this.line==null){
			this.line = this.surface.createLine({x1: -1000, y1: -1000, x2: -1000, y2: -1000})
		            .setStroke(this.stroke);
		}else{
			this.line.setShape({x1: -1000, y1: -1000, x2: -1000, y2: -1000});
		}
	}else{
		this.updateLine();
	}
},
updateLine: function(){
	if(this.visible){
		var fromx = this.fromBundle.x + (this.fromBundle.width / 2) + this.typeOffset;
		var fromy = this.fromBundle.y + (this.fromBundle.height / 2)+ this.typeOffset;
		var tox = this.toBundle.x + (this.toBundle.width / 2)+ this.typeOffset;
		var toy = this.toBundle.y + (this.toBundle.height / 2)+ this.typeOffset;
		if(this.line==null){
			this.line = this.surface.createLine({x1: fromx, y1: fromy, x2: tox, y2: toy})
		            .setStroke(this.stroke);
		}else{
			this.line.setShape({x1: fromx, y1: fromy, x2: tox, y2: toy});
		}
		this.line.moveToBack();
	}
},
removeSelf: function(){
	console.log("Line from "+this.fromBundle.id+" to "+this.toBundle.id+" being removed");
	if(!this.removed){
		console.log("Line from "+this.fromBundle.id+" to "+this.toBundle.id+" being removed from surface");
		this.surface.remove(this.line);
		console.log("Line from "+this.fromBundle.id+" to "+this.toBundle.id+" being marked as deleted");
		this.removed = true;
	}else{
		console.log("Line from "+this.fromBundle.id+" to "+this.toBundle.id+" already marked as deleted");
	}
}
});
