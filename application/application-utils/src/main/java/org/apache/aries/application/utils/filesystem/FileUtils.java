package org.apache.aries.application.utils.filesystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.aries.application.utils.manifest.BundleManifest;

public class FileUtils {
  /**
   * Check whether a file is a bundle.
   * @param file the file path
   * @return
   */
  public static boolean isBundle(File file) {
    BundleManifest bm = BundleManifest.fromBundle(file);
    return ((bm != null) && (bm.isValid()));
  }

  /**
   * Get a list of urls for the bundles under the parent url
   * @param sourceDir The parent url
   * @return
   * @throws IOException
   */
  public static  List<URI> getBundlesRecursive(URI sourceDir) throws IOException {
    List<URI> filesFound = new ArrayList<URI>();
    if (sourceDir == null) {
      return filesFound;
    } if (sourceDir != null) {
      File sourceFile = new File(sourceDir);
      if (sourceFile.isFile()) {
        if (isBundle(sourceFile)) {
          filesFound.add(sourceDir);
        }
      } else if (sourceFile.isDirectory()) {
        File[] subFiles = sourceFile.listFiles();
        if ((subFiles !=null) && (subFiles.length >0)) {
          for (File file : subFiles) {
            filesFound.addAll(getBundlesRecursive(file.toURI()));
          }
        }
      }
    }
    return filesFound;
  }

}
