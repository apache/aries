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
dojo.provide("demo.Bundle");
dojo.require("demo.TwistieSection");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.fx");
dojo.require("dojox.gfx.Moveable");
dojo.require("dojo.data.ItemFileWriteStore");
dojo.require("demo.Preferences");
dojo.require("demo.BundleAppearance");

dojo.declare("demo.Bundle", [], {

	//properties
	id: null,
	name: null,
	state: null,	
	version: null,
	hidden: false,

	//gfx objects
	group: null,
	surface: null,
	outline: null,
	
	textName: null,
	nameRule: null,
	
	importTwistie: null,
	exportTwistie: null,
	
	textState: null,
	stateRule: null,
	
	textVersion: null,
	versionRule: null,

    // configuration preferences
    preferences: null,
    bundleAppearance: null,
		
	//dimensions/coords
	x: 0,
	y: 0,
	minWidth: 150,
	minHeight: 80,
	ruleOffset: 5,
	ruleHeight: 20,
	width: null,
	height: null,
	selected: false,
	
	linesFrom: null,
	linesTo: null,	
	
	constructor: function(surface, id, name, state, version) {
	this.surface=surface;
	this.id=id;
	this.name=name;
	this.version=version;
	this.state=state;
	this.x=0;
	this.y=0;
	this.preferences=new demo.Preferences();
	this.bundleAppearance=new demo.BundleAppearance(state, this.preferences);

	this.height=115;
	this.width=this.minWidth;
	
	this.linesFrom = new Array();
	this.linesTo = new Array();
	
	this.calcWidthBasedOnContent();
	
	this.initGfx();
	
	var bundleThis = this;
	var importCallBack = function(){
	    var importCallback = function(data)
	    {
	      if (data != null && typeof data == 'object'){
		      bundleThis.importTwistie.items = data;	 
		      bundleThis.importTwistie.addItemsToDisplay();
		      bundleThis.resize();
	      }
	    };				
		ServerSideClass.getPackageImports(bundleThis.id, importCallback);
	};
	var exportCallBack = function(){
	    var exportCallback = function(data)
	    {
	      if (data != null && typeof data == 'object'){
		      bundleThis.exportTwistie.items = data;	 
		      bundleThis.exportTwistie.addItemsToDisplay();
		      bundleThis.resize();
	      }
	    };				
		ServerSideClass.getPackageExports(bundleThis.id, exportCallback);
	};
	
	//name, parentGroup, owningBundle, x, y
	var impy = (this.ruleHeight*2)+this.ruleOffset+25;
	var expy = impy+12;
	this.importTwistie=new demo.TwistieSection("Imports", this.group, this, 5, impy, importCallBack, this.bundleAppearance);
	this.exportTwistie=new demo.TwistieSection("Exports", this.group, this, 5, expy, exportCallBack, this.bundleAppearance);
	
	//update size to account for twisties.
	this.resize();

	var mover = new dojox.gfx.Moveable(this.group);
    dojo.connect(mover, "onMoved", this, "onMoved");  
    
    this.group.connect("onclick",dojo.hitch(this,"onClick"));
    this.group.connect("onmouseenter",dojo.hitch(this,"onMouseEnter"));
    this.group.connect("onmouseleave",dojo.hitch(this,"onMouseLeave"));
    
    dojo.publish("demo.bundle.create",[this]);
},
removeSelf: function() {
	this.surface.remove(this.group);
	dojo.publish("demo.bundle.delete", [this]);
	
	dojo.forEach(this.linesTo, function(line){
		line.removeSelf();
	});
	dojo.forEach(this.linesFrom, function(line){
		line.removeSelf();	
	});
},
initGfx: function() {
	this.group = this.surface.createGroup();

	this.outline = this.group.createRect({x: 0, y: 0, width: this.width, height: this.height, r: 5});

	this.drawState(this.state);

	this.createNameRule(this.width);
	this.createStateRule(this.width);
	this.createVersionRule(this.width);

	this.createNameText(this.name);
	this.createStateText(this.state, this.id);
	this.createVersionText(this.version);
        
},
createNameText: function(name){
if (this.textName != null)
    {
	   this.group.remove(this.textName);
	}

	var y = (this.ruleHeight*0.5)+this.ruleOffset+5;
	this.textName = this.group.createText({x: 5, y: y, text: name, align: "start"})
	.setFont({family: this.bundleAppearance.fontFamily,fontStretch: this.bundleAppearance.fontStretch, size: "12pt"})
	.setFill(this.bundleAppearance.textFill);	
},
createVersionText: function(version){
  if (this.textVersion != null)
  {
  	this.group.remove(this.textVersion);
  }

  if (this.preferences.showVersion) {
  var y = (this.ruleHeight*1.5)+this.ruleOffset+5;
  this.textVersion = this.group.createText({x: 5, y: y, text: version, align: "start"})
    .setFont({family: this.bundleAppearance.fontFamily, fontStretch: this.bundleAppearance.fontStretch, size: "12pt"})
    .setFill(this.bundleAppearance.textFill);
  }

},
drawState: function(state){
    this.outline.setStroke({width: 2, color: this.bundleAppearance.lineColour, style: this.bundleAppearance.lineStyle});
	this.outline.setFill({type: "linear",  x1: 0, y1: 0, x2: 150, y2: 80,
	colors: [{ offset: 0, color: this.bundleAppearance.backgroundColour },{ offset: 1, color: "white" } ]});

},

createStateText: function(state){
 if (this.textState != null)
     {
	    this.group.remove(this.textState);
	 }
	 if (this.preferences.showState)
	 {
	    var stateidstr = "" + this.id + ":" + this.state;
	    var multiplier=2.5;
	 // We need to draw things slightly higher if we didn't have a line for the version
	 if (!this.preferences.showVersion)
	 {
	     multiplier = 1.5;
	 }
	
	var y = (this.ruleHeight*2.5)+this.ruleOffset+5;
	this.textState = this.group.createText({x: 5, y: y, text: state, align: "start"})
	.setFont({family: this.bundleAppearance.fontFamily, fontStretch: this.bundleAppearance.fontStretch, size: "12pt"})
	.setFill(this.bundleAppearance.textFill);
 	} 
},
createNameRule: function(width){
    // Clean up the previous line
	   if (this.versionRule != null)
       {
           this.group.remove(this.nameRule);
       }

	var y = (this.ruleHeight+this.ruleOffset);
	this.nameRule = this.group.createLine({x1:0, y1: y, x2: width, y2: y})
	.setStroke({width:1, color: '#808080'});
},
createVersionRule: function(width){
 // Clean up the previous line
 if (this.versionRule != null)
 {
    this.group.remove(this.versionRule);
 }
 if (this.preferences.showVersion)
 {
	var y = (this.ruleHeight*2)+this.ruleOffset;
	this.versionRule = this.group.createLine({x1:0, y1:y, x2: width, y2: y})
	.setStroke({width:1, color: '#808080'});
 }
},
createStateRule: function(width){
    // Clean up the previous line
	if (this.stateRule != null)
    {
         this.group.remove(this.stateRule);
    }
    // Only add a rule if we're going to be adding a line for the state
    if (this.preferences.showState)
    {
    // We need to draw things slightly higher if we didn't have a line for the version
    var multiplier=3;
    if (!this.preferences.showVersion)
    {
        multiplier = 2;
    }
	var y = (this.ruleHeight*3)+this.ruleOffset;
	this.stateRule = this.group.createLine({x1:0, y1:y, x2: width, y2: y})
	.setStroke({width:1, color: '#808080'});
	}
},
resize: function(){
	var width = this.width;
	if(width < this.importTwistie.width ){
		width = this.importTwistie.width;
	}
	if(width < this.exportTwistie.width ){
		width = this.exportTwistie.width;
	}
	
	var height = this.height;
	if( this.importTwistie.height > 0 || this.exportTwistie.height > 0 ){
		height = height + this.importTwistie.height;
		height = height + this.exportTwistie.height;	
	}
	var multiplier=2;
    if (!this.preferences.showState)
    {
        multiplier=multiplier - 1;
    }
	    if (!this.preferences.showVersion)
    {
        multiplier=multiplier - 1;
    }
    height = height - this.ruleHeight*(2 - multiplier);
	
	var impy = (this.ruleHeight*2)+this.ruleOffset+25;
	var expy = impy+12+this.importTwistie.height;
	//make the export twistie appear in the correct location.
	this.exportTwistie.updatePosition(5,(expy));

	this.createNameRule(width);
	this.createStateRule(width);
	this.createVersionRule(width);
	this.outline.setShape({x: 0, y: 0, width: width, height: height, r: 5});
	this.group.moveToFront();
	
	dojo.forEach(this.linesTo, function(line){ line.updateLine() });
	dojo.forEach(this.linesFrom, function(line){ line.updateLine() });
},
calcWidthBasedOnContent: function(){
	this.width=this.minWidth;
	//adjust the width, based on the name length
	if(this.name.length > 18){
		var extra = this.name.length - 18;
		extra = extra * 7;
		var proposedNewWidth = this.minWidth + extra;
		if(proposedNewWidth > this.width){
			this.width = proposedNewWidth;
		}	
	}
	//adjust the width, based on the version length
	if(this.version.length > 18){
		var extra = this.version.length - 18;
		extra = extra * 8;
		var proposedNewWidth = this.minWidth + extra;	
		if(proposedNewWidth > this.width){
			this.width = proposedNewWidth;
		}	
	}	
},
update: function(id, name, state, version) {	
	if(this.name != name || this.state != state || this.version != version){
		this.name = name;		
		this.state = state;
		this.version = version;
		
		this.bundleAppearance.update(state);
		this.calcWidthBasedOnContent();
	
	this.drawState(state);
	this.createNameText(this.name);

    this.createStateText(this.state, this.id);

    this.createVersionText(this.version);

    this.importTwistie.update();
    this.exportTwistie.update();

	dojo.publish("demo.bundle.update",[this.id,name,state,version]);
	}
},
moveToNewPlace: function(x, y) {
	if(!this.hidden){
	  this.group.setTransform({dx:x, dy:y});
    }
	this.x = x;
	this.y = y;
	this.updateAfterMove();	
},
toggleHidden: function(){
	var hideMe = !this.hidden;	

	if(hideMe){
		//cheat.. move it off canvas..
		this.group.setTransform({dx:-1000, dy:-1000});	
	}else{
		//bring it back =) good job we remembered where it was supposed to go !
		this.group.setTransform({dx:this.x, dy:this.y});	
	}
		
	this.hidden = hideMe;
	dojo.publish("demo.bundle.hidden",[this.id,hideMe]);
	
	//en/disable all lines to/from..
	dojo.forEach(this.linesTo, function(line){  line.updateVisibility(); line.updateLine(); });
	dojo.forEach(this.linesFrom, function(line){ line.updateVisibility(); line.updateLine(); });
},
onMoved: function(mover, shift) {   
    this.x=this.group.matrix.dx;
    this.y=this.group.matrix.dy;
    
	this.updateAfterMove();
},
updateAfterMove: function(){
	//todo, move line updates to lines.. not here.. 
	dojo.forEach(this.linesTo, function(line){ line.updateLine(); });
	dojo.forEach(this.linesFrom, function(line){ line.updateLine(); });
	
	dojo.publish("demo.bundle.move",[this.id,this.x,this.y]);
},
pulse: function() {
	var endColor = this.selected ? "#CEBEEE" : "#808080";
	dojox.gfx.fx.animateStroke({
	    shape: this.outline,
	    duration: 500,
	    color: {start: "#CEBEEE", end: endColor},
	    width: {start:10, end: 2},
	    join:  {values: ["miter", "bevel", "round"]}
	}).play();
	dojo.forEach( this.linesTo, function(line){
		dojox.gfx.fx.animateStroke({
		    shape: line.line,
		    duration: 500,
		    color: {start: "#CEBEEE", end: line.stroke},
		    join:  {values: ["miter", "bevel", "round"]}
		}).play();
	});
	dojo.forEach( this.linesFrom, function(line){
		dojox.gfx.fx.animateStroke({
		    shape: line.line,
		    duration: 500,
		    color: {start: "#CEBEEE", end: line.stroke},
		    join:  {values: ["miter", "bevel", "round"]}
		}).play();
	});
},
glow: function(){
	var endColor = this.selected ? "#BA98E2" : "#808080";
	//this.outline.setStroke({width: 2, color: '#808080'});
	dojox.gfx.fx.animateStroke({
	    shape: this.outline,
	    duration: 500,
	    color: {start: "#BA98E2", end: endColor},
	    width: {start: 3, end: 2},
	    join:  {values: ["miter", "bevel", "round"]}
	}).play();	
},
onClick: function() {
	this.group.moveToFront();
	this.pulse();
	//this.selected=!this.selected;
},
onMouseEnter: function() {
	this.outline.setStroke({width: 3, color: '#682DAE', style: this.bundleAppearance.lineStyle});
	this.selected=true;
},
onMouseLeave: function() {
	this.selected=false;
	this.glow();
}

});
