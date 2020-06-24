/*
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
package org.apache.aries.transaction.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;

import org.apache.aries.transaction.AriesTransactionManager;
import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.log.UnrecoverableLog;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

/**
 */
@SuppressWarnings("rawtypes")
public class TransactionManagerService {

    public static final String TRANSACTION_TIMEOUT = "aries.transaction.timeout";
    public static final String RECOVERABLE = "aries.transaction.recoverable";
    public static final String TMID = "aries.transaction.tmid";
    public static final String HOWL_BUFFER_CLASS_NAME = "aries.transaction.howl.bufferClassName";
    public static final String HOWL_BUFFER_SIZE = "aries.transaction.howl.bufferSize";
    public static final String HOWL_CHECKSUM_ENABLED = "aries.transaction.howl.checksumEnabled";
    public static final String HOWL_ADLER32_CHECKSUM = "aries.transaction.howl.adler32Checksum";
    public static final String HOWL_FLUSH_SLEEP_TIME = "aries.transaction.howl.flushSleepTime";
    public static final String HOWL_LOG_FILE_EXT = "aries.transaction.howl.logFileExt";
    public static final String HOWL_LOG_FILE_NAME = "aries.transaction.howl.logFileName";
    public static final String HOWL_MAX_BLOCKS_PER_FILE = "aries.transaction.howl.maxBlocksPerFile";
    public static final String HOWL_MAX_LOG_FILES = "aries.transaction.howl.maxLogFiles";
    public static final String HOWL_MAX_BUFFERS = "aries.transaction.howl.maxBuffers";
    public static final String HOWL_MIN_BUFFERS = "aries.transaction.howl.minBuffers";
    public static final String HOWL_THREADS_WAITING_FORCE_THRESHOLD = "aries.transaction.howl.threadsWaitingForceThreshold";
    public static final String HOWL_LOG_FILE_DIR = "aries.transaction.howl.logFileDir";
    public static final String HOWL_FLUSH_PARTIAL_BUFFERS = "aries.transaction.flushPartialBuffers";

    public static final int DEFAULT_TRANSACTION_TIMEOUT = 600; // 600 seconds -> 10 minutes
    public static final boolean DEFAULT_RECOVERABLE = false;   // not recoverable by default

    private static final String PLATFORM_TRANSACTION_MANAGER_CLASS = "org.springframework.transaction.PlatformTransactionManager";

    @SuppressWarnings("unused")
    private final String pid;
    private final Dictionary properties;
    private final BundleContext bundleContext;
    private boolean useSpring;
    private AriesTransactionManagerImpl transactionManager;
    private TransactionLog transactionLog;
    private ServiceRegistration<?> serviceRegistration;

    public TransactionManagerService(String pid, Dictionary properties, BundleContext bundleContext) throws ConfigurationException {
        this.pid = pid;
        this.properties = properties;
        this.bundleContext = bundleContext;
        // Transaction timeout
        int transactionTimeout = getInt(this.properties, TRANSACTION_TIMEOUT, DEFAULT_TRANSACTION_TIMEOUT);
        if (transactionTimeout <= 0) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1728
            throw new ConfigurationException(TRANSACTION_TIMEOUT, "The transaction timeout property must be greater than zero.");
        }

