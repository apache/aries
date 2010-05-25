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
dojo.provide("goat.LayoutManager");
dojo.require("dijit.form.Button");
dojo.require("dijit.form.ComboBox");
dojo.require("dojo.data.ItemFileWriteStore");

//LayoutManager
// looks after the loading & saving of arrangements of components on the surface.
// stores configurations by name, by provider.
//
// listens for provider change notifications to know what the current provider is,
// when a provider is selected, all current layouts are discarded, and the layout
// set for the new provider is setup.
//
// the list of layouts for a provider is stored in a cookie as 
//    "ozzy.demo.ui.layouts.<providerName>"
// the layout data for each layout is stored in a cookie as 
//    "ozzy.demo.ui.coords.<providerName>.<layoutName>"
//
// both are stored as a json encoded array.
//
// if a provider is selected that we havent seen before, a new cookie is created
// with a layout entry of 'default'.
//
// coord save load is now based over component name & version, not id.
// load will only move components it is able to match, 
// if a provider gives back different components to the ones saved with coords, the
// mismatches are ignored.
//
// TODO:
//   - delete Layout.
//   - (possible) remove load button & trigger off of selection of dropdown.
//   - writeProtect 'default' so it is always the one seen 1st time.
//   - activate loadCoords after provider change.
//   - store 'active' layout to reload on switching to provider
//   - allow dynamic contributions to layout from layout plugins
//     these would add fixed entries to the layout dropdown, and would
//     arrange the layout according to logic rather than a saved layout.
//     these could live server side, or client side, and be added via ajax.
dojo.declare("goat.LayoutManager", [], {
	loadButton:null,
	saveButton:null,
	layoutSelector:null,
	disabled:true,
	items:[],
	layouts:null,
	
	constructor : function(where) {
	  var _this=this;
	  this.loadButton = new dijit.form.Button( 
			  {label: "Load Coords", onClick: function(){_this.loadCoords();} });
	  this.saveButton = new dijit.form.Button( 
			  {label: "Save Coords", onClick: function(){_this.saveCoords();} });
	  
      this.layouts = new dojo.data.ItemFileWriteStore( {data : {identifier: 'name', items: this.items }});
      
	  this.layoutSelector = new dijit.form.ComboBox( {id: "BundleLayoutSelector",  store: this.layouts} );
	  
	  this.layoutSelector.attr('value',"default");
	    
	  dojo.byId(where).appendChild(this.layoutSelector.domNode);
	  dojo.byId(where).appendChild(this.loadButton.domNode);
	  dojo.byId(where).appendChild(this.saveButton.domNode);
	  	  
	  this.disable();
	  
	  dojo.subscribe("goat.provider.change", this, this.onProviderChange);
	},
	onProviderChange: function(evt){
		  console.log("OnProviderChange");
		  console.log(evt);
		  this.enable();
		  
		  this.removeAllLayoutsFromMenu();
			
		  console.log("Adding default if missing");
		  this.addDefaultLayoutIfMissing(providerSelector.getProvider());	 	  
		  this.getLayoutsForProvider(providerSelector.getProvider());
		  
		  this.layoutSelector.attr('value',"default");
	},
	removeAllLayoutsFromMenu: function(){
		  this.layoutSelector.attr('value',"");
		  var _this=this;
			function deleteItems(items, request){
				console.log("total removal query");
				console.log(items);
				console.log(request);			
				dojo.forEach(items, function(item){
					console.log("deleting "+item.name);
					_this.layouts.deleteItem(item);
				});		
			    console.log("end process");
			}
			this.layouts.fetch({query:{}, onComplete: deleteItems});
			this.layouts.save();
			
			this.items=[];
			this.layouts.close();
	},
	enable: function(){
		this.loadButton.attr('disabled',false);
		this.saveButton.attr('disabled',false);
		this.layoutSelector.attr('disabled',false);
	},
	disable: function(){
		this.loadButton.attr('disabled',true);
		this.saveButton.attr('disabled',true);
		this.layoutSelector.attr('disabled',true);
	},
	addDefaultLayoutIfMissing: function(provider){
		  var component_layouts_str = dojo.cookie("ozzy.demo.ui.layouts."+provider);
		  if(component_layouts_str==null){
			    console.log("No cookie found listing layouts.. adding default & creating one.");
				this.layouts.newItem({name: "default"});
				this.layouts.save();
				this.saveLayoutsToCookie();
				this.layoutSelector.attr('value',"default");
				this.saveCoords();
				this.removeAllLayoutsFromMenu();
		  }	
	},
	saveLayoutsToCookie: function(){
		var name = "ozzy.demo.ui.layouts."+providerSelector.getProvider();
		console.log("Saving layouts to "+name);
		var names = new Array();		
		
		var _this=this;
		function process(items, request){
			console.log("total namegather query");
			console.log(items);
			console.log(request);			
			dojo.forEach(items, function(item){
				var name = _this.layouts.getValue(item, 'name');
				console.log("name "+name);
				names.push(name);
			});		
		    console.log("end process");
		}
		this.layouts.fetch({query:{}, onComplete: process});
		
		//dojo.forEach(this.items, function(item){ console.log(item); console.log(item.name[0]); names.push(item.name[0])});
		console.log("Names to save..")
		console.log(names);
		dojo.cookie(name, dojo.toJson(names));
		console.log("Saved");
	},
	getLayoutsForProvider: function(provider){	  			 
	  var component_layouts_str = dojo.cookie("ozzy.demo.ui.layouts."+provider);
	  if(component_layouts_str!=null){
		  var names = dojo.fromJson(component_layouts_str);
		  console.log("Loaded for provider"+provider);
		  console.log(component_layouts_str);
		  console.log(names);
		  var _this=this;
		  dojo.forEach(names, function(name){
			  _this.layouts.newItem({name: name});
		  });
		  this.layouts.save();
		  this.layoutSelector.attr('value',"default");
	  }else{
		  //do-nothng.
	  }
	  	  
	},	
	saveCoords: function() {
		console.log("Save coords");
		var component_coords = new Array();
		var idx=0;
		var coords_cookie="";
		
		var coord_data = new Array();
		
		dojo.forEach(components, function(component){
			if(component!=null){
				var x=component.x;
				var y=component.y;		
				var h=component.hidden ? 1 : 0;
				var name=component.elements["component.property.SymbolicName"].value;
				var version=component.elements["component.property.Version"].value;
				
				coord_data.push( {name:name, ver:version, x: x, y: y, h: h});			
			}		
		});	
		var coords_json = dojo.toJson(coord_data);
				
		dojo.cookie("ozzy.demo.ui.coords."+providerSelector.getProvider()+"."+this.layoutSelector.attr('value'), coords_json);
		
		var match=false;
		var name = this.layoutSelector.attr('value');
		console.log("checking layouts for "+name);
	    this.layouts.fetch({query: { name: name }, onComplete: function(item){
	    	if(item!=null && item.length>0){
	    		console.log("Found it");
	    		console.log(item);
	    		match=true;
	    	}
	    }});
	    console.log("adding if needed");
	    if(!match){
	    	console.log("adding "+name);
			this.layouts.newItem({name: name});
			this.layouts.save();
			console.log("pushing to cookie");
			this.saveLayoutsToCookie();
	    }
		
		//dwr.util.setValue("coords_status", "Done");
	},
	loadCoords: function() {
		console.log("Load coords");
		var component_coords_str = dojo.cookie("ozzy.demo.ui.coords."+providerSelector.getProvider()+"."+this.layoutSelector.attr('value'));
		if(component_coords_str != null){
			var component_coord_data = dojo.fromJson(component_coords_str);
			dojo.forEach(component_coord_data, function(ci){
				var id=-1;
				dojo.forEach(components, function(c){ 
						if(c!=null){
							console.log(c);
							console.log("Inside foreach");
							var name=c.elements["component.property.SymbolicName"].value;
							var version=c.elements["component.property.Version"].value;							
							if((name==ci.name) && (version==ci.ver)){
								id=c.id;
							} 
						}
					});
				
				console.log("id "+id);
				
				//we may not know the component yet.. just ignore it for now.
				if(components[id]!=null){				
					components[id].x = ci.x;
					components[id].y = ci.y;	
					components[id].moveToNewPlace(ci.x, ci.y);					
					var h2=components[id].hidden ? 1 : 0;
					//	console.log("Bundle "+id+" had hidden flag of "+h+" ("+h2+") and current state of "+components[id].hidden);
					if( ci.h!=h2 ){
						//console.log("Flipping hidden state on component.."); 
						components[id].toggleHidden();
					}
				}				
			});

			//dwr.util.setValue("coords_status", "Done");
		}else{
			//dwr.util.setValue("coords_status", "No cookie found");
		}
	}	
});
