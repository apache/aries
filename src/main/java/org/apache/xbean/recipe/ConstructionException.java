/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.recipe;

/**
 * @version $Rev: 6680 $ $Date: 2005-12-24T04:38:27.427468Z $
 */
public class ConstructionException extends RuntimeException {

    private String className;
    private String attributeName;

    public ConstructionException() {
    }

    public ConstructionException(String message) {
        super(message);
    }

    public ConstructionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstructionException(Throwable cause) {
        super(cause);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setPrependAttributeName(String attributeName) {
    }

    public String getMessage() {
        if (className == null) {
            return super.getMessage();
        }

        if (attributeName == null) {
            return "Unable to create bean of type " + className + ": "  + super.getMessage();
        }

        return "Unable to create bean of type " + className + " for attribute " + attributeName + ": "  + super.getMessage();
    }
}
