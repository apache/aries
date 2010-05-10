// dojo.provide allows pages use all types declared in this resource
dojo.provide("demo.DwrLoadingDialog");
dojo.require("dijit.Dialog");

dojo.declare("demo.DwrLoadingDialog", [], {
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