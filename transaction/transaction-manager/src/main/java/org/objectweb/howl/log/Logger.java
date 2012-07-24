/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * All rights reserved.
 * 
 * Contact: howl@objectweb.org
 * 
 * This software is licensed under the BSD license.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *     
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * ------------------------------------------------------------------------------
 * $Id: Logger.java,v 1.14 2006/04/21 15:03:36 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

import java.io.IOException;

/**
 * Manage a configured set of two or more physical log files.
 * 
 * <p>Log files have a configured maximum size.  When a file has
 * reached the configured capacity, Logger switches to
 * the next available alternate file.  Normally, log files are created
 * in advance to guarantee that space is available during execution.
 * 
 * <p>Each log file has a file header containing information 
 * allowing Logger to reposition and replay the logs
 * during a recovery scenario.
 * 
 * <p>LogFile <i> marking </i>
 * <p>The LogFile's mark is the the position within the file
 * of the oldest active entry.
 * Initially the mark is set at the beginning of the file.
 * At some configured interval, the caller invokes <i> mark() </i>
 * with the key of the oldest active entry in the log.
 * 
 * <p>For XA the key would be for the oldest transaction still
 * in committing state.  In theory, XA could call <i> mark() </i> every
 * time a DONE record is logged.  In practice, it should only be
 * necessary to call <i> mark() </i> every minute or so depending on the
 * capacity of the log files.
 * 
 * <p>The Logger maintains an active mark within the set
 * of log files.  A file may be reused only if the mark does not
 * reside within the file.  The Logger will throw
 * LogFileOverflowException if an attempt is made to switch to a
 * file that contains a mark.
 * 
 * @author Michael Giroux
 *
 */
public class Logger extends LogObject
{
  /**
   * indicates whether the LogFile is open.
   * <p>Logger methods return LogClosedException when log is closed.
   */
  protected volatile boolean isClosed = true;
  
  /**
   * Manages a pool of buffers used for log file IO.
   */
  LogBufferManager bmgr = null;
  
  /**
   * Manages a pool of files used for log file IO.
   */
  LogFileManager lfmgr = null;
  
  /**
   * @return activeMark member of the associated LogFileManager.
   */
  public long getActiveMark()
  {
    return lfmgr.activeMark;
  }
  
  /**
   * Construct a Logger using default Configuration object.
   * @throws IOException
   */
  public Logger()
    throws IOException
  {
    this(new Configuration());
  }
  
  /**
   * Construct a Logger using a Configuration supplied
   * by the caller.
   * @param config Configuration object
   * @throws IOException
   */
  public Logger(Configuration config)
    throws IOException
  {
    super(config);

    lfmgr = new LogFileManager(config);
    
    bmgr = new LogBufferManager(config);
  }
  
  /**
   * add a USER record consisting of byte[][] to log. 
   * 
   * <p>if <i> sync </i> parameter is true, then the method will
   * block (in bmgr.put()) until the <i> data </i> buffer is forced to disk.
   * Otherwise, the method returns immediately.
   * 
   * @param data record data
   * @param sync true if call should block until force
   * 
   * @return a key that can be used to locate the record.
   * Some implementations may use the key as a correlation ID 
   * to associate related records.
   * 
   * When automark is disabled (false) the caller must
   * invoke mark() using this key to indicate the location
   * of the oldest active entry in the log.
   * 
   * @throws LogClosedException
   * @throws LogRecordSizeException
   * @throws LogFileOverflowException
   * @throws InterruptedException
   * @throws IOException
   * 
   * @see #mark(long)
   * @see #setAutoMark(boolean)
   */
  public long put(byte[][] data, boolean sync)
    throws LogClosedException, LogRecordSizeException, LogFileOverflowException,
                InterruptedException, IOException
  {
    return put(LogRecordType.USER, data, sync);
  }
  
