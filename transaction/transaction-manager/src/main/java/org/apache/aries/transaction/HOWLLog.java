/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.aries.transaction;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.transaction.xa.Xid;

import org.apache.geronimo.transaction.manager.LogException;
import org.apache.geronimo.transaction.manager.Recovery;
import org.apache.geronimo.transaction.manager.TransactionBranchInfo;
import org.apache.geronimo.transaction.manager.TransactionBranchInfoImpl;
import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.objectweb.howl.log.Configuration;
import org.objectweb.howl.log.LogClosedException;
import org.objectweb.howl.log.LogConfigurationException;
import org.objectweb.howl.log.LogFileOverflowException;
import org.objectweb.howl.log.LogRecord;
import org.objectweb.howl.log.LogRecordSizeException;
import org.objectweb.howl.log.LogRecordType;
import org.objectweb.howl.log.ReplayListener;
import org.objectweb.howl.log.xa.XACommittingTx;
import org.objectweb.howl.log.xa.XALogRecord;
import org.objectweb.howl.log.xa.XALogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev: 912058 $ $Date: 2010-02-20 09:34:14 +0800 (Sat, 20 Feb 2010) $
 */
public class HOWLLog implements TransactionLog {
//    static final byte PREPARE = 1;
    //these are used as debugging aids only
    private static final byte COMMIT = 2;
    private static final byte ROLLBACK = 3;

    static final String[] TYPE_NAMES = {null, "PREPARE", "COMMIT", "ROLLBACK"};

    private static final Logger log = LoggerFactory.getLogger(HOWLLog.class);

    private File serverBaseDir;
    private String logFileDir;

    private final XidFactory xidFactory;

    private final XALogger logger;
    private final Configuration configuration = new Configuration();
    private boolean started = false;
    private HashMap<Xid, Recovery.XidBranchesPair> recovered;

    public HOWLLog(String bufferClassName,
                   int bufferSize,
                   boolean checksumEnabled,
                   boolean adler32Checksum,
                   int flushSleepTimeMilliseconds,
                   String logFileDir,
                   String logFileExt,
                   String logFileName,
                   int maxBlocksPerFile,
                   int maxBuffers,
                   int maxLogFiles,
                   int minBuffers,
                   int threadsWaitingForceThreshold,
                   boolean flushPartialBuffers,
                   XidFactory xidFactory,
                   File serverBaseDir) throws IOException, LogConfigurationException {
        this.serverBaseDir = serverBaseDir;
        setBufferClassName(bufferClassName);
        setBufferSizeKBytes(bufferSize);
        setChecksumEnabled(checksumEnabled);
        setAdler32Checksum(adler32Checksum);
        setFlushSleepTimeMilliseconds(flushSleepTimeMilliseconds);
        //setLogFileDir(logFileDir);
        this.logFileDir = logFileDir;
        setLogFileExt(logFileExt);
        setLogFileName(logFileName);
        setMaxBlocksPerFile(maxBlocksPerFile);
        setMaxBuffers(maxBuffers);
        setMaxLogFiles(maxLogFiles);
        setMinBuffers(minBuffers);
        setThreadsWaitingForceThreshold(threadsWaitingForceThreshold);
        setFlushPartialBuffers(flushPartialBuffers);
        this.xidFactory = xidFactory;
        this.logger = new XALogger(configuration);
    }

    public String getLogFileDir() {
        return logFileDir;
    }

    public void setLogFileDir(String logDirName) {
        File logDir = new File(logDirName);
        if (!logDir.isAbsolute()) {
            logDir = new File(serverBaseDir, logDirName);
        }

        this.logFileDir = logDirName;
        if (started) {
            configuration.setLogFileDir(logDir.getAbsolutePath());
        }
    }

    public String getLogFileExt() {
        return configuration.getLogFileExt();
    }

    public void setLogFileExt(String logFileExt) {
        configuration.setLogFileExt(logFileExt);
    }

    public String getLogFileName() {
        return configuration.getLogFileName();
    }

    public void setLogFileName(String logFileName) {
        configuration.setLogFileName(logFileName);
    }

    public boolean isChecksumEnabled() {
        return configuration.isChecksumEnabled();
    }

    public void setChecksumEnabled(boolean checksumOption) {
        configuration.setChecksumEnabled(checksumOption);
    }

