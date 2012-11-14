/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.jmx.blueprint;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.SimpleType;

/**
 * Constants.
 *
 * Additionally, this class contains a number of utility types that are used in
 * different places in the specification. These are {@link #LONG_ARRAY_TYPE},
 * {@link #STRING_ARRAY_TYPE}, and {@link #PRIMITIVE_BYTE_ARRAY_TYPE}.
 *
 * @Immutable
 */
public class JmxConstants {

    /*
     * Empty constructor to make sure this is not used as an object.
     */
    private JmxConstants() {
        // empty
    }


    public static final ArrayType<Byte>    BYTE_ARRAY_TYPE   = Item
                                                                    .arrayType(
                                                                            1,
                                                                            SimpleType.BYTE);

    /**
     * The MBean Open type for an array of strings
     */
    public static final ArrayType<String>    STRING_ARRAY_TYPE   = Item
                                                                    .arrayType(
                                                                            1,
                                                                            SimpleType.STRING);


    /**
     * The domain name of the Blueprint MBeans
     */
    public static final String          ARIES_BLUEPRINT  = "org.apache.aries.blueprint";
}
