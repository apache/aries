package org.apache.aries.transaction;

import javax.transaction.Transactional.TxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransactionalAnnotationAttributes {

	private TxType txType;
	private List<Class> rollbackOn = new ArrayList<Class>();
	private List<Class> dontRollbackOn = new ArrayList<Class>();

	public TransactionalAnnotationAttributes(TxType defaultType) {
		this.txType = defaultType;
	}

	public TransactionalAnnotationAttributes(TxType txType, Class[] dontRollbackOn, Class[] rollbackOn) {
		this.txType = txType;
		if (dontRollbackOn != null) {
			this.dontRollbackOn = Arrays.asList(dontRollbackOn);
		}
		if (rollbackOn != null) {
			this.rollbackOn = Arrays.asList(rollbackOn);
		}
	}

	public TxType getTxType() {
		return txType;
	}

	public void setTxType(TxType txType) {
		this.txType = txType;
	}

	public List<Class> getRollbackOn() {
		return rollbackOn;
	}

	public void setRollbackOn(List<Class> rollbackOn) {
		this.rollbackOn = rollbackOn;
	}

	public List<Class> getDontRollbackOn() {
		return dontRollbackOn;
	}

	public void setDontRollbackOn(List<Class> dontRollbackOn) {
		this.dontRollbackOn = dontRollbackOn;
	}
}