  /**
   * add a USER record consisting of byte[] to the log.
   * 
   * <p>wrap byte[] <i> data </i> in a new byte[][]
   * and delegates call to put(byte[][], boolean)
   *  
   * @param data byte[] to be written to log
   * @param sync true if caller wishes to block waiting for the
   * record to force to disk.
   * @return log key for the record
   * @throws LogClosedException
   * @throws LogRecordSizeException
   * @throws LogFileOverflowException
   * @throws InterruptedException
   * @throws IOException
   */
  public long put(byte[] data, boolean sync)
    throws LogClosedException, LogRecordSizeException, LogFileOverflowException,
      InterruptedException, IOException
  {
    return put(LogRecordType.USER, new byte[][]{data}, sync);
  }

  /**
   * Sub-classes call this method to write log records with
   * a specific record type.
   * 
   * @param type a record type defined in LogRecordType.
   * @param data record data to be logged.
   * @param sync boolean indicating whether call should
   * wait for data to be written to physical disk.
   * 
   * @return a log key that can be used to reference
   * the record.
   */
  protected long put(short type, byte[][] data, boolean sync)
  throws LogClosedException, LogRecordSizeException, LogFileOverflowException,
  InterruptedException, IOException
  {
    synchronized(this)
    {
      if (isClosed) throw new LogClosedException();
    }
    
    // QUESTION: should we deal with exceptions here?

    long key = bmgr.put(type, data, sync);
    lfmgr.setCurrentKey(key);
    
    return key;
  }
  
  /**
   * sets the LogFile's mark.
   * 
   * <p><i> mark() </i> provides a generalized method for callers
   * to inform the Logger that log space can be released
   * for reuse.
   * 
   * <p>calls LogFileManager to process the request.
   * 
   * @param key is a log key returned by a previous call to put().
   * @param force a boolean that indicates whether the mark data
   * should be forced to disk.  When set to <b> true </b> the caller
   * is blocked until the mark record is forced to disk.
   * 
   * @throws InvalidLogKeyException
   * if <i> key </i> parameter is out of range.
   * <i> key </i> must be greater than current activeMark and less than the most recent
   * key returned by put().
   * @throws LogClosedException
   * if this logger instance has been closed.
   */
  public void mark(long key, boolean force)
    throws InvalidLogKeyException, LogClosedException, IOException, InterruptedException
  {
    synchronized(this)
    {
      if (isClosed)
        throw new LogClosedException("log is closed");
    }
    
    lfmgr.mark(key, force);
  }
  
  /**
   * calls Logger.mark(key, force) with <i> force </i> set to <b> true </b>.
   * <p>Caller is blocked until mark record is forced to disk.
   * @param key a log key returned by a previous call to put().
   * @throws InvalidLogKeyException
   * @throws LogClosedException
   * @throws IOException
   * @throws InterruptedException
   * @see #mark(long, boolean)
   */
  public void mark(long key)
    throws InvalidLogKeyException, LogClosedException, IOException, InterruptedException
  {
    mark(key, true);
  }
  
  /**
   * Sets the LogFile marking mode.
   * 
   * <p>passes call to LogFileManager
   * 
   * @param autoMark true to indicate automatic marking.
   */
  public void setAutoMark(boolean autoMark)
    throws InvalidLogKeyException, LogClosedException, LogFileOverflowException, IOException, InterruptedException
  {
    synchronized(this)
    {
      if (this.isClosed) throw new LogClosedException();
    }
    
    lfmgr.setAutoMark(autoMark);
  }
  
  /**
   * close the Log files and perform necessary cleanup tasks.
   */
  public void close() throws IOException, InterruptedException
  {
    // prevent new threads from adding to the log
    synchronized(this) { isClosed = true; }
    
    lfmgr.close();
    bmgr.close();
  }
  
  /**
   * open Log files and perform necessart initialization.
   * 
   * TODO: consider open(String name) to allow named configurations.
   *       this would allow utility to open two loggers and copy 
   *       old records to new files.
   *
   */
  public void open()
    throws InvalidFileSetException,
           IOException, LogConfigurationException, InvalidLogBufferException, InterruptedException
  {
    lfmgr.open();
    
    try {
      bmgr.open();
    } catch (ClassNotFoundException e) {
      String cnf = "LogBuffer Class not found: " + config.getBufferClassName();
      LogConfigurationException lce = new LogConfigurationException(cnf, e);
      throw lce;
    }
    
    // read header information from each file
    lfmgr.init(bmgr);
    
    // indicate that Log is ready for use.
    synchronized(this) { isClosed = false; }
  }
  
