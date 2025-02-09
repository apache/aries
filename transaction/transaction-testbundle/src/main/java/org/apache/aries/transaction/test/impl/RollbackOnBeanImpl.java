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
package org.apache.aries.transaction.test.impl;

import org.apache.aries.transaction.test.RollbackOnBean;
import org.apache.aries.transaction.test.TestBean;

import javax.transaction.Transactional;
import java.sql.SQLException;

import static javax.transaction.Transactional.TxType;

public class RollbackOnBeanImpl implements RollbackOnBean {

	private TestBean rBean;

	@Override
	@Transactional(value = TxType.REQUIRED)
	public void throwException(String name, int value) throws Exception {
		rBean.insertRow(name, value, null);
		throw new Exception("Test exception");
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
	public void throwExceptionRollbackOnException(String name, int value) throws Exception {
		rBean.insertRow(name, value, null);
		throw new Exception("Test exception");
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
	public void throwRuntimeExceptionRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = SQLException.class)
	public void throwRuntimeExceptionRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = Exception.class)
	public void throwRuntimeExceptionDontRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = SQLException.class)
	public void throwRuntimeExceptionDontRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
	public void throwApplicationExceptionRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, rollbackOn = SQLException.class)
	public void throwApplicationExceptionRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = Exception.class)
	public void throwApplicationExceptionDontRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = SQLException.class)
	public void throwApplicationExceptionDontRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = Exception.class, rollbackOn = SQLException.class)
	public void throwExceptionRollbackOnExceptionDontRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = SQLException.class, rollbackOn = Exception.class)
	public void throwExceptionRollbackOnAppExceptionDontRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new RuntimeException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = SQLException.class, rollbackOn = Exception.class)
	public void throwAppExceptionRollbackOnExceptionDontRollbackOnAppException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}

	@Override
	@Transactional(value = TxType.REQUIRED, dontRollbackOn = Exception.class, rollbackOn = SQLException.class)
	public void throwAppExceptionRollbackOnAppExceptionDontRollbackOnException(String name, int value) throws SQLException {
		rBean.insertRow(name, value, new SQLException("Test exception"));
	}


	@Override
	public void setrBean(TestBean rBean) {
		this.rBean = rBean;
	}

}
