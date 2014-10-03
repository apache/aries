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


import static org.apache.aries.util.filesystem.IDirectoryFinder.IDIR_SCHEME;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IDirectoryFinder;
import org.osgi.framework.Version;

public class Location {
  enum LocationType {
    SUBSYSTEM("subsystem", "subsystem"), IDIRFINDER(IDIR_SCHEME, IDIR_SCHEME), URL("url", null), UNKNOWN("unknown", null);
    final String toString;
    final String scheme;
    LocationType(String toString, String scheme) {this.toString = toString; this.scheme = scheme;}
    @Override
    public String toString() {return toString;}
  };

  private final LocationType type;
  private final String value;
  private final URI uri;
  private final URL url;
  private final SubsystemUri subsystemUri;
  private final IllegalArgumentException subsystemUriException;

  /*
   * type, value, uri are always set to some non-null value, url and
   * subsystemUri depend on the type.
   */
  public Location(String location) throws MalformedURLException, URISyntaxException {
    value = location;
    URI locationUri = null;
    try {
        locationUri = new URI(location);
    } catch ( URISyntaxException urise ) {
        // ignore
    }
    if (locationUri == null) {
        type = LocationType.UNKNOWN;
        url = null;
        uri = null;
        subsystemUri = null;
        subsystemUriException = null;

    } else if (locationUri.isAbsolute()) {  // i.e. looks like scheme:something
      String scheme = locationUri.getScheme();
      if (LocationType.SUBSYSTEM.scheme.equals(scheme)) {
        type = LocationType.SUBSYSTEM;
        SubsystemUri ssUri;
        IllegalArgumentException ssUriException = null;
        try {
          ssUri = new SubsystemUri(location);
        } catch (IllegalArgumentException ex) {
          // In some cases the SubsystemUri can't be parsed by the SubsystemUri parser.
          ssUri = null;
          ssUriException = ex;
        }
        subsystemUri = ssUri;
        subsystemUriException = ssUriException;
        if (subsystemUri != null) {
          url = subsystemUri.getURL(); // subsystem uris may contain a nested url.
          uri = (url==null) ? null : url.toURI();
        } else {
          url = null;
          uri = locationUri;
        }
      } else if (LocationType.IDIRFINDER.scheme.equals(scheme)) {
        type = LocationType.IDIRFINDER;
        subsystemUri = null;
        subsystemUriException = null;
        url = null;
        uri = locationUri;
      } else {                       // otherwise will only accept a url, (a url
        type = LocationType.URL;     // always has a scheme, so fine to have
        subsystemUri = null;         // this inside the 'if isAbsolute' block).
        subsystemUriException = null;
        URL localUrl = null;
        try {
            localUrl = locationUri.toURL();
        } catch ( final MalformedURLException mue) {
            // ignore
        }
        url = localUrl;
        uri = locationUri;
      }
    } else {
    	type = LocationType.UNKNOWN;
    	url = null;
    	uri = null;
    	subsystemUri = null;
    	subsystemUriException = null;
    }
  }

  public String getValue() {
    return value;
  }

  public String getSymbolicName() {
    if (subsystemUriException != null) {
      throw subsystemUriException;
    }
    return (subsystemUri!=null) ? subsystemUri.getSymbolicName() : null;
  }

  public Version getVersion() {
    if (subsystemUriException != null) {
      throw subsystemUriException;
    }
    return (subsystemUri!=null) ? subsystemUri.getVersion() : null;
  }

  public IDirectory open() throws IOException, URISyntaxException {
    switch (type) {
      case IDIRFINDER :
        return retrieveIDirectory();
      case SUBSYSTEM : // drop through to share 'case url' code
      case URL :
        if ("file".equals(url.getProtocol()))
          return FileSystem.getFSRoot(new File(uri));
        else
          return FileSystem.getFSRoot(url.openStream());
      case UNKNOWN:
    	  // Only try to create a URL with the location value here. If the
    	  // location was just a string and an InputStream was provided, this
    	  // method will never be called.
    	  return FileSystem.getFSRoot(new URL(value).openStream());
      default : // should never get here as switch should cover all types
        throw new UnsupportedOperationException("cannot open location of type " + type);
    }
  }

  /*
   * Although the uri should contain information about the directory finder
   * service to use to retrieve the directory, there are not expected to be
   * many such services in use (typically one), so a simple list of all
   * directory finders is maintained by the activator and we loop over them in
   * turn until the desired directory is retrieved or there are no more finders
   * left to call.
   */
  private IDirectory retrieveIDirectory() throws IOException {
    Collection<IDirectoryFinder> iDirectoryFinders = Activator.getInstance().getIDirectoryFinders();
    for(IDirectoryFinder iDirectoryFinder : iDirectoryFinders) {
      IDirectory directory = iDirectoryFinder.retrieveIDirectory(uri);
      if (directory!=null)
        return directory;
    }
    throw new IOException("cannot find IDirectory corresponding to id " + uri);
  }

  @Override
  public String toString() {
	  return value;
  }

}
