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
dojo.provide("demo.BundleStatusGrid");
dojo.require("dojox.grid.DataGrid");
dojo.require("dojo.data.ItemFileWriteStore");

dojo.declare("demo.BundleStatusGrid", [], {
	grid:null,
	jsonStore:null,
	dataItems:{ identifier : 'id',	label : 'name',	items : [] },
	lastMouseOverIndex:-1,
constructor : function(where) {
	var layout = [ 
	{
		name : "ID",
		field : "id",
		width : "20px"
	}, {
		name : "SymbolicName",
		field : "name",
		width : "155px"
	}, {
		name : "State",
		field : "state",
		width : "12px",
		formatter : this.makeStateImage
	}, {
		name : "x",
		field : "x",
		width : "15px"
	}, {
		name : "y",
		field : "y",
		width : "15px"
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
			dojo.forEach( items, function(bundle){
				console.log(bundle);
				console.log(bundle.id);
				//I'd like to just do bundle.pulse here, but that doesnt work..
				bundles[bundle.id].pulse();
			});
		} 
	});
	dojo.connect(grid, "onRowMouseOver", function(evt){
		if(_this.lastMouseOverIndex!=evt.rowIndex){
			bundles[evt.grid.getItem(evt.rowIndex).id].glow();			
			_this.lastMouseOverIndex=evt.rowIndex;
		}
	});
	dojo.connect(grid, "onRowMouseOut", function(evt){
		if(_this.lastMouseOverIndex!=-1){
			_this.lastMouseOverIndex=-1;
		}
	});	

	dojo.subscribe("demo.bundle.create", this, this.onBundleCreate);
	dojo.subscribe("demo.bundle.delete", this, this.onBundleDelete);
	dojo.subscribe("demo.bundle.update", this, this.onBundleUpdate);
	dojo.subscribe("demo.bundle.hidden", this, this.onBundleHidden);
	dojo.subscribe("demo.bundle.move", this, this.onBundleMove);
},
onBundleCreate: function(bundle){
	console.log(">onBundleCreate");
	console.log(bundle);
	//add the bundle to the backing store
	if (this.jsonStore != null) {
		this.jsonStore.newItem(bundle);
	}
	this.jsonStore.save();
	console.log("<onBundleCreate");
},
onBundleDelete: function(bundle){
	console.log("onBundleDelete");
	console.log(bundle);
	console.log("invoking fetch for "+bundle.id);
	var _this=this;
	this.jsonStore.fetch({query: { id: bundle.id }, onComplete: function(item){
		console.log("Deleting "+item[0].id);
		console.log(item[0]);
		_this.jsonStore.deleteItem(item[0]);
	}});
	this.jsonStore.save();
	console.log("<onBundleDelete");
},
onBundleUpdate: function(id,name,state,version){
	console.log(">onBundleUpdate");
	var _this=this;
	this.jsonStore.fetch({query: { id: id }, onComplete: function(item){
		_this.jsonStore.setValue(item[0], "name", name);
		_this.jsonStore.setValue(item[0], "state", state);
		_this.jsonStore.setValue(item[0], "version", version);
	}});
	this.jsonStore.save();
	console.log("<onBundleUpdate");
},
onBundleHidden: function(id,hidden){
	console.log(">onBundleHidden");
	var _this=this;
	this.jsonStore.fetch({query: { id: id }, onComplete: function(item){
		_this.jsonStore.setValue(item[0], "hidden", hidden);
	}});
	this.jsonStore.save();
	console.log("<onBundleHidden");
},
onBundleMove: function(id,x,y){
	//console.log("onBundleMove");
	var _this=this;
	this.jsonStore.fetch({query: { id: id }, onComplete: function(item){
		_this.jsonStore.setValue(item[0], "x", x);
		_this.jsonStore.setValue(item[0], "y", y);
	}});
	this.jsonStore.save();
},
makeHideButton: function(pk){
	var checked="";
	var text="Show "+bundles[pk].name;
	if(!bundles[pk].hidden){
		checked="checked";
		text="Hide "+bundles[pk].name;
	}
	var showBox = "<div dojoType=\"dijit.form.Button\">";
	showBox = showBox + "<input type=\"checkbox\" ";
	showBox = showBox + "title=\""+text+"\" ";
	showBox = showBox + "onClick=\"hideBundle('"+pk+"')\" "+checked+">";
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
