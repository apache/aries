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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.util.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.aries.util.internal.MessageUtil;

/**
 * This class can be used to buffer an arbitrary amount of content from an input stream and be able to reset to 
 * the start.
 */
public class RememberingInputStream extends InputStream {

  /** The size by which to grow our array */
  private static final int bufferGrowthSize = 0x4000;
  /** The bytes that have been read so far */
  private byte[] bytes = new byte[bufferGrowthSize];
  /** Index of the next empty entry in the array */
  private int pos = 0;
  /** The input stream that actually holds the data */
  private final InputStream stream;
  /** Index of the last valid byte in the byte array */
  private int maxRead = -1;
  /** The point to reset to */
  private int markPoint = -1;
  
  
  public RememberingInputStream(InputStream in) throws IOException{
    stream = in;
    // Pre fill with data that we know we're going to need - it's 
    // more efficient than the single byte reads are - hopefully
    // someone reading a lot of data will do reads in bulk
    
    maxRead = stream.read(bytes) - 1;
  }

  @Override
  public int read() throws IOException {
    
    if(pos <= maxRead)
    {
      //We can't return the byte directly, because it is signed
      //We can pretend this is an unsigned byte by using boolean
      //& to set the low end byte of an int.
      return bytes[pos++] & 0xFF;
    } else {
      int i = stream.read();
      if(i<0)
        return i;
    
      ensureCapacity(0);
      bytes[pos++] = (byte) i;
      return i;
    }
  }

  /**
   * Ensure our internal byte array can hold enough data
   * @param i one less than the number of bytes that need
   *          to be held.
   */
  private void ensureCapacity(int i) {
    if((pos + i) >= bytes.length) {
      byte[] tmp = bytes;
      int newLength = bytes.length + bufferGrowthSize;
      while(newLength < pos + i) {
        newLength += bufferGrowthSize;
      }
      bytes = new byte[newLength];
      System.arraycopy(tmp, 0, bytes, 0, (maxRead >= pos) ? maxRead + 1 : pos);
    }
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if(pos <= maxRead) {
      if(pos + len <= maxRead)
      {
        System.arraycopy(bytes, pos, b, off, len);
        pos += len;
        return len;
      } else {
        int lengthLeftOfBuffer = (maxRead - pos) + 1;
        System.arraycopy(bytes, pos, b, off, lengthLeftOfBuffer);
        int read = stream.read(b, off + lengthLeftOfBuffer, len - lengthLeftOfBuffer);
        if(read < 0) {
          pos += lengthLeftOfBuffer;
          return lengthLeftOfBuffer;
        }
        ensureCapacity(lengthLeftOfBuffer + read - 1);
        System.arraycopy(b, off + lengthLeftOfBuffer, bytes, maxRead + 1, read);
        pos +=  (lengthLeftOfBuffer + read);
        return lengthLeftOfBuffer + read;
      }
    } else {
      int i = stream.read(b, off, len);
      if(i<0)
        return i;
      ensureCapacity(i - 1);
      System.arraycopy(b, off, bytes, pos, i);
      pos += i;
      return i;
    }
  }

  @Override
  public long skip(long n) throws IOException {
    throw new IOException(MessageUtil.getMessage("UTIL0017E"));
  }

  @Override
  public int available() throws IOException {
    if(pos <= maxRead) 
      return (maxRead - pos) + 1;
    else 
      return stream.available(); 
  }

  @Override
  public synchronized void mark(int readlimit) {
    markPoint = pos;
  }

  @Override
  public synchronized void reset() throws IOException {
    if(maxRead < pos)
      maxRead = pos - 1;
    pos = markPoint;
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  /**
   * Noop. Does not close the passed in archive, which is kept open for further reading.
   */
  @Override
  public void close() throws IOException {
    //No op, don't close the parent.
  }
  
  /**
   * Actually closes the underlying InputStream. Call this method instead of close, which is implemented as a no-op.
   * Alternatively call close directly on the parent.
   * @throws IOException
   */
  public void closeUnderlying() throws IOException {
      stream.close();
  }
}