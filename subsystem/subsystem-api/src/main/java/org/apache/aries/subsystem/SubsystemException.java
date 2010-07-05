/*
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
package org.apache.aries.subsystem;

/**
 * Exception thrown by SubsystemAdmin or Subsystem when
 * a problem occurs.
 */
public class SubsystemException extends RuntimeException {

    /**
     * Construct a subsystem exception which no message.
     */
    public SubsystemException() {
    }

    /**
     * Construct a subsystem exception specifying a message.
     * 
     * @param message The message to include in the exception.
     */
    public SubsystemException(String message) {
        super(message);
    }

    /**
     * Construct a subsystem exception, wrapping an existing exception.
     * 
     * @param message The message to be included in the exception
     * @param cause The cause of the exception.
     */
    public SubsystemException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a subsystem exception, wrapping an existing exception.
     * 
     * @param cause The cause of the exception.
     */
    public SubsystemException(Throwable cause) {
        super(cause);
    }

}
