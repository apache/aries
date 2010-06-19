/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
dojo.provide("goat.Component");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.fx");
dojo.require("dojox.gfx.Moveable");

dojo.require("goat.elements.ElementBase");
dojo.require("goat.elements.ElementRegistry");
dojo.require("goat.elements.ComponentContainer");
dojo.require("goat.elements.TextComponentProperty");
dojo.require("goat.RelationshipManager");
dojo.require("goat.ElementLayoutManager");

dojo.require("goat.configuration.Theme");
dojo.require("goat.configuration.ComponentAppearance");

// Component,
// looks after a component shape on the surface.
//
dojo.declare("goat.Component", [], {

	// fixed properties.

	/* String */id : null, // must be unique within enclosing component, if
							// any, or globally if none.
	/* goat.Component[] */children : null,
	/* goat.elements.ElementBase[] */elements : null, // gui display elements
													// within this component,
													// all must extend
													// 'goat.elements.ElementBase'

	// manager objects..
	/* goat.RelationshipManager */relationshipManager : null, // manager for
															// handling all
															// relationships
															// to/from this
															// component.
	/* goat.ElementLayoutManager */elementLayoutManager : null, // manager for
																// arranging
																// elements
																// within a
																// component.
	/* goat.elements.ElementRegistry */elementRegistry : null, // manager for
																// returning
																// appropriate
																// renderers for
																// component
																// elements

	// state.
	/* boolean */hidden : false, // is component visible on the surface.
	/* boolean */selected : false, // is mouse currently within our boundary ?
	/* boolean */refreshing : false, // is a refresh in progress ?

	// gfx objects
	/* dojox.gfx.Group */group : null,
	/* dojox.gfx.Surface */surface : null,
	/* dojox.gfx.Rect */outline : null,

	/* goat.configuration.ComponentAppearance */componentAppearance : null,
	// dimensions/coords - onsurface positioning.
	/* int */x : 0,
	/* int */y : 0,
	/* int */width : null,
	/* int */height : null,

	/* int */minWidth : 150,
	/* int */minHeight : 80,

	constructor : function(/* dojox.gfx.Surface */surface,
	/* String */id,
	/* String[] */props,
	/* goat.Component[] */children, /* goat.configuration */theme) {

		this.surface = surface;
		this.x = 0;
		this.y = 0;
		this.height = 115;
		this.width = this.minWidth;

	// TODO if we have a parent we should pass through their component
	// appearance */
	this.componentAppearance = new goat.configuration.ComponentAppearance(
			theme, null);
	// console.log("Adding Box");
	this.initGfx();

	this.id = id;
	this.elements = new Array();

	this.children = children;
	if (this.children != null) {
		var type = "component.container";
		var element = this.elementRegistry.getProperty(this.group,
				this.componentAppearance, type, children);
		this.elements[type] = element;
	}

	this.elementRegistry = new goat.elements.ElementRegistry(this, props);
	this.elementRegistry = this.elementRegistry.getRegistry();

	this.updateProperties(props);

	// console.log("Creating RM");
	this.relationshipManager = new goat.RelationshipManager(this);
	// console.log("Creating ELM");
	this.elementLayoutManager = new goat.ElementLayoutManager(this);

	// console.log("Invoking refresh");
	this.refresh();

	var mover = new dojox.gfx.Moveable(this.group);
	dojo.connect(mover, "onMoved", this, "onMoved");

	this.group.connect("onclick", dojo.hitch(this, "onClick"));
	this.group.connect("onmouseenter", dojo.hitch(this, "onMouseEnter"));
	this.group.connect("onmouseleave", dojo.hitch(this, "onMouseLeave"));

	dojo.publish("goat.component.create", [ this ]);

	dojo.subscribe("goat.component.refresh", this, this.onRefresh);
},
updateProperties : function(props) {
	// console.log("Processing properties");
	// console.log(props);

	this.componentProperties = new Array();
	for ( var key in props) {
		var value = props[key];
		// console.log("The fieldname or key is: "+key+" and the value at that
		// field is: "+value);
		var type = "component.property." + key;
		if (this.elements[type] == null) {
			var element = this.elementRegistry.getProperty(this,
					this.componentAppearance, type, value);
			this.elements[type] = element;
		} else {
			this.elements[type].update(value);
		}
	}
	// TODO: what about elements in this.elements that were not present in
	// props,
	// should they be removed from the this.elements array.. currently elements
	// dont
	// support a 'remove' method.
	// for now, as components dont need removing, wont worry about this.

	// console.log("Properties processed.");
},
removeSelf : function() {

	//Remove this component and all the relationship elements attached to it.
	this.surface.remove(this.group);
	//Now we remove all other the elements
	for (var type in this.elements) {
		this.elements[type].remove();
		delete this.elements[type];
	}
	this.relationshipManager.removeSelf();
	delete this.RelationshipManager;

	//This one is subscribed to by Relationship
	dojo.publish("goat.component.delete." + this.id, [ this ]);

	//This one is subscribed to by ComponentStatusGrid
	dojo.publish("goat.component.delete", [ this ]);
},
initGfx : function() {
	this.group = this.surface.createGroup();

	this.outline = this.group.createRect( {
		x : 0,
		y : 0,
		width : this.width,
		height : this.height,
		r : 5
	}).setStroke( {
		width : 2,
		color : this.componentAppearance.getOutlineColor0()
	})

	if (this.componentAppearance.useLinearShading()) {
		this.outline.setFill( {
			type : "linear",
			x1 : 0,
			y1 : 0,
			x2 : 150,
			y2 : 80,
			colors : [ {
				offset : 0,
				color : this.componentAppearance.getBackgroundColor()
			}, {
				offset : 1,
				color : this.componentAppearance.getBackgroundContrastColor()
			} ]
		});
	} else {
		this.outline.setFill(this.componentAppearance.getBackgroundColor());

	}

},
refresh : function() {
	// console.log(">Component.refresh");
	this.elementLayoutManager.doLayout();

	// console.log("Sizing box");
	// not compatible with component shape property!
	this.outline.setShape( {
		x : 0,
		y : 0,
		width : this.width,
		height : this.height,
		r : 5
	});

	if (this.componentAppearance.useLinearShading()) {
		this.outline.setFill( {
			type : "linear",
			x1 : 0,
			y1 : 0,
			x2 : 150,
			y2 : 80,
			colors : [ {
				offset : 0,
				color : this.componentAppearance.getBackgroundColor()
			}, {
				offset : 1,
				color : this.componentAppearance.getBackgroundContrastColor() 
			} ]
		});
	} else {
		this.outline.setFill(this.componentAppearance.getBackgroundColor());

	}
	// console.log("Movng to front");
	// make sure we can be seen.
	this.group.moveToFront();
	// console.log("<Component.refresh");
},
update : function(id, props, children) {

	// console.log("Updating "+id+", processing properties array.. ");

	// rebuilds the prop array internally
	this.updateProperties(props);

	// console.log("Updating "+id+", rebuilding onscreen with new props ");
	// rebuild the onscreen object with the layout mgr
	this.refresh();

	// TODO: update children.

	// tells everyone who cares that we just did that.
	dojo.publish("goat.component.update." + this.id, [ this ]);
},
moveToNewPlace : function(x, y) {
	if (!this.hidden) {
		this.group.setTransform( {
			dx : x,
			dy : y
		});
	}
	this.x = x;
	this.y = y;
	this.updateAfterMove();
},
toggleHidden : function() {
	var hideMe = !this.hidden;
	if (hideMe) {
		// cheat.. move it off canvas..
	this.group.setTransform( {
		dx : -1000,
		dy : -1000
	});
} else {
	// bring it back =) good job we remembered where it was supposed to go !
	this.group.setTransform( {
		dx : this.x,
		dy : this.y
	});
}

this.hidden = hideMe;
dojo.publish("goat.component.hidden", [ this.id, hideMe ]);
dojo.publish("goat.component.hidden." + this.id, [ this ]);
},
onMoved : function(mover, shift) {
// this may stop working once this.group isnt directly within the surface
	// (ie once child components are renderable)
	this.x = this.group.matrix.dx;
	this.y = this.group.matrix.dy;

	this.updateAfterMove();
},
updateAfterMove : function() {
	dojo.publish("goat.component.move", [ this.id, this.x, this.y ]);
	dojo.publish("goat.component.move." + this.id, [ this ]);
},
pulse : function() {
	if(this.selected) {
		var endColor = this.componentAppearance.getOutlineColor1();
	} else { 
		var endColor =  this.componentAppearance.getOutlineColor0();
	}
	dojox.gfx.fx.animateStroke( {
		shape : this.outline,
		duration : 500,
		color : {
			start :  this.componentAppearance.getOutlineColor2(),
			end : endColor
		},
		width : {
			start : 7,
			end : 2
		},
		join : {
			values : [ "miter", "bevel", "round" ]
		}
	}).play();
},
glow : function() {
	if(this.selected) {
		var endColor =  this.componentAppearance.getOutlineColor1();
	} else {
		var endColor =  this.componentAppearance.getOutlineColor0(); 
	}
	dojox.gfx.fx.animateStroke( {
		shape : this.outline,
		duration : 500,
		color : {
			start :  this.componentAppearance.getOutlineColor2(),
			end : endColor
		},
		width : {
			start : 3,
			end : 2
		},
		join : {
			values : [ "miter", "bevel", "round" ]
		}
	}).play();
},
onClick : function() {
	this.group.moveToFront();
	this.pulse();
	dojo.publish("goat.component.onclick." + this.id, [ this ]);
},
onMouseEnter : function() {
	this.outline.setStroke( {
		width : 3,
		color :  this.componentAppearance.getOutlineColor1() 
	});
	this.selected = true;
	dojo.publish("goat.component.onenter." + this.id, [ this ]);
},
onMouseLeave : function() {
	this.selected = false;
	this.glow();
	dojo.publish("goat.component.onexit." + this.id, [ this ]);
},
onRefresh : function() {
	if (!this.refreshing) {
		this.refreshing = true;
		this.refresh();
		this.refreshing = false;
	}
},
getComponentAppearance : function() {
	return this.componentAppearance;
}

});