  /**
   * Registers a LogEventListener for log event notifications.
   * 
   * @param eventListener object to be notified of logger events.
   */
  public void setLogEventListener(LogEventListener eventListener)
  {
    lfmgr.setLogEventListener(eventListener);
  }
  
  /**
   * Replays log from a specified mark forward to the current mark.
   * 
   * <p>Beginning with the record located at <i> mark </i>
   * the Logger reads log records forward to the end of the log.
   * USER records are passed to the <i> listener </i> onRecord()
   * method. When the end of log has been reached, replay returns
   * one final record with a type of END_OF_LOG to inform <i> listener </i>
   * that no further records will be returned.
   * 
   * <p>If an error is encountered while reading the log, the
   * <i> listener </i> onError method is called.  Replay terminates
   * when any error occurs and when END_OF_LOG is encountered.
   * 
   * @param listener an object that implements ReplayListener interface.
   * @param mark a log key to begin replay from.
   * <p>The <i> mark </i> should be a valid log key returned by the put()
   * method.  To replay the entire log beginning with the oldest available
   * record, <i> mark </i> should be set to zero (0L).
   * @throws LogConfigurationException
   * most likely because the configured LogBuffer class cannot be found.
   * @throws InvalidLogKeyException
   * if <i> mark </i> is not a valid log key.
   */
  public void replay(ReplayListener listener, long mark)
    throws InvalidLogKeyException, LogConfigurationException
  {
    // replay only the user records.
    bmgr.replay(listener, mark, false);
  }
  
  /**
   * Replays log from the active mark forward to the current position.
   * 
   * @param listener an object that implements ReplayListener interface.
   * @throws LogConfigurationException
   * most likely because the configured LogBuffer class cannot be found.
   * @see #replay(ReplayListener, long)
   */
  public void replay(ReplayListener listener) throws LogConfigurationException
  {
    try {
      bmgr.replay(listener, lfmgr.activeMark, false);
    } catch (InvalidLogKeyException e) {
      // should not happen -- use assert to catch during development
      assert false : "Unhandled InvalidLogKeyException" + e.toString();
    }
  }
  
  /**
   * Allows sub-classes of Logger to replay control records.
   * 
   * @param listener ReplayListener to receive the records 
   * @param mark starting mark (log key) for the replay.
   * @param replayCtrlRecords boolean indicating whether to
   * return CTRL records.
   * @throws InvalidLogKeyException
   * If the <i> mark </i> parameter specifies an invalid log key
   * (one that does not exist in the current set of log files.)
   * @throws LogConfigurationException
   * 
   * @see org.objectweb.howl.log.LogBufferManager#replay(ReplayListener, long, boolean)
   */
  protected void replay(ReplayListener listener, long mark, boolean replayCtrlRecords)
  throws InvalidLogKeyException, LogConfigurationException
  {
    bmgr.replay(listener, mark, replayCtrlRecords);
  }
  
