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
import java.io.RandomAccessFile;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import javax.transaction.xa.Xid;

import org.apache.commons.io.FileUtils;
import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.manager.TransactionBranchInfo;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.apache.geronimo.transaction.manager.XidImpl;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LogConversionTest {

    public static Logger LOG = LoggerFactory.getLogger(LogConversionTest.class);
    private static XidFactory xidFactory = new TestXidFactoryImpl("org.apache.aries.transaction.test".getBytes());
    private static File BASE = new File(System.getProperty("user.dir"), "txlogs");

    private static long start = 42L;
    private static long count = start;

    @Test
    public void initialConfiguration() throws Exception {
        File logDir = new File(BASE, "initialConfiguration");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("initialConfiguration", "transaction", 2, -1, 1, properties);

        assertFalse(TransactionLogUtils.copyActiveTransactions(null, properties));
    }

    @Test
    public void initialConfigurationEmptyTransactionLog() throws Exception {
        File logDir = new File(BASE, "initialConfigurationEmptyTransactionLog");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("initialConfigurationEmptyTransactionLog", "transaction", 2, -1, 1, properties);
        new RandomAccessFile(new File(logDir, "transaction_1.log"), "rw").close();
        new RandomAccessFile(new File(logDir, "transaction_2.log"), "rw").close();

        assertFalse(TransactionLogUtils.copyActiveTransactions(null, properties));
    }

    @Test
    public void initialConfigurationExistingTransactionLogNoChanges() throws Exception {
        File logDir = new File(BASE, "initialConfigurationExistingTransactionLogNoChanges");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("initialConfigurationExistingTransactionLogNoChanges", "transaction", 2, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        assertFalse(TransactionLogUtils.copyActiveTransactions(null, properties));
        assertThat("Transaction log should not be touched", new File(logDir, "transaction_1.log").lastModified(), equalTo(lm));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void existingTransactionLogChangedLogDir() throws Exception {
        File logDir = new File(BASE, "existingTransactionLogChangedLogDir");
        File newLogDir = new File(BASE, "existingTransactionLogChangedLogDir-new");
        FileUtils.deleteDirectory(logDir);
        FileUtils.deleteDirectory(newLogDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("existingTransactionLogChangedLogDir", "transaction", 2, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.logFileDir", newLogDir.getAbsolutePath());

        assertFalse(TransactionLogUtils.copyActiveTransactions(properties, newConfig));
        assertThat("Transaction log should not be touched", new File(newLogDir, "transaction_1.log").lastModified(), equalTo(lm));
        assertFalse("Old transaction log should be moved", logDir.exists());
        assertTrue("New transaction log should be created", newLogDir.exists());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void unknownExistingTransactionLogChangedLogDir() throws Exception {
        File logDir = new File(BASE, "unknownExistingTransactionLogChangedLogDir");
        File newLogDir = new File(BASE, "unknownExistingTransactionLogChangedLogDir-new");
        FileUtils.deleteDirectory(logDir);
        FileUtils.deleteDirectory(newLogDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("unknownExistingTransactionLogChangedLogDir", "transaction", 2, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.logFileDir", newLogDir.getAbsolutePath());

        assertFalse(TransactionLogUtils.copyActiveTransactions(null, newConfig));
        assertFalse("Transaction log should not exist", new File(newLogDir, "transaction_1.log").exists());
        assertTrue("Old transaction log should not be removed", logDir.exists());
        assertFalse("New transaction log should not be created (will be created on TX Manager startup)", newLogDir.exists());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changedNumberOfFiles() throws Exception {
        File logDir = new File(BASE, "changedNumberOfFiles");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("changedNumberOfFiles", "transaction", 2, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.maxLogFiles", "20");

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        assertTrue(TransactionLogUtils.copyActiveTransactions(properties, newConfig));
        assertTrue("Transaction log should exist", new File(logDir, "transaction_1.log").exists());
        assertTrue("There should be 20 transaction log files", new File(logDir, "transaction_20.log").exists());
        assertThat("Transaction log should be processed", new File(logDir, "transaction_1.log").lastModified(), not(equalTo(lm)));
    }

    private long earlierLastModified(File file) {
        file.setLastModified(file.lastModified() - 10000L);
        return file.lastModified();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changedMaxBlocksPerFile() throws Exception {
        File logDir = new File(BASE, "changedMaxBlocksPerFile");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("changedMaxBlocksPerFile", "transaction", 3, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.maxBlocksPerFile", "20");

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        assertTrue(TransactionLogUtils.copyActiveTransactions(properties, newConfig));
        assertTrue("Transaction log should exist", new File(logDir, "transaction_1.log").exists());
        assertFalse("There should be 3 transaction log files", new File(logDir, "transaction_4.log").exists());
        assertThat("Transaction log should be processed", new File(logDir, "transaction_1.log").lastModified(), not(equalTo(lm)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changedBlockSize() throws Exception {
        File logDir = new File(BASE, "changedBlockSize");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("changedBlockSize", "transaction", 3, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.bufferSize", "32");

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        assertTrue(TransactionLogUtils.copyActiveTransactions(properties, newConfig));
        assertTrue("Transaction log should exist", new File(logDir, "transaction_1.log").exists());
        assertThat("Transaction log should be processed", new File(logDir, "transaction_1.log").lastModified(), not(equalTo(lm)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changed3ParametersAndLogDir() throws Exception {
        File logDir = new File(BASE, "changed3ParametersAndLogDir");
        File newLogDir = new File(BASE, "changed3ParametersAndLogDir-new");
        FileUtils.deleteDirectory(logDir);
        FileUtils.deleteDirectory(newLogDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("changed3ParametersAndLogDir", "transaction", 3, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.maxLogFiles", "4");
        newConfig.put("aries.transaction.howl.maxBlocksPerFile", "4");
        newConfig.put("aries.transaction.howl.bufferSize", "4");
        newConfig.put("aries.transaction.howl.logFileDir", newLogDir.getAbsolutePath());

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        assertTrue(TransactionLogUtils.copyActiveTransactions(properties, newConfig));
        assertTrue("Old transaction log should exist", new File(logDir, "transaction_1.log").exists());
        assertTrue("New transaction log should exist", new File(newLogDir, "transaction_1.log").exists());
        assertThat("Old transaction log should be touched (HOWL Log opened)", new File(logDir, "transaction_1.log").lastModified(), not(equalTo(lm)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void existingTransactionLogChangedLogFileName() throws Exception {
        File logDir = new File(BASE, "existingTransactionLogChangedLogFileName");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("existingTransactionLogChangedLogFileName", "transaction", 2, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.logFileName", "megatransaction");

        assertTrue(TransactionLogUtils.copyActiveTransactions(properties, newConfig));
        assertFalse("Old transaction log should not exist", new File(logDir, "transaction_1.log").exists());
        assertTrue("New transaction log should exist", new File(logDir, "megatransaction_1.log").exists());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void existingTransactionLogChangedLogFileNameAndLogDir() throws Exception {
        File logDir = new File(BASE, "existingTransactionLogChangedLogFileNameAndLogDir");
        File newLogDir = new File(BASE, "existingTransactionLogChangedLogFileNameAndLogDir-new");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("existingTransactionLogChangedLogFileNameAndLogDir", "transaction", 2, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.logFileName", "megatransaction");
        newConfig.put("aries.transaction.howl.logFileDir", newLogDir.getAbsolutePath());

        assertTrue(TransactionLogUtils.copyActiveTransactions(properties, newConfig));
        assertFalse("Old transaction log should not exist", new File(logDir, "transaction_1.log").exists());
        assertTrue("New transaction log should exist", new File(newLogDir, "megatransaction_1.log").exists());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void existingTransactionLogChangedLogFileNameAndBlockSize() throws Exception {
        File logDir = new File(BASE, "existingTransactionLogChangedLogFileNameAndBlockSize");
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        HOWLLog txLog = createLog("existingTransactionLogChangedLogFileNameAndBlockSize", "transaction", 2, -1, 1, properties);
        txLog.doStart();
        transaction(txLog, 1, false);
        txLog.doStop();

        long lm = earlierLastModified(new File(logDir, "transaction_1.log"));

        Hashtable<String, Object> newConfig = (Hashtable<String, Object>) properties.clone();
        newConfig.put("aries.transaction.howl.logFileName", "megatransaction");
        newConfig.put("aries.transaction.howl.bufferSize", "4");

        assertTrue(TransactionLogUtils.copyActiveTransactions(properties, newConfig));
        assertThat("Old transaction log should be touched (HOWL Log opened)", new File(logDir, "transaction_1.log").lastModified(), not(equalTo(lm)));
        assertTrue("New transaction log should exist", new File(logDir, "megatransaction_1.log").exists());
    }

    private HOWLLog createLog(String logFileDir, String logFileName,
                              int maxLogFiles, int maxBlocksPerFile, int bufferSizeInKB,
                              Dictionary<String, Object> properties) throws Exception {
        properties.put("aries.transaction.recoverable", "true");
        properties.put("aries.transaction.howl.bufferClassName", "org.objectweb.howl.log.BlockLogBuffer");
        properties.put("aries.transaction.howl.checksumEnabled", "true");
        properties.put("aries.transaction.howl.adler32Checksum", "true");
        properties.put("aries.transaction.howl.flushSleepTime", "50");
        properties.put("aries.transaction.howl.logFileExt", "log");
        properties.put("aries.transaction.howl.logFileName", logFileName);
        properties.put("aries.transaction.howl.minBuffers", "1");
        properties.put("aries.transaction.howl.maxBuffers", "0");
        properties.put("aries.transaction.howl.threadsWaitingForceThreshold", "-1");
        properties.put("aries.transaction.flushPartialBuffers", "true");
        String absoluteLogFileDir = new File(BASE, logFileDir).getAbsolutePath() + "/";
        properties.put("aries.transaction.howl.logFileDir", absoluteLogFileDir);
        properties.put("aries.transaction.howl.bufferSize", Integer.toString(bufferSizeInKB));
        properties.put("aries.transaction.howl.maxBlocksPerFile", Integer.toString(maxBlocksPerFile));
        properties.put("aries.transaction.howl.maxLogFiles", Integer.toString(maxLogFiles));

        return new HOWLLog("org.objectweb.howl.log.BlockLogBuffer", bufferSizeInKB,
                true, true, 50,
                absoluteLogFileDir, "log", logFileName,
                maxBlocksPerFile, 0, maxLogFiles, 1, -1, true, xidFactory, null);
    }

    private void transaction(HOWLLog log, int transactionBranchCount, boolean commit) throws Exception {
        Xid xid = xidFactory.createXid();
        List<TransactionBranchInfo> txBranches = new LinkedList<TransactionBranchInfo>();
        for (int b = 1; b <= transactionBranchCount; b++) {
            // TransactionImpl.enlistResource()
            Xid branchXid = xidFactory.createBranch(xid, b);
            txBranches.add(new TestTransactionBranchInfo(branchXid, String.format("res-%02d", b)));
        }

        // org.apache.geronimo.transaction.manager.TransactionImpl.internalPrepare()
        Object logMark = log.prepare(xid, txBranches);
        if (commit) {
            // org.apache.geronimo.transaction.manager.CommitTask.run()
            log.commit(xid, logMark);
        }
    }

    private static class TestTransactionBranchInfo implements TransactionBranchInfo {

        private final Xid xid;
        private final String name;

        public TestTransactionBranchInfo(Xid xid, String name) {
            this.xid = xid;
            this.name = name;
        }

        @Override
        public String getResourceName() {
            return name;
        }

        @Override
        public Xid getBranchXid() {
            return xid;
        }

    }

    private static class TestXidFactoryImpl extends XidFactoryImpl {

        private final byte[] baseId = new byte[Xid.MAXGTRIDSIZE];

        public TestXidFactoryImpl(byte[] tmId) {
            super(tmId);
            System.arraycopy(tmId, 0, baseId, 8, tmId.length);
        }

        @Override
        public Xid createXid() {
            byte[] globalId = baseId.clone();
            long id;
            synchronized (this) {
                id = count++;
            }
            insertLong(id, globalId, 0);
            return new XidImpl(globalId);
        }

        @Override
        public Xid createBranch(Xid globalId, int branch) {
            byte[] branchId = baseId.clone();
            branchId[0] = (byte) branch;
            branchId[1] = (byte) (branch >>> 8);
            branchId[2] = (byte) (branch >>> 16);
            branchId[3] = (byte) (branch >>> 24);
            insertLong(start, branchId, 4);
            return new XidImpl(globalId, branchId);
        }

    }

}
