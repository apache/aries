package org.apache.aries.tx.control.service.xa.impl;

import static org.apache.aries.tx.control.service.xa.impl.LocalResourceSupport.ENFORCE_SINGLE;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * This will be more useful once the OSGi converter exists, for now it just
 * generates a metatype for this service.
 */
@ObjectClassDefinition(pid=Activator.PID, description="Apache Aries Transaction Control Service (XA)")
@interface Config {
	@AttributeDefinition(name="Enable recovery logging", required=false, description="Enable recovery logging")
	boolean recovery_log_enabled() default false;

	@AttributeDefinition(name="Recovery Log storage folder", required=false, description="Transaction Recovery Log directory")
	boolean recovery_log_dir();
	
	@AttributeDefinition(name="Transaction Timeout", required=false, description="Transaction Timeout in seconds")
	int transaction_timeout() default 300;
	
	@AttributeDefinition(name="Local Resources", required=false, description="Allow Local Resources to participate in transactions")
	LocalResourceSupport local_resources() default ENFORCE_SINGLE;
}