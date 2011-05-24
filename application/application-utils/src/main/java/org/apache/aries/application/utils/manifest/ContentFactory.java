package org.apache.aries.application.utils.manifest;

import org.apache.aries.application.Content;
import org.apache.aries.application.impl.ContentImpl;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValueMap;


public class ContentFactory {
  /**
   * Parse a content object
   * @param bundleSymbolicName bundle symbolic name
   * @param versionRange version range in the String format
   * @return Content object
   */
  public static Content parseContent(String bundleSymbolicName, String versionRange) {
    return new ContentImpl(bundleSymbolicName, ManifestHeaderProcessor.parseVersionRange(versionRange));
  }
  
  /**
   * Parse a content
   * @param contentName The content name
   * @param nameValueMap The map containing the content attributes/directives
   * @return a content object
   */
  public static Content parseContent(String contentName, NameValueMap<String, String> nameValueMap) {
    return new ContentImpl(contentName, nameValueMap);
  }
}
