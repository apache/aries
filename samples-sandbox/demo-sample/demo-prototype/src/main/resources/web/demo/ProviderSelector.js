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
dojo.provide("demo.ProviderSelector");
dojo.require("dojo.data.ItemFileWriteStore");
dojo.require("dijit.form.ComboBox");
dojo.require("dijit.form.Select");
dojo.require("dijit.form.FilteringSelect");

//ProviderSelector
// looks after the providers, and allows selection of one. 
// 
// when user uses a new provider, 
// - asks the server for the bundles for that provider
// - issues an event to 
//     "demo.provider.change"
//   with the new provider as the payload.
// 
//TODO:
// - Make the provider list dynamic, currently providers are only retrieved at construction
//   Suspect this also requires a serverside rewrite to use a serviceTracker, rather than a single lookup.
//   - add is already present, remove will need adding for dynamic.
// - (possible) auto activate provider on select, rather than requiring a 'use' button.
// - respond to server setting a provider (needed if another client alters the view on a shared session {the default})
dojo.declare("demo.ProviderSelector", [], {
	
	providerSelector:null,
	whereProvider:null,
	whereOptions:null,
	providers:null,
	initialSelect:true,
	
constructor : function( whereProvider) {
	//and this stuff, should really be combined with the provider stuff to make 
	//cookie stored provider configs.
	//dojo.byId(whereProvider).appendChild("Data Provider: ");
	this.whereProvider = whereProvider;

	console.log("creating store");
    var providers = new dojo.data.ItemFileWriteStore( {data : {identifier: 'value', label: 'label', items:[]}});
	this.providerSelector = new dijit.form.FilteringSelect( {id: "ProviderSelector",  searchAttr: "value", store: providers} );
	this.providers=providers;
	
	//oddball hackery to link button to this instance.. 
	//otherwise 'this' inside the handleButton becomes the Button, not me.
	var _this=this;
	var useButton = new dijit.form.Button( 
			  {label: "Use Provider", onClick: function(){
				  _this.handleButton();
			  }});
	
	console.log("looking up.. ");
	dojo.byId(whereProvider).appendChild(this.providerSelector.domNode);
	dojo.byId(whereProvider).appendChild(useButton.domNode);
    
	var _this=this;
	var providerCallback = function(data)
	{
		console.log("callback...")
		console.log(data);
		if (data != null && typeof data == 'object'){
			dojo.forEach( data, function(provider){			      
				_this.addProvider(provider);
			});
		}
	}
	console.log("Requesting initial providers");
	ServerSideClass.getProviders(providerCallback);		
},
addProvider: function(provider){
	console.log("adding provider "+provider);
	this.providers.newItem({value: provider, label: provider});
	if(this.initialSelect){
		this.providerSelector.attr('value',provider);
		this.initialSelect=false;		
	}
},
notifyChangeOfProvider: function(provider){

	this.providerSelector.attr('value',provider);
	console.log("getting bundles for prov");
	ServerSideClass.getInitialBundles(this.getProvider(), "");
	console.log("Publishing provider change to "+provider);
	dojo.publish("demo.provider.change",[provider]);
},
getProvider: function(){
	return dijit.byId("ProviderSelector").value;
},
handleButton: function(){
	this.notifyChangeOfProvider(this.getProvider());
}
		
});