  /**
   * Read a specific record from the log.
   * <p>Control records are not filtered by this method.
   * If the requested mark is valid and identifies a control record,
   * the record will be returned.
   * @param lr LogRecord to be updated or null if caller wishes a new
   *           LogRecord to be allocated.
   * @param mark a log key identifying the location of the record
   *               within the journal
   * @return LogRecord containing requested record
   * @throws InvalidLogKeyException
   *         if logkey parameter is < 0L or if the requested key is not within the current range
   *         of keys in the log.
   * @throws LogConfigurationException
   * @throws LogException
   */  // FEATURE: 300792
  public LogRecord get(LogRecord lr, long mark) throws InvalidLogKeyException,
    LogConfigurationException,
    LogException, InvalidLogBufferException
  {
    /* this code is similar to LogBufferManager.replay() -- potential for refactor */
    int bsn = bmgr.bsnFromMark(mark);
    if (mark < 0 || (bsn == 0 && mark != 0))
      throw new InvalidLogKeyException(Long.toHexString(mark));
    
    if (lr == null)
      lr = new LogRecord((config.getBufferSize() * 1024)/4); // default to 1/4 buffer size
    
    // allocate a LogBuffer that we can use to read the journal
    try {
      if (lr.buffer == null)
        lr.buffer = bmgr.getLogBuffer(-1);
    } catch (ClassNotFoundException e) {
      throw new LogConfigurationException(e);
    }
    
    LogBuffer buffer = lr.buffer;
    
    // read block containing requested mark
    try {
      bmgr.forceCurrentBuffer();
      lfmgr.read(buffer, bsn);
    } catch (IOException e) {
      LogFile lf = buffer.lf;
      String msg = "Error reading " + lf.file + " @ position [" + lf.position + "]";
      throw new LogException(msg, e);
    }
    
    if (buffer.bsn == -1)
    {
      lr.type = LogRecordType.END_OF_LOG;
      return lr;
    }
    
    // verify we have the desired block
    // if requested mark == 0 then we start with the oldest block available
    int markBSN = (mark == 0) ? buffer.bsn : bmgr.bsnFromMark(mark);
    if (markBSN != buffer.bsn) {
      InvalidLogBufferException lbe = new InvalidLogBufferException(
          "block read [" + buffer.bsn + "] not block requested: " + markBSN);
      throw lbe;
    }
    
    /*
     * position buffer to requested mark.
     * 
     * Although the mark contains a buffer offset, we search forward
     * through the buffer to guarantee that we have the start
     * of a record.  This protects against using marks that were
     * not generated by the current Logger.
     */
    lr.get(buffer); // get first record in buffer
    if (mark > 0 && mark > bmgr.markFromBsn(markBSN,0)) {
      while(lr.key < mark) {
        lr.get(buffer);
      }
      if (lr.key != mark) {
        String msg = "The requested mark [" + Long.toHexString(mark) + 
          "] was not found in the log.";
        // BUG 300733 following line changed to throw an exception
        throw new InvalidLogKeyException(msg);
      }
    }
    
    return lr;
  }
  
  /**
   * Read the journal record that follows the record identified by lr.
   * @param lr LogRecord to be updated with the next journal record.
   * <p>The LogRecord <code> lr </code> must have been returned
   * by a previous call to Logger.get(LogRecord, long). 
   * <p>Effectively, the record identified by lr is located, and the record
   * immediately following it is returned.
   * @return LogRecord containing the requested record.
   * @throws IllegalArgumentException
   * if lr parameter is null or if the lr.buffer member is null.
   */  // FEATURE: 300792
  public LogRecord getNext(LogRecord lr)
    throws InvalidLogBufferException, LogException
  {
    if (lr == null || lr.buffer == null) throw new IllegalArgumentException();
    
    LogBuffer buffer = lr.buffer;
    
    // get next record
    lr.get(buffer);
    
    if (lr.isEOB())
    {
      long bsn = buffer.bsn;  // so we can test for wraparound
      try {
        lfmgr.read(buffer, buffer.bsn+1);
      } catch (IOException e) {
        LogFile lf = lr.buffer.lf;
        String msg = "Error reading " + lf.file + " @ position [" + lf.position + "]";
        throw new LogException(msg, e);
      }
      
      if (buffer.bsn == -1 || buffer.bsn < bsn) // BUG 304982
      {
        lr.type = LogRecordType.END_OF_LOG;
        return lr;
      }
      
      lr.get(buffer);
    }

    return lr;
  }

  /**
   * return an XML node containing statistics for the Logger,
   * the LogFile pool and the LogBuffer pool.
   * 
   * <p>The getStats method for the LogBufferManager and LogFileManager
   * are called to include statistics for these contained objects.
   * 
   * @return String contiining XML node.
   */
  public String getStats()
  {
    String name = this.getClass().getName();
    StringBuffer stats = new StringBuffer(
        "<Logger  class='" + name + "'>" 
    );
    
    // TODO: append Logger specific stats
    
    stats.append(bmgr.getStats());
    
    stats.append(lfmgr.getStats());
    
    stats.append("\n</Logger>" +
        "\n");
    
    return stats.toString();
  }
  
}
