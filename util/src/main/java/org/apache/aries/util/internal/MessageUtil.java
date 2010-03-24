/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.util.internal;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class MessageUtil {
    /** The resource bundle for blueprint messages */
    private final static ResourceBundle messages = 
        ResourceBundle.getBundle("org.apache.aries.util.messages.UTILmessages");

    /**
     * Resolve a message from the bundle, including any necessary formatting.
     * 
     * @param key
     *            the message key.
     * @param inserts
     *            any required message inserts.
     * @return the message translated into the server local.
     */
    public static final String getMessage(String key, Object... inserts) {
        String msg = messages.getString(key);

        if (inserts.length > 0)
            msg = MessageFormat.format(msg, inserts);

        return msg;
    }
}
