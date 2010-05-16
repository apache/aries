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
dojo.provide("demo.LayoutManager");
dojo.require("dijit.form.Button");
dojo.require("dijit.form.ComboBox");
dojo.require("dojo.data.ItemFileWriteStore");

//LayoutManager
// looks after the loading & saving of arrangements of bundles on the surface.
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
// coord save load is now based over bundle name & version, not id.
// load will only move bundles it is able to match, 
// if a provider gives back different bundles to the ones saved with coords, the
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
dojo.declare("demo.LayoutManager", [], {
	loadButton:null,
	saveButton:null,
	switchButton:null,
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
	  
	  this.hideButton = new dijit.form.Button( 
			  {label: "Hide All", onClick: function(){_this.hideAll();} });  
	  
	  this.showButton = new dijit.form.Button( 
			  {label: "Show All", onClick: function(){_this.showAll();} });  
	  
      this.layouts = new dojo.data.ItemFileWriteStore( {data : {identifier: 'name', items: this.items }});
      
	  this.layoutSelector = new dijit.form.ComboBox( {id: "BundleLayoutSelector",  store: this.layouts} );
	  
	  this.layoutSelector.attr('value',"default");
	    
	  dojo.byId(where).appendChild(this.layoutSelector.domNode);
	  dojo.byId(where).appendChild(this.loadButton.domNode);
	  dojo.byId(where).appendChild(this.saveButton.domNode);
	  dojo.byId(where).appendChild(this.hideButton.domNode);
	  dojo.byId(where).appendChild(this.showButton.domNode);
	  	  
	  this.disable();
	  
	  dojo.subscribe("demo.provider.change", this, this.onProviderChange);
	},
	
	hideAll: function() {
		console.log("Hiding all bundles");
		dojo.forEach(bundles, function(bundle){
			if(bundle!=null){
				//If it isn't hidden already
				if(!bundle.hidden) {
					bundle.toggleHidden();
				}
			}
	    });
	},
	
	showAll: function() {
		console.log("Showing all bundles");
		dojo.forEach(bundles, function(bundle){
			if(bundle!=null){
				//If bundle is hidden make it visible
				if(bundle.hidden) {
					bundle.toggleHidden();
				}
			}
	    });
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
		  var bundle_layouts_str = dojo.cookie("ozzy.demo.ui.layouts."+provider);
		  if(bundle_layouts_str==null){
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
	  var bundle_layouts_str = dojo.cookie("ozzy.demo.ui.layouts."+provider);
	  if(bundle_layouts_str!=null){
		  var names = dojo.fromJson(bundle_layouts_str);
		  console.log("Loaded for provider"+provider);
		  console.log(bundle_layouts_str);
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
		var bundle_coords = new Array();
		var idx=0;
		var coords_cookie="";
		
		var coord_data = new Array();
		
		dojo.forEach(bundles, function(bundle){
			if(bundle!=null){
				var x=bundle.x;
				var y=bundle.y;		
				var h=bundle.hidden ? 1 : 0;
				
				coord_data.push( {name:bundle.name, ver:bundle.version, x: x, y: y, h: h});			
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
		var bundle_coords_str = dojo.cookie("ozzy.demo.ui.coords."+providerSelector.getProvider()+"."+this.layoutSelector.attr('value'));
		if(bundle_coords_str != null){
			var bundle_coord_data = dojo.fromJson(bundle_coords_str);
			dojo.forEach(bundle_coord_data, function(bi){
				var id=-1;
				dojo.forEach(bundles, function(b){ 
						if(b!=null){
							console.log(b);
							console.log("Inside foreach");
							if((b.name==bi.name) && (b.version==bi.ver)){
								id=b.id;
							} 
						}
					});
				
				console.log("id "+id)
				
				//we may not know the bundle yet.. just ignore it for now.
				if(bundles[id]!=null){				
					bundles[id].x = bi.x;
					bundles[id].y = bi.y;						
					moveBundle(bundles[id], bi.x, bi.y);
					
					var h2=bundles[id].hidden ? 1 : 0;
					//	console.log("Bundle "+id+" had hidden flag of "+h+" ("+h2+") and current state of "+bundles[id].hidden);
					if( bi.h!=h2 ){
						//console.log("Flipping hidden state on bundle.."); 
						bundles[id].toggleHidden();
					}
				}				
			});

			//dwr.util.setValue("coords_status", "Done");
		}else{
			//dwr.util.setValue("coords_status", "No cookie found");
		}
	}	
});
