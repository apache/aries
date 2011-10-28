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
dojo.provide("goat.DwrLoadingDialog");
dojo.require("dijit.Dialog");

//not sure this is working anymore..
//it worked best when js was making requests TO the server.. 
//but we've got it flipped so that the js makes a quick request, which causes the server to make calls back to us
//might revisit this at some point to try to have the long requests all be client initiated, so we can display the dialog.

dojo.declare("goat.DwrLoadingDialog", [], {
	loadingMessage:"Loading data from server..<br>waiting for <span id='count'>0</span> items",
    dialog:null,
    count:0,
	constructor : function() {
		this.dialog = new dijit.Dialog({title:"Loading...", id:"loadDialog"});
		this.dialog.attr('content', this.loadingMessage);
		dojo.body().appendChild(this.dialog.domNode);
		this.dialog.hide();
		
		dwr.engine.setPreHook(this.dwrPreHook);
		dwr.engine.setPostHook(this.dwrPostHook);
	},
	dwrPreHook: function(){
		console.log("***************** PRE HOOK");
		if(this.count==0){
			this.dialog.show();
			this.count++;
			dojo.byId('count').innerHtml = ""+this.count;
		}
	},
	dwrPostHook: function(){
		console.log("***************** POST HOOK");
		this.count--;
		if(this.count==0) this.dialog.hide();
		dojo.byId('count').innerHtml = ""+this.count;
	}	
});
