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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.transaction;

import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

import org.apache.aries.util.nls.MessageUtil;

public class Constants {
    public static final Pattern WILDCARD = Pattern.compile("\\Q.*\\E");
    
    public static final int BANNED_MODIFIERS = Modifier.PRIVATE | Modifier.STATIC;
    public static final MessageUtil MESSAGES = MessageUtil.createMessageUtil(Constants.class, "org.apache.aries.transaction.nls.blueprintTx");
}
