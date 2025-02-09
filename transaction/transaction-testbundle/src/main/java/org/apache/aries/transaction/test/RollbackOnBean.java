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
package org.apache.aries.transaction.test;

import java.sql.SQLException;

public interface RollbackOnBean {

	void throwException(String name, int value) throws Exception;

	void throwExceptionRollbackOnException(String name, int value) throws Exception;

	void throwRuntimeExceptionRollbackOnException(String name, int value) throws SQLException;

	void throwRuntimeExceptionDontRollbackOnException(String name, int value) throws SQLException;

	void throwRuntimeExceptionDontRollbackOnAppException(String name, int value) throws SQLException;

	void throwRuntimeExceptionRollbackOnAppException(String name, int value) throws SQLException;

	void throwApplicationExceptionRollbackOnException(String name, int value) throws SQLException;

	void throwApplicationExceptionRollbackOnAppException(String name, int value) throws SQLException;

	void throwApplicationExceptionDontRollbackOnException(String name, int value) throws SQLException;

	void throwApplicationExceptionDontRollbackOnAppException(String name, int value) throws SQLException;

	void throwExceptionRollbackOnExceptionDontRollbackOnAppException(String name, int value) throws SQLException;

	void throwExceptionRollbackOnAppExceptionDontRollbackOnException(String name, int value) throws SQLException;

	void throwAppExceptionRollbackOnExceptionDontRollbackOnAppException(String name, int value) throws SQLException;

	void throwAppExceptionRollbackOnAppExceptionDontRollbackOnException(String name, int value) throws SQLException;

	void setrBean(TestBean rBean);
}
