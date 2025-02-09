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
