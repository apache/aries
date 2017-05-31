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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.manager.Recovery;
import org.apache.geronimo.transaction.manager.TransactionBranchInfo;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.objectweb.howl.log.LogRecordType;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.aries.transaction.internal.TransactionManagerService.*;

public class TransactionLogUtils {

    public static Logger log = LoggerFactory.getLogger(TransactionLogUtils.class);
    private static Pattern TX_FILE_NAME = Pattern.compile("(.*)_([0-9]+)\\.([^.]+)");

    /**
     * <p>When <code>org.apache.aries.transaction</code> PID changes, there may be a need to copy
     * entries from transaction log when some important configuration changed (like block size)</p>
     * @param oldConfiguration previous configuration when configuration changed, may be <code>null</code> when starting bundle
     * @param newConfiguration configuration to create new transaction manager
     * @return <code>true</code> if there was conversion performed
     */
    public static boolean copyActiveTransactions(Dictionary<String, Object> oldConfiguration, Dictionary<String, ?> newConfiguration)
            throws ConfigurationException, IOException {
        boolean initialConfiguration = false;
        if (oldConfiguration == null) {
            oldConfiguration = new Hashtable<String, Object>();
            // initialConfiguration means we don't know the location of "old" logs (if there are any) and
            // assume there may be logs in newLogDirectory
            initialConfiguration = true;
        }
        if (oldConfiguration.get(HOWL_LOG_FILE_DIR) == null) {
            // we will be adjusting oldConfiguration to be able to create "old HOWLLog"
            oldConfiguration.put(HOWL_LOG_FILE_DIR, newConfiguration.get(HOWL_LOG_FILE_DIR));
        }
        String oldLogDirectory = (String) oldConfiguration.get(HOWL_LOG_FILE_DIR);
        String newLogDirectory = (String) newConfiguration.get(HOWL_LOG_FILE_DIR);

        if (newLogDirectory == null || oldLogDirectory == null) {
            // handle with exceptions at TM creation time
            return false;
        }

        File oldDir = new File(oldLogDirectory);
        oldLogDirectory = oldDir.getAbsolutePath();
        File newDir = new File(newLogDirectory);
        newLogDirectory = newDir.getAbsolutePath();

        // a file which may tell us what's the previous configuation
        File transaction_1 = null;

        if (!oldDir.equals(newDir)) {
            // recent logs are in oldDir, so even if newDir contains some logs, we will remove them
            deleteDirectory(newDir);
            transaction_1 = new File(oldDir, configuredTransactionLogName(oldConfiguration, 1));
        } else {
            // we may need to move oldDir to some temporary location, if the configuration is changed
            // we'll then have to copy old tx log to new one
            transaction_1 = new File(oldDir, configuredTransactionLogName(oldConfiguration, 1));
            if (!transaction_1.exists() || transaction_1.length() == 0L) {
                oldConfiguration.put(HOWL_LOG_FILE_NAME, getString(newConfiguration, HOWL_LOG_FILE_NAME, "transaction"));
                oldConfiguration.put(HOWL_LOG_FILE_EXT, getString(newConfiguration, HOWL_LOG_FILE_EXT, "log"));
                transaction_1 = new File(oldDir, configuredTransactionLogName(newConfiguration, 1));
            }
        }

        if (!transaction_1.exists() || transaction_1.length() == 0L) {
            // no need to copy anything
            return false;
        }

        BaseTxLogConfig oldTxConfig = transactionLogFileConfig(transaction_1);
        BaseTxLogConfig newTxConfig = transactionLogFileConfig(newConfiguration);

        if (oldTxConfig == null || oldTxConfig.equals(newTxConfig)) {
            // old log files compatible, but maybe we have to copy them
            if (!oldDir.equals(newDir)) {
                if (!oldDir.renameTo(newDir)) {
                    log.warn(NLS.MESSAGES.getMessage("tx.log.problem.renaming", oldDir.getAbsolutePath()));
                    return false;
                }
            }
            // files are compatible - we'll check one more thing - name_N.extension
            String oldName = configuredTransactionLogName(oldConfiguration, 1);
            String newName = configuredTransactionLogName(newConfiguration, 1);
            if (!oldName.equals(newName)) {
                final Dictionary<String, Object> finalOldConfiguration = oldConfiguration;
                final Dictionary<String, ?> finalNewConfiguration = newConfiguration;
                final Map<String, String> changes = new HashMap<String, String>();
                newDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        Matcher matcher = TX_FILE_NAME.matcher(name);
                        if (matcher.matches()) {
                            if (matcher.group(1).equals(getString(finalOldConfiguration, HOWL_LOG_FILE_NAME, "transaction"))
                                    && matcher.group(3).equals(getString(finalOldConfiguration, HOWL_LOG_FILE_EXT, "log"))) {
                                changes.put(name, String.format("%s_%d.%s",
                                        getString(finalNewConfiguration, HOWL_LOG_FILE_NAME, "transaction"),
                                        Integer.parseInt(matcher.group(2)),
                                        getString(finalNewConfiguration, HOWL_LOG_FILE_EXT, "log")));
                            }
                        }
                        return false;
                    }
                });

