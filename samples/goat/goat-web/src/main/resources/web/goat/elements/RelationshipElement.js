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
dojo.provide("goat.elements.RelationshipElement");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.Moveable");
dojo.require("goat.Component");

//Relationship
// represents a line between two components.
//
//TODO: 
// - add methods for pulse & glow.
// - add methods for line offset adjust & reset
//   - this will allow lines to move to rows of a twistie, then snap back
dojo.declare("goat.elements.RelationshipElement", [], {	
	
	//relationship properties.
	fromComponent: null,
	toComponent: null,
	name: null,	
	type: null,
	
	//object properties	
	surface: null,
	visible: null,
	typeOffset: 0,
	
	//gfx objects
	line: null,
	
	// helper elements
	decorators: null,
	
	//internals
	stroke: null,
	
	//am I deleted?
	removed: false,
	
	//for the up and coming relationship aspect info.. 
	aspects: null,
	
	subs: null,
	
constructor: function(surface, name, type, fromComponent, toComponent, aspects) {
	this.surface=surface;
	
	//console.log("Building relationship elt for "+name+" "+type+" from "+fromComponent.id+" to "+toComponent.id+" ");
	
	this.name=name;
    this.type=type;
	this.fromComponent=fromComponent;
	this.toComponent=toComponent;
	this.aspects=aspects;
	
	this.stroke = '#000000';
    this.setStroke();
	this.updateVisibility();
	this.updateLine(); 
		
	this.subs=new Array();
	this.subs.push(dojo.subscribe("goat.component.move."+fromComponent.id, this, this.onComponentMove));
	this.subs.push(dojo.subscribe("goat.component.move."+toComponent.id, this, this.onComponentMove));
	this.subs.push(dojo.subscribe("goat.component.hidden."+fromComponent.id, this, this.onComponentHidden));
	this.subs.push(dojo.subscribe("goat.component.hidden."+toComponent.id, this, this.onComponentHidden));
	this.subs.push(dojo.subscribe("goat.component.onclick."+toComponent.id, this, this.onComponentClick));
	this.subs.push(dojo.subscribe("goat.component.onclick."+fromComponent.id, this, this.onComponentClick));
	
	this.decorators = new Array();
	
	//console.log("Publishing relationship create event");
	dojo.publish("goat.relationship.create."+fromComponent.id,[this]);
},
addDecorator: function(decorator) {
	decorator.setStroke(this.stroke);
	this.decorators.push(decorator);
},
setStroke: function(){	
	
	//right idea.. wrong approach.. this needs the registry to provide the renderer for this relationship type.
	
	if(this.type=="packageImport"){
		this.typeOffset=5;
		this.stroke = '#F08080';
	}else if(this.type=="packageExport"){
		this.stroke = '#80F080';
		this.typeOffset=-5;
	}else if(this.type=="serviceExport"){
		this.stroke = '#F080F0';
		this.typeOffset=10;
	}else if(this.type=="serviceImport"){
		this.stroke = '#8080F0';
		this.typeOffset=-10;
	}
},
updateVisibility: function(){
	//if(this.removed){
		//console.log("uv EEK.. this line should be dead.. and its aliiiiiive "+this.type+" from "+this.fromComponent.id+" to "+this.toComponent.id);
		//console.log(this);
	//}
	
	this.visible = (!this.fromComponent.hidden) && (!this.toComponent.hidden);
	
	if(!this.visible){
		if(this.line==null){
			// No need to erase a line which doesn't exist ...
		}else{
			this.line.setShape({x1: -1000, y1: -1000, x2: -1000, y2: -1000});

            //console.log("Hiding decorators..");
			dojo.forEach(this.decorators, function(decorator){
                //console.log("Hiding decorator..");
				decorator.makeInvisible();
			},this);

		}
	}else{
		this.updateLine();
	}
},
updateLine: function(){
	if(this.removed){
		console.log("ul EEK.. this line should be dead.. and its aliiiiiive "+this.type+" from "+this.fromComponent.id+" to "+this.toComponent.id);
		console.log(this);
	}
	
	if(this.visible){
        //console.log("Updating VISIBLE line from "+this.fromComponent.id+" to "+this.toComponent.id);
		var fromx = this.fromComponent.x + (this.fromComponent.width / 2) + this.typeOffset;
		var fromy = this.fromComponent.y + (this.fromComponent.height / 2)+ this.typeOffset;
		var tox = this.toComponent.x + (this.toComponent.width / 2)+ this.typeOffset;
		var toy = this.toComponent.y + (this.toComponent.height / 2)+ this.typeOffset;
			
		if(this.line==null){
			this.line = this.surface.createLine({x1: fromx, y1: fromy, x2: tox, y2: toy})
		            .setStroke(this.stroke);
		}else{
			this.line.setShape({x1: fromx, y1: fromy, x2: tox, y2: toy});
		}

        if (this.decorators != null) {
            dojo.forEach(this.decorators, function(decorator){
                decorator.lineUpdated(this.line);
            },this);
        }

        // Our line should be underneath any decorations
        this.line.moveToBack();
	}
	
},
removeSelf: function(){
	//console.log("Line from "+this.fromComponent.id+" to "+this.toComponent.id+" being removed");
    //console.log(this);
	if(!this.removed){
		this.removed = true;
		
		//console.log("Line from "+this.fromComponent.id+" to "+this.toComponent.id+" being removed from surface");
        //console.log(this);
        if(this.line!=null) {
            this.surface.remove(this.line);
        }

        //console.log("Removing decorators..");
		dojo.forEach(this.decorators, function(decorator){
            //console.log("Asking...");
            //console.log(decorator);
            //console.log("..to remove itself");
			decorator.removeSelf();
		});
        this.decorators = new Array();
		//console.log("Line from "+this.fromComponent.id+" to "+this.toComponent.id+" being marked as deleted");
		
		//console.log("Removing line subscriptions to components.");
		dojo.forEach(this.subs, function(sub){
			//console.log("unsubscribing.. ");
			//console.log(sub);
			dojo.unsubscribe(sub);
		});
		
		this.subs = new Array();
		
		dojo.publish("goat.relationship.remove."+this.fromComponent.id,[this]);
	}else{
		console.log("Line from "+this.fromComponent.id+" to "+this.toComponent.id+" already marked as deleted");
	}
},
getKey: function(){
	var key = ""+this.fromComponent.id+"!"+this.toComponent.id+"!"+this.type+"!"+this.name;
},
onComponentMove: function(component){
	this.updateLine();
},
onComponentHidden: function(component){
	this.updateVisibility();
},
onComponentClick: function(component){
    //console.log("OnClick "+component.id);
	if(this.removed){
		console.log("occ EEK.. this line should be dead.. and its aliiiiiive "+this.type+" from "+this.fromComponent.id+" to "+this.toComponent.id);
	}

    if(this.line!=null) {
        dojox.gfx.fx.animateStroke({
            shape: this.line,
            duration: 500,
            color: {start: "#FF3030", end: this.stroke},
            width: {start: 3, end: 2},
            join:  {values: ["miter", "bevel", "round"]}
        }).play();	
    }
}
});
