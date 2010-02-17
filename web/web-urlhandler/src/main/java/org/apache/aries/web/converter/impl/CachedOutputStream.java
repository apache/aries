/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.web.converter.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CachedOutputStream extends OutputStream {

    private static final int DEFAULT_THRESHOLD = 64 * 1024;
    
    private OutputStream currentStream;
    private long threshold;
    private int totalLength;
    private boolean inmem;
    private List<InputStream> streams;
    private File tempFile;
    private File outputDir;

    public CachedOutputStream() {
        this(DEFAULT_THRESHOLD, null);
    }

    public CachedOutputStream(long threshold, File outputDir) {
        this.threshold = threshold; 
        this.outputDir = outputDir;
        this.currentStream = new ByteArrayOutputStream(2048);
        this.inmem = true;
        this.streams = new ArrayList<InputStream>(1);
    }

    public void flush() throws IOException {
        currentStream.flush();
    }
    
    public void close() throws IOException {
        currentStream.flush();       
        currentStream.close();
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    
    public void write(byte[] b, int off, int len) throws IOException {
        totalLength += len;
        if (inmem && totalLength > threshold) {
            createFileOutputStream();
        }
        currentStream.write(b, off, len);
    }

    public void write(int b) throws IOException {
        totalLength++;
        if (inmem && totalLength > threshold) {
            createFileOutputStream();
        }
        currentStream.write(b);
    }

    private void createFileOutputStream() throws IOException {
        ByteArrayOutputStream bout = (ByteArrayOutputStream) currentStream;
        if (outputDir == null) {
            tempFile = File.createTempFile("cos", "tmp");
        } else {
            tempFile = File.createTempFile("cos", "tmp", outputDir);
        }
        
        currentStream = new BufferedOutputStream(new FileOutputStream(tempFile));
        bout.writeTo(currentStream);
        inmem = false;
    }

    public void destroy() {
        streams.clear();
        if (tempFile != null) {
            tempFile.delete();
        }
    }
    
    public int size() {
        return totalLength;
    }
    
    public InputStream getInputStream() throws IOException {
        close();
        if (inmem) {
            return new ByteArrayInputStream(((ByteArrayOutputStream) currentStream).toByteArray());
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(tempFile) {
                    public void close() throws IOException {
                        super.close();
                        maybeDeleteTempFile(this);
                    }
                };
                streams.add(fileInputStream);
                return fileInputStream;
            } catch (FileNotFoundException e) {
                throw new IOException("Cached file was deleted, " + e.toString());
            }
        }
    }
    
    private void maybeDeleteTempFile(Object stream) {
        streams.remove(stream);
        if (tempFile != null && streams.isEmpty()) {
            tempFile.delete();
            tempFile = null;
        }
    }

}
