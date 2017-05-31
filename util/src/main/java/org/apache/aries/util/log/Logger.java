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
package org.apache.aries.util.log;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

// Note this file originated in the JMX subproject.

/**
 * <p>This <tt>Logger</tt> class represents ServiceTracker for LogService.
 * It provides a simple mechanism for interacting with the log service.
 *
 * @see org.osgi.service.log.LogService
 * @see org.osgi.util.tracker.ServiceTracker
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Logger extends ServiceTracker implements LogService {
    /**
     * Constructs new Logger(ServiceTracker for LogService).
     *
     * @param context bundle context.
     */
    public Logger(BundleContext context) {
        super(context, LogService.class.getName(), null);
    }

    /**
     * @see org.osgi.service.log.LogService#log(int, java.lang.String)
     */
    public void log(int level, String message) {
        LogService logService = (LogService) getService();
        if (logService != null) {
            logService.log(level, message);
        }
    }

    /**
     * @see org.osgi.service.log.LogService#log(int, java.lang.String, java.lang.Throwable)
     */
    public void log(int level, String message, Throwable exception) {
        LogService logService = (LogService) getService();
        if (logService != null) {
            logService.log(level, message, exception);
        }
    }

    /**
     * @see org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference, int, java.lang.String)
     */
    public void log(ServiceReference ref, int level, String message) {
        LogService logService = (LogService) getService();
        if (logService != null) {
            logService.log(ref, level, message);
        }
    }

    /**
     * @see org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference, int, java.lang.String,
     *      java.lang.Throwable)
     */
    public void log(ServiceReference ref, int level, String message, Throwable exception) {
        LogService logService = (LogService) getService();
        if (logService != null) {
            logService.log(ref, level, message, exception);
        }
    }
}
