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
dojo.provide("goat.ComponentStatusGrid");
dojo.require("dojox.grid.DataGrid");
dojo.require("dojo.data.ItemFileWriteStore");

//TODO:

//this has been partly converted to components.. 
//it knows too much about component properties at present.

dojo.declare("goat.ComponentStatusGrid", [], {
	hidecoords: true,
	grid:null,
	jsonStore:null,
	dataItems:{ identifier : 'id',	label : 'name',	items : [] },
	lastMouseOverIndex:-1,
constructor : function(where) {
	var layout = [ 
	{
		name : "Unique ID",
		field : "id",
		width : "auto",
		hidden : true
	}, {
		name : "ID",
		field : "bundleid",
		width : "20px"
	}, {
		name : "SymbolicName",
		field : "name",
		width : "auto" //"155px"
	}, {
		name : "State",
		field : "state",
		width : "12px",
		formatter : this.makeStateImage
	}, {
		name : "x",
		field : "x",
		width : "15px",
	    hidden : this.hidecoords
	}, {
		name : "y",
		field : "y",
		width : "15px",
		hidden : this.hidecoords
	}, {
		name: 'Show', 
		field: 'id', 
		width: '22px', 
		formatter: this.makeHideButton
	}		
	];

	this.jsonStore = new dojo.data.ItemFileWriteStore( {
		data : this.dataItems
	});

	var grid = new dojox.grid.DataGrid( {
		structure : layout,
		store : this.jsonStore
	}, where);
	grid.startup();
	
	var _this=this;

	dojo.connect(window, "onresize", grid, "resize");
	dojo.connect(grid, "onRowClick", function(evt){
		var items = evt.grid.selection.getSelected();
		if(items!=null){
			dojo.forEach( items, function(component){
				console.log(component);
				console.log(component.id);
				components[component.id].onClick();
			});
		} 
	});
	dojo.connect(grid, "onRowMouseOver", function(evt){
		if(_this.lastMouseOverIndex!=evt.rowIndex){
			components[evt.grid.getItem(evt.rowIndex).id].glow();			
			_this.lastMouseOverIndex=evt.rowIndex;
		}
	});
	dojo.connect(grid, "onRowMouseOut", function(evt){
		if(_this.lastMouseOverIndex!=-1){
			_this.lastMouseOverIndex=-1;
		}
	});	

	dojo.subscribe("goat.component.create", this, this.onComponentCreate);
	dojo.subscribe("goat.component.delete", this, this.onComponentDelete);
	dojo.subscribe("goat.component.update", this, this.onComponentUpdate);
	dojo.subscribe("goat.component.hidden", this, this.onComponentHidden);
	
	if(!this.hidecoords){
	  dojo.subscribe("goat.component.move", this, this.onComponentMove);
	}
},
onComponentCreate: function(/*goat.Component*/ component){
	//console.log(">onComponentCreate");
	//console.log(component);
	//add the component to the backing store
	if (this.jsonStore != null) {
			
		//read through the property elements to their values.. 
		var id = component.elements["component.property.BundleID"].value;
		var name = component.elements["component.property.SymbolicName"].value;
		var state = component.elements["component.property.State"].value;
		var version = component.elements["component.property.Version"].value;
		
		this.jsonStore.newItem({id: component.id, bundleid: id, name: name, state: state, version: version, x: component.x, y: component.y});
	}
	this.jsonStore.save();
	//console.log("<onComponentCreate");
},
onComponentDelete: function(component){
	//console.log("onComponentDelete");
	//console.log(component);
	//console.log("invoking fetch for "+component.id);
	var _this=this;
	this.jsonStore.fetch({query: { id: component.id }, onComplete: function(item){
		//console.log("Deleting "+item[0].id);
		//console.log(item[0]);
		_this.jsonStore.deleteItem(item[0]);
	}});
	this.jsonStore.save();
	//console.log("<onComponentDelete");
},
onComponentUpdate: function(component){
	//console.log(">onComponentUpdate");

	var bid = component.elements["component.property.BundleID"].value;
	var name = component.elements["component.property.SymbolicName"].value;
	var state = component.elements["component.property.State"].value;
	var version = component.elements["component.property.Version"].value;

	var _this=this;
	this.jsonStore.fetch({query: { id: id }, onComplete: function(item){
		_this.jsonStore.setValue(item[0], "bundleid", bid);
		_this.jsonStore.setValue(item[0], "name", name);
		_this.jsonStore.setValue(item[0], "state", state);
		_this.jsonStore.setValue(item[0], "version", version);
	}});
	this.jsonStore.save();
	//console.log("<onComponentUpdate");
},
onComponentHidden: function(id,hidden){
	//console.log(">onComponentHidden");
	var _this=this;
	this.jsonStore.fetch({query: { id: id }, onComplete: function(item){
		_this.jsonStore.setValue(item[0], "hidden", hidden);
	}});
	this.jsonStore.save();
	//console.log("<onComponentHidden");
},
onComponentMove: function(id,x,y){
	//optimisation, only process the move if we are displaying the coords...
	if(!this.hidecoords){
		//console.log("onComponentMove");
		var _this=this;
		this.jsonStore.fetch({query: { id: id }, onComplete: function(item){
			_this.jsonStore.setValue(item[0], "x", x);
			_this.jsonStore.setValue(item[0], "y", y);
		}});
		this.jsonStore.save();
    }
},
makeHideButton: function(pk){
	var checked="";
	var text="Show "+components[pk].id;
	if(!components[pk].hidden){
		checked="checked";
		text="Hide "+components[pk].id;
	}
	var showBox = "<div dojoType=\"dijit.form.Button\">";
	showBox = showBox + "<input type=\"checkbox\" ";
	showBox = showBox + "title=\""+text+"\" ";
	showBox = showBox + "onClick=\"hideComponent('"+pk+"')\" "+checked+">";
	showBox = showBox + "</div>";

	//return hideButton;
	return showBox;
},
makeHideStatus: function(hidden){
	var img;
	if(hidden){
		img="\"../dojo/dojo/resources/images/dndCopy.png\"";
	}else{
		img="\"../dojo/dojo/resources/images/dndNoCopy.png\"";
	}
	var hideButton = "<img src="+img;
	hideButton = hideButton + " width=\"18\" height=\"18\"";
	hideButton = hideButton + "\">";
	return hideButton;		
},
makeStateImage: function(state){
	var img=null;

	if(state=="UNINSTALLED"){
		img = "../images/dndNoMove.png";
	}else if(state=="INSTALLED"){
		img = "../images/dndCopy.png";
	}else if(state=="RESOLVED"){
		img = "../images/dndNoCopy.png";
	}else if(state=="STARTING"){
		//todo: starting img
		img = "../images/resolved.png";
	}else if(state=="STOPPING"){
		//todo: stopping img
		img = "../images/resolved.png";
	}else if(state=="ACTIVE"){
		img = "../images/running.png";
	}	
	var hideButton = "";
	if(img!=null){
		hideButton = hideButton + "<img src=\""+img;
		hideButton = hideButton + "\" width=\"12\" height=\"12\" alt=\"";
		hideButton = hideButton + state + "\" title=\""+state+"\">";
	}
	return hideButton;		    		
}
});