    public boolean isAdler32ChecksumEnabled() {
        return configuration.isAdler32ChecksumEnabled();
    }

    public void setAdler32Checksum(boolean checksumOption) {
        configuration.setAdler32Checksum(checksumOption);
    }

    public int getBufferSizeKBytes() {
        return configuration.getBufferSize();
    }

    public void setBufferSizeKBytes(int bufferSize) throws LogConfigurationException {
        configuration.setBufferSize(bufferSize);
    }

    public String getBufferClassName() {
        return configuration.getBufferClassName();
    }

    public void setBufferClassName(String bufferClassName) {
        configuration.setBufferClassName(bufferClassName);
    }

    public int getMaxBuffers() {
        return configuration.getMaxBuffers();
    }

    public void setMaxBuffers(int maxBuffers) throws LogConfigurationException {
        configuration.setMaxBuffers(maxBuffers);
    }

    public int getMinBuffers() {
        return configuration.getMinBuffers();
    }

    public void setMinBuffers(int minBuffers) throws LogConfigurationException {
        configuration.setMinBuffers(minBuffers);
    }

    public int getFlushSleepTimeMilliseconds() {
        return configuration.getFlushSleepTime();
    }

    public void setFlushSleepTimeMilliseconds(int flushSleepTime) {
        configuration.setFlushSleepTime(flushSleepTime);
    }

    public int getThreadsWaitingForceThreshold() {
        return configuration.getThreadsWaitingForceThreshold();
    }

    public void setThreadsWaitingForceThreshold(int threadsWaitingForceThreshold) {
        configuration.setThreadsWaitingForceThreshold(threadsWaitingForceThreshold == -1 ? Integer.MAX_VALUE : threadsWaitingForceThreshold);
    }

    public int getMaxBlocksPerFile() {
        return configuration.getMaxBlocksPerFile();
    }

    public void setMaxBlocksPerFile(int maxBlocksPerFile) {
        configuration.setMaxBlocksPerFile(maxBlocksPerFile == -1 ? Integer.MAX_VALUE : maxBlocksPerFile);
    }

    public int getMaxLogFiles() {
        return configuration.getMaxLogFiles();
    }

    public void setMaxLogFiles(int maxLogFiles) {
        configuration.setMaxLogFiles(maxLogFiles);
    }

    public boolean isFlushPartialBuffers() {
        return configuration.isFlushPartialBuffers();
    }

    public void setFlushPartialBuffers(boolean flushPartialBuffers) {
        configuration.setFlushPartialBuffers(flushPartialBuffers);
    }

    public void doStart() throws Exception {
        started = true;
        setLogFileDir(logFileDir);
        log.debug("Initiating transaction manager recovery");
        recovered = new HashMap<Xid, Recovery.XidBranchesPair>();

        logger.open(null);

        ReplayListener replayListener = new GeronimoReplayListener(xidFactory, recovered);
        logger.replayActiveTx(replayListener);

        log.debug("In doubt transactions recovered from log");
    }

    public void doStop() throws Exception {
        started = false;
        logger.close();
        recovered = null;
    }

    public void doFail() {
    }

    public void begin(Xid xid) throws LogException {
    }

