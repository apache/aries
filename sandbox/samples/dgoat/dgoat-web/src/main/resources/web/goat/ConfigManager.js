//dojo.provide allows pages use all types declared in this resource
dojo.provide("goat.ConfigManager");

//not even started this yet.. the idea is to have a config panel for the global config, allowing 
//all sorts of magic stuff to be done ;-) .. yeah.. right..

dojo.declare("goat.ConfigManager", [], {
	
    dialog:null,

	constructor : function() {
		this.dialog = new dijit.Dialog({title:"Configuration", id:"configDialog"});

		dojo.body().appendChild(this.dialog.domNode);
		this.dialog.hide();
}



});
