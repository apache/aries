package org.apache.aries.samples.transaction;

import javax.transaction.RollbackException;

public class GenericJTAException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3760423779101497679L;

	public GenericJTAException(Exception e) {
		super (e);
	}

}
