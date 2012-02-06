package org.apache.aries.transaction.pojo;

import org.apache.aries.transaction.annotations.Transaction;
import org.apache.aries.transaction.annotations.TransactionPropagationType;

public class BadlyAnnotatedPojo1 {
	
	@Transaction
	public void increment(String key) {}

	@Transaction(TransactionPropagationType.Supports)
	protected int checkValue(String key) {
		return 0;
	}
	
	@Transaction(TransactionPropagationType.Mandatory)
	Object getRealObject(String key) {
		return null;
	}
	
	@Transaction
	private void doesNotWork() {}
}
