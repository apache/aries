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
package org.apache.aries.subsystem.core.internal;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

    private FileUtils() {
    }

    /**
     * Build a directory path - creating directories if necessary
     *
     * @param file
     * @return true if the directory exists, or making it was successful
     * @throws java.io.IOException
     */
    public static void mkdirs(File file) throws IOException {
        if (!file.isDirectory() && !file.mkdirs()) {
            throw new IOException("Could not create directory: " + file);
        }
    }

    /**
     * Unpack an archive from an input stream
     *
     * @param is
     * @param targetDir
     * @throws IOException
     */
    public static void unpackArchive(InputStream is, File targetDir) throws IOException {
        mkdirs(targetDir);
        byte[] buf = new byte[1024];
        ZipInputStream zis = new ZipInputStream(is);
        try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                File file = new File(targetDir, entry.getName());
                // Take the sledgehammer approach to creating directories
                // to work around ZIP's that incorrectly miss directories
                mkdirs(file.getParentFile());
                if (!entry.isDirectory()) {
                    FileOutputStream fos = new FileOutputStream(file);
                    try {
                        int n;
                        while ((n = zis.read(buf, 0, 1024)) > -1) {
                            fos.write(buf, 0, n);
                        }
                    } finally {
                        closeQuietly(fos);
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                } else {
                    mkdirs(file);
                    entry = zis.getNextEntry();
                }
            }//while
        } finally {
            closeQuietly(zis, is);
        }
     }

    public static void closeQuietly(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