    public Object prepare(Xid xid, List<? extends TransactionBranchInfo> branches) throws LogException {
        int branchCount = branches.size();
        byte[][] data = new byte[3 + 2 * branchCount][];
        data[0] = intToBytes(xid.getFormatId());
        data[1] = xid.getGlobalTransactionId();
        data[2] = xid.getBranchQualifier();
        int i = 3;
        for (TransactionBranchInfo transactionBranchInfo : branches) {
            data[i++] = transactionBranchInfo.getBranchXid().getBranchQualifier();
            data[i++] = transactionBranchInfo.getResourceName().getBytes();
        }
        try {
            XACommittingTx committingTx = logger.putCommit(data);
            return committingTx;
        } catch (LogClosedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (LogRecordSizeException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (LogFileOverflowException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (InterruptedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    public void commit(Xid xid, Object logMark) throws LogException {
        //the data is theoretically unnecessary but is included to help with debugging and because HOWL currently requires it.
        byte[][] data = new byte[4][];
        data[0] = new byte[]{COMMIT};
        data[1] = intToBytes(xid.getFormatId());
        data[2] = xid.getGlobalTransactionId();
        data[3] = xid.getBranchQualifier();
        try {
            logger.putDone(data, (XACommittingTx) logMark);
//            logger.putDone(null, (XACommittingTx) logMark);
        } catch (LogClosedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (LogRecordSizeException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (LogFileOverflowException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (InterruptedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    public void rollback(Xid xid, Object logMark) throws LogException {
        //the data is theoretically unnecessary but is included to help with debugging and because HOWL currently requires it.
        byte[][] data = new byte[4][];
        data[0] = new byte[]{ROLLBACK};
        data[1] = intToBytes(xid.getFormatId());
        data[2] = xid.getGlobalTransactionId();
        data[3] = xid.getBranchQualifier();
        try {
            logger.putDone(data, (XACommittingTx) logMark);
//            logger.putDone(null, (XACommittingTx) logMark);
        } catch (LogClosedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (LogRecordSizeException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (LogFileOverflowException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (InterruptedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    public Collection<Recovery.XidBranchesPair> recover(XidFactory xidFactory) throws LogException {
        log.debug("Initiating transaction manager recovery");
        Map<Xid, Recovery.XidBranchesPair> recovered = new HashMap<Xid, Recovery.XidBranchesPair>();
        ReplayListener replayListener = new GeronimoReplayListener(xidFactory, recovered);
        logger.replayActiveTx(replayListener);
        log.debug("In doubt transactions recovered from log");
        return recovered.values();
    }

    public String getXMLStats() {
        return logger.getStats();
    }

    public int getAverageForceTime() {
        return 0;//logger.getAverageForceTime();
    }

    public int getAverageBytesPerForce() {
        return 0;//logger.getAverageBytesPerForce();
    }

    private byte[] intToBytes(int formatId) {
        byte[] buffer = new byte[4];
        buffer[0] = (byte) (formatId >> 24);
        buffer[1] = (byte) (formatId >> 16);
        buffer[2] = (byte) (formatId >> 8);
        buffer[3] = (byte) (formatId >> 0);
        return buffer;
    }

    private int bytesToInt(byte[] buffer) {
        return ((int) buffer[0]) << 24 + ((int) buffer[1]) << 16 + ((int) buffer[2]) << 8 + ((int) buffer[3]) << 0;
    }

    private class GeronimoReplayListener implements ReplayListener {

        private final XidFactory xidFactory;
        private final Map<Xid, Recovery.XidBranchesPair> recoveredTx;

        public GeronimoReplayListener(XidFactory xidFactory, Map<Xid, Recovery.XidBranchesPair> recoveredTx) {
            this.xidFactory = xidFactory;
            this.recoveredTx = recoveredTx;
        }

        public void onRecord(LogRecord plainlr) {
            XALogRecord lr = (XALogRecord) plainlr;
            short recordType = lr.type;
            XACommittingTx tx = lr.getTx();
            if (recordType == LogRecordType.XACOMMIT) {

                byte[][] data = tx.getRecord();

                assert data[0].length == 4;
                int formatId = bytesToInt(data[1]);
                byte[] globalId = data[1];
                byte[] branchId = data[2];
                Xid masterXid = xidFactory.recover(formatId, globalId, branchId);

                Recovery.XidBranchesPair xidBranchesPair = new Recovery.XidBranchesPair(masterXid, tx);
                recoveredTx.put(masterXid, xidBranchesPair);
                log.debug("recovered prepare record for master xid: " + masterXid);
                for (int i = 3; i < data.length; i += 2) {
                    byte[] branchBranchId = data[i];
                    String name = new String(data[i + 1]);

                    Xid branchXid = xidFactory.recover(formatId, globalId, branchBranchId);
                    TransactionBranchInfoImpl branchInfo = new TransactionBranchInfoImpl(branchXid, name);
                    xidBranchesPair.addBranch(branchInfo);
                    log.debug("recovered branch for resource manager, branchId " + name + ", " + branchXid);
                }
            } else {
                if(recordType != LogRecordType.END_OF_LOG) { // This value crops up every time the server is started
                    log.warn("Received unexpected log record: " + lr +" ("+recordType+")");
                }
            }
        }

        public void onError(org.objectweb.howl.log.LogException exception) {
            log.error("Error during recovery: ", exception);
        }

        public LogRecord getLogRecord() {
            //TODO justify this size estimate
            return new LogRecord(10 * 2 * Xid.MAXBQUALSIZE);
        }

    }
}