        final String tmid = getString(this.properties, TMID, pid);
        // the max length of the factory should be 64
        XidFactory xidFactory = new XidFactoryImpl(tmid.substring(0, Math.min(tmid.length(), 64)).getBytes());
        // Transaction log
        transactionLog = createTransactionLog(this.properties, xidFactory);
        // Create transaction manager
        try {
            try {
                transactionManager = new SpringTransactionManagerCreator().create(transactionTimeout, xidFactory, transactionLog);
                useSpring = true;
            } catch (NoClassDefFoundError e) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1069
                transactionManager = new AriesTransactionManagerImpl(transactionTimeout, xidFactory, transactionLog);
            }
        } catch (XAException e) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1728
            throw new RuntimeException("An exception occurred during transaction recovery.", e);
        }
    }

    public void start() throws Exception {
        List<String> clazzes = new ArrayList<String>();
//IC see: https://issues.apache.org/jira/browse/ARIES-1069
        clazzes.add(AriesTransactionManager.class.getName());
        clazzes.add(TransactionManager.class.getName());
        clazzes.add(TransactionSynchronizationRegistry.class.getName());
        clazzes.add(UserTransaction.class.getName());
        clazzes.add(RecoverableTransactionManager.class.getName());
        if (useSpring) {
            clazzes.add(PLATFORM_TRANSACTION_MANAGER_CLASS);
        }
        String[] ifar = clazzes.toArray(new String[clazzes.size()]);
        serviceRegistration = bundleContext.registerService(ifar, transactionManager, null);
    }

    public void close() throws Exception {
//IC see: https://issues.apache.org/jira/browse/ARIES-1516
        if(serviceRegistration != null) {
          try {
            serviceRegistration.unregister();
          } catch (IllegalStateException e) {
            //This can be safely ignored
          }
        }
      
        if (transactionLog instanceof HOWLLog) {
            ((HOWLLog) transactionLog).doStop();
        }
    }

    static String getString(Dictionary properties, String property, String dflt) {
        String value = (String) properties.get(property);
        if (value != null) {
            return value;
        }
        return dflt;
    }

    static int getInt(Dictionary properties, String property, int dflt) throws ConfigurationException {
        String value = (String) properties.get(property);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1728
                throw new ConfigurationException(property, "The property " + property + " should have an integer value, but the value " + value + " is not an integer.", e);
            }
        }
        return dflt;
    }

    static boolean getBool(Dictionary properties, String property, boolean dflt) throws ConfigurationException {
        String value = (String) properties.get(property);
        if (value != null) {
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1728
                throw new ConfigurationException(property, "The property " + property + " should have a boolean value, but the value " + value + " is not a boolean.", e);
            }
        }
        return dflt;
    }

    static TransactionLog createTransactionLog(Dictionary properties, XidFactory xidFactory) throws ConfigurationException {
        TransactionLog result = null;
        if (getBool(properties, RECOVERABLE, DEFAULT_RECOVERABLE)) {
            String bufferClassName = getString(properties, HOWL_BUFFER_CLASS_NAME, "org.objectweb.howl.log.BlockLogBuffer");
            int bufferSizeKBytes = getInt(properties, HOWL_BUFFER_SIZE, 4);
            if (bufferSizeKBytes < 1 || bufferSizeKBytes > 32) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1728
                throw new ConfigurationException(HOWL_BUFFER_SIZE, "The buffer size must be between one and thirty-two.");
            }
            boolean checksumEnabled = getBool(properties, HOWL_CHECKSUM_ENABLED, true);
            boolean adler32Checksum = getBool(properties, HOWL_ADLER32_CHECKSUM, true);
            int flushSleepTimeMilliseconds = getInt(properties, HOWL_FLUSH_SLEEP_TIME, 50);
            String logFileExt = getString(properties, HOWL_LOG_FILE_EXT, "log");
            String logFileName = getString(properties, HOWL_LOG_FILE_NAME, "transaction");
            int maxBlocksPerFile = getInt(properties, HOWL_MAX_BLOCKS_PER_FILE, -1);
            int maxLogFiles = getInt(properties, HOWL_MAX_LOG_FILES, 2);
            int minBuffers = getInt(properties, HOWL_MIN_BUFFERS, 4);
            if (minBuffers < 0) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1728
                throw new ConfigurationException(HOWL_MIN_BUFFERS, "The minimum number of buffers must be greater than zero.");
            }
            int maxBuffers = getInt(properties, HOWL_MAX_BUFFERS, 0);
            if (maxBuffers > 0 && minBuffers < maxBuffers) {
                throw new ConfigurationException(HOWL_MAX_BUFFERS, "The maximum number of buffers must be greater than the minimum number of buffers.");
            }
            int threadsWaitingForceThreshold = getInt(properties, HOWL_THREADS_WAITING_FORCE_THRESHOLD, -1);
            boolean flushPartialBuffers = getBool(properties, HOWL_FLUSH_PARTIAL_BUFFERS, true);
            String logFileDir = getString(properties, HOWL_LOG_FILE_DIR, null);
            if (logFileDir == null || logFileDir.length() == 0 || !new File(logFileDir).isAbsolute()) {
                throw new ConfigurationException(HOWL_LOG_FILE_DIR, "The log file directory must be set to an absolute directory.");
            }
            try {
                result = new HOWLLog(bufferClassName,
                        bufferSizeKBytes,
                        checksumEnabled,
                        adler32Checksum,
                        flushSleepTimeMilliseconds,
                        logFileDir,
                        logFileExt,
                        logFileName,
                        maxBlocksPerFile,
                        maxBuffers,
                        maxLogFiles,
                        minBuffers,
                        threadsWaitingForceThreshold,
//IC see: https://issues.apache.org/jira/browse/ARIES-881
                        flushPartialBuffers,
//IC see: https://issues.apache.org/jira/browse/ARIES-873
                        xidFactory,
                        null);
                ((HOWLLog) result).doStart();
            } catch (Exception e) {
                // This should not really happen as we've checked properties earlier
                throw new ConfigurationException(null, e.getMessage(), e);
            }
        } else {
            result = new UnrecoverableLog();
        }

        return result;
    }

    /**
     * We use an inner static class to decouple this class from the spring-tx classes
     * in order to not have NoClassDefFoundError if those are not present.
     */
    public static class SpringTransactionManagerCreator {

        public AriesTransactionManagerImpl create(int defaultTransactionTimeoutSeconds, XidFactory xidFactory, TransactionLog transactionLog) throws XAException {
//IC see: https://issues.apache.org/jira/browse/ARIES-1069
            return new AriesPlatformTransactionManager(defaultTransactionTimeoutSeconds, xidFactory, transactionLog);
        }

    }
}
