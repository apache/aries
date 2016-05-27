/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.transaction.control.recovery;

import javax.transaction.xa.XAResource;

/**
 * This service interface is published by Transaction control services that are
 * able to support recovery. Any recoverable resources should register
 * themselves with all available recovery services as they are created.
 */
public interface TransactionRecovery {

	/**
	 * Allow the {@link TransactionRecovery} service to attempt to recover any
	 * incomplete XA transactions. Any recovery failures that occur must be
	 * logged and not thrown to the caller of this service.
	 * 
	 * @param resource
	 */
	public void recover(XAResource resource);

}