                for (String old : changes.keySet()) {
                    new File(newDir, old).renameTo(new File(newDir, changes.get(old)));
                }

                return true;
            }
            return false;
        }

        File backupDir = null;
        if (oldDir.equals(newDir)) {
            // move old dir to backup dir
            backupDir = new File(newLogDirectory + String.format("-%016x", System.currentTimeMillis()));
            if (!oldDir.renameTo(backupDir)) {
                log.warn(NLS.MESSAGES.getMessage("tx.log.problem.renaming", oldDir.getAbsolutePath()));
                return false;
            }
            oldConfiguration = copy(oldConfiguration);
            oldConfiguration.put(HOWL_LOG_FILE_DIR, backupDir.getAbsolutePath());
        }

        log.info(NLS.MESSAGES.getMessage("tx.log.conversion", oldDir.getAbsolutePath(), newDir.getAbsolutePath()));

        oldConfiguration.put(RECOVERABLE, newConfiguration.get(RECOVERABLE));
        oldConfiguration.put(HOWL_MAX_LOG_FILES, Integer.toString(oldTxConfig.maxLogFiles));
        oldConfiguration.put(HOWL_MAX_BLOCKS_PER_FILE, Integer.toString(oldTxConfig.maxBlocksPerFile));
        oldConfiguration.put(HOWL_BUFFER_SIZE, Integer.toString(oldTxConfig.bufferSizeKBytes));

        String tmid1 = TransactionManagerService.getString(oldConfiguration, TMID, Activator.PID);
        XidFactory xidFactory1 = new XidFactoryImpl(tmid1.substring(0, Math.min(tmid1.length(), 64)).getBytes());
        String tmid2 = TransactionManagerService.getString(newConfiguration, TMID, Activator.PID);
        XidFactory xidFactory2 = new XidFactoryImpl(tmid2.substring(0, Math.min(tmid2.length(), 64)).getBytes());

        org.apache.geronimo.transaction.manager.TransactionLog oldLog = null;
        org.apache.geronimo.transaction.manager.TransactionLog newLog = null;
        try {
            oldLog = TransactionManagerService.createTransactionLog(oldConfiguration, xidFactory1);
            newLog = TransactionManagerService.createTransactionLog(newConfiguration, xidFactory2);

            if (!(oldLog instanceof HOWLLog)) {
                log.info(NLS.MESSAGES.getMessage("tx.log.notrecoverable", oldLogDirectory));
                return false;
            }
            if (!(newLog instanceof HOWLLog)) {
                log.info(NLS.MESSAGES.getMessage("tx.log.notrecoverable", newLogDirectory));
                return false;
            }

            HOWLLog from = (HOWLLog) oldLog;
            HOWLLog to = (HOWLLog) newLog;

            Collection<Recovery.XidBranchesPair> pairs = from.recover(xidFactory1);
            for (Recovery.XidBranchesPair xidBranchesPair : pairs) {
                log.info(NLS.MESSAGES.getMessage("tx.log.migrate.xid", xidBranchesPair.getXid()));
                for (TransactionBranchInfo branchInfo : xidBranchesPair.getBranches()) {
                    log.info(NLS.MESSAGES.getMessage("tx.log.migrate.xid.branch", branchInfo.getBranchXid(), branchInfo.getResourceName()));
                }
                to.prepare(xidBranchesPair.getXid(), new ArrayList<TransactionBranchInfo>(xidBranchesPair.getBranches()));
            }
            log.info(NLS.MESSAGES.getMessage("tx.log.migrate.complete"));
            deleteDirectory(backupDir);

            return !pairs.isEmpty();
        } catch (Exception e) {
            log.error(NLS.MESSAGES.getMessage("exception.tx.log.migration"), e);
            if (backupDir != null) {
                deleteDirectory(newDir);
                backupDir.renameTo(oldDir);
            }
            return false;
        } finally {
            try {
                if (oldLog instanceof HOWLLog) {
                    ((HOWLLog)oldLog).doStop();
                }
                if (newLog instanceof HOWLLog) {
                    ((HOWLLog)newLog).doStop();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Retrieves 3 important configuration parameters from single HOWL transaction log file
     * @param txFile existing HOWL file
     * @return
     */
    private static BaseTxLogConfig transactionLogFileConfig(File txFile) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(txFile, "r");
        FileChannel channel = raf.getChannel();
        try {
            ByteBuffer bb = ByteBuffer.wrap(new byte[1024]);
            int read = channel.read(bb);
            if (read < 0x47) { // enough data to have HOWL block header and FILE_HEADER record
                return null;
            }

            bb.rewind();
            if (bb.getInt() != 0x484f574c) { // HOWL
                return null;
            }
            bb.getInt(); // BSN
            int bufferSizeKBytes = bb.getInt() / 1024;
            bb.getInt(); // size
            bb.getInt(); // checksum
            bb.getLong(); // timestamp
            bb.getShort(); // 0x0d0a
            if (bb.getShort() != LogRecordType.FILE_HEADER) {
                return null;
            }
            bb.getShort(); // size
            bb.getShort(); // size
            bb.get(); // automark
            bb.getLong(); // active mark
            bb.getLong(); // log key
            bb.getLong(); // timestamp
            int maxLogFiles = bb.getInt();
            int maxBlocksPerFile = bb.getInt();
            if (maxBlocksPerFile == Integer.MAX_VALUE) {
                maxBlocksPerFile = -1;
            }
            bb.getShort(); // 0x0d0a

            return new BaseTxLogConfig(maxLogFiles, maxBlocksPerFile, bufferSizeKBytes);
        } finally {
            channel.close();
            raf.close();
        }
    }

    /**
     * Retrieves 3 important configuration parameters from configuration
     * @param configuration
     * @return
     */
    private static BaseTxLogConfig transactionLogFileConfig(Dictionary<String, ?> configuration) throws ConfigurationException {
        BaseTxLogConfig result = new BaseTxLogConfig();
        result.maxLogFiles = getInt(configuration, HOWL_MAX_LOG_FILES, 2);
        result.maxBlocksPerFile = getInt(configuration, HOWL_MAX_BLOCKS_PER_FILE, -1);
        result.bufferSizeKBytes = getInt(configuration, HOWL_BUFFER_SIZE, 4);
        return result;
    }

    private static String configuredTransactionLogName(Dictionary<String, ?> configuration, int number) throws ConfigurationException {
        String logFileName = getString(configuration, HOWL_LOG_FILE_NAME, "transaction");
        String logFileExt = getString(configuration, HOWL_LOG_FILE_EXT, "log");
        return String.format("%s_%d.%s", logFileName, number, logFileExt);
    }

    private static Dictionary<String, Object> copy(Dictionary<String, Object> configuration) {
        Dictionary<String, Object> result = new Hashtable<String, Object>();
        for (Enumeration<String> keys = configuration.keys(); keys.hasMoreElements(); ) {
            String k = keys.nextElement();
            result.put(k, configuration.get(k));
        }
        return result;
    }

    /**
     * Recursively delete directory with content
     * @param file
     */
    private static boolean deleteDirectory(File file) {
        if (file == null) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
            return file.delete();
        } else {
            if (!file.delete()) {
                return false;
            }
            return true;
        }
    }

    private static class BaseTxLogConfig {

        public int maxLogFiles;
        public int maxBlocksPerFile;
        public int bufferSizeKBytes;

        public BaseTxLogConfig() {
        }

        public BaseTxLogConfig(int maxLogFiles, int maxBlocksPerFile, int bufferSizeKBytes) {
            this.maxLogFiles = maxLogFiles;
            this.maxBlocksPerFile = maxBlocksPerFile;
            this.bufferSizeKBytes = bufferSizeKBytes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BaseTxLogConfig that = (BaseTxLogConfig) o;

            if (maxLogFiles != that.maxLogFiles) return false;
            if (maxBlocksPerFile != that.maxBlocksPerFile) return false;
            if (bufferSizeKBytes != that.bufferSizeKBytes) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = maxLogFiles;
            result = 31 * result + maxBlocksPerFile;
            result = 31 * result + bufferSizeKBytes;
            return result;
        }

    }

}
