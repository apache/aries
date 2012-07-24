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
package org.apache.aries.transaction;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;

import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.log.UnrecoverableLog;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

/**
 */
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

    public static final int DEFAULT_TRANSACTION_TIMEOUT = 600; // 600 seconds -> 10 minutes
    public static final boolean DEFAULT_RECOVERABLE = false;   // not recoverable by default

    private static final String PLATFORM_TRANSACTION_MANAGER_CLASS = "org.springframework.transaction.PlatformTransactionManager";

    private final String pid;
    private final Dictionary properties;
    private final BundleContext bundleContext;
    private boolean useSpring;
    private GeronimoTransactionManager transactionManager;
    private TransactionLog transactionLog;
    private ServiceRegistration serviceRegistration;

    public TransactionManagerService(String pid, Dictionary properties, BundleContext bundleContext) throws ConfigurationException {
        this.pid = pid;
        this.properties = properties;
        this.bundleContext = bundleContext;
        // Transaction timeout
        int transactionTimeout = getInt(TRANSACTION_TIMEOUT, DEFAULT_TRANSACTION_TIMEOUT);
        if (transactionTimeout <= 0) {
            throw new ConfigurationException(TRANSACTION_TIMEOUT, NLS.MESSAGES.getMessage("tx.timeout.greaterthan.zero"));
        }

        final String tmid = getString(TMID, pid);
        // the max length of the factory should be 64
        XidFactory xidFactory = new XidFactoryImpl(tmid.substring(0, Math.min(tmid.length(), 64)).getBytes());
        // Transaction log
        if (getBool(RECOVERABLE, DEFAULT_RECOVERABLE)) {
            String bufferClassName = getString(HOWL_BUFFER_CLASS_NAME, "org.objectweb.howl.log.BlockLogBuffer");
            int bufferSizeKBytes = getInt(HOWL_BUFFER_SIZE, 32);
            if (bufferSizeKBytes < 1 || bufferSizeKBytes > 32) {
                throw new ConfigurationException(HOWL_BUFFER_SIZE, NLS.MESSAGES.getMessage("buffer.size.between.one.and.thirtytwo"));
            }
            boolean checksumEnabled = getBool(HOWL_CHECKSUM_ENABLED, true);
            boolean adler32Checksum = getBool(HOWL_ADLER32_CHECKSUM, true);
            int flushSleepTimeMilliseconds = getInt(HOWL_FLUSH_SLEEP_TIME, 50);
            String logFileExt = getString(HOWL_LOG_FILE_EXT, "log");
            String logFileName = getString(HOWL_LOG_FILE_NAME, "transaction");
            int maxBlocksPerFile = getInt(HOWL_MAX_BLOCKS_PER_FILE, -1);
            int maxLogFiles = getInt(HOWL_MAX_LOG_FILES, 2);
            int minBuffers = getInt(HOWL_MIN_BUFFERS, 4);
            if (minBuffers < 0) {
                throw new ConfigurationException(HOWL_MIN_BUFFERS, NLS.MESSAGES.getMessage("min.buffers.greaterthan.zero"));
            }
            int maxBuffers = getInt(HOWL_MAX_BUFFERS, 0);
            if (maxBuffers > 0 && minBuffers < maxBuffers) {
                throw new ConfigurationException(HOWL_MAX_BUFFERS, NLS.MESSAGES.getMessage("max.buffers.greaterthan.min.buffers"));
            }
            int threadsWaitingForceThreshold = getInt(HOWL_THREADS_WAITING_FORCE_THRESHOLD, -1);
            String logFileDir = getString(HOWL_LOG_FILE_DIR, null);
            if (logFileDir == null || logFileDir.length() == 0 || !new File(logFileDir).isAbsolute()) {
                throw new ConfigurationException(HOWL_LOG_FILE_DIR, NLS.MESSAGES.getMessage("log.file.dir"));
            }
            try {
                transactionLog = new HOWLLog(bufferClassName,
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
                                             xidFactory,
                                             null);
                ((HOWLLog) transactionLog).doStart();
            } catch (Exception e) {
                // This should not really happen as we've checked properties earlier
                throw new ConfigurationException(null, null, e);
            }
        } else {
            transactionLog =  new UnrecoverableLog();
        }
        // Create transaction manager
        try {
            try {
                transactionManager = new SpringTransactionManagerCreator().create(transactionTimeout, xidFactory, transactionLog);
                useSpring = true;
            } catch (NoClassDefFoundError e) {
                transactionManager = new GeronimoTransactionManager(transactionTimeout, xidFactory, transactionLog);
            }
        } catch (XAException e) {
            throw new RuntimeException(NLS.MESSAGES.getMessage("tx.recovery.error"), e);
        }
    }

    public void start() throws Exception {
        List<String> clazzes = new ArrayList<String>();
        clazzes.add(TransactionManager.class.getName());
        clazzes.add(TransactionSynchronizationRegistry.class.getName());
        clazzes.add(UserTransaction.class.getName());
        clazzes.add(RecoverableTransactionManager.class.getName());
        if (useSpring) {
            clazzes.add(PLATFORM_TRANSACTION_MANAGER_CLASS);
        }
        serviceRegistration = bundleContext.registerService(clazzes.toArray(new String[clazzes.size()]), transactionManager, new Properties());
    }

    public void close() throws Exception {
        AriesFrameworkUtil.safeUnregisterService(serviceRegistration);
      
        if (transactionLog instanceof HOWLLog) {
            ((HOWLLog) transactionLog).doStop();
        }
    }

    private String getString(String property, String dflt) throws ConfigurationException {
        String value = (String) properties.get(property);
        if (value != null) {
            return value;
        }
        return dflt;
    }

    private int getInt(String property, int dflt) throws ConfigurationException {
        String value = (String) properties.get(property);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                throw new ConfigurationException(property, NLS.MESSAGES.getMessage("prop.value.not.int", property, value), e);
            }
        }
        return dflt;
    }

    private boolean getBool(String property, boolean dflt) throws ConfigurationException {
        String value = (String) properties.get(property);
        if (value != null) {
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
                throw new ConfigurationException(property, NLS.MESSAGES.getMessage("prop.value.not.boolean", property, value), e);
            }
        }
        return dflt;
    }

    /**
     * We use an inner static class to decouple this class from the spring-tx classes
     * in order to not have NoClassDefFoundError if those are not present.
     */
    public static class SpringTransactionManagerCreator {

        public GeronimoTransactionManager create(int defaultTransactionTimeoutSeconds, XidFactory xidFactory, TransactionLog transactionLog) throws XAException {
            return new GeronimoPlatformTransactionManager(defaultTransactionTimeoutSeconds, xidFactory, transactionLog);
        }

    }
}
