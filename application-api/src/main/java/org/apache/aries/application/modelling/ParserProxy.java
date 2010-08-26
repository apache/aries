package org.apache.aries.application.modelling;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

/** 
 * This interface is implemented by the service which proxies the
 * Apache Aries blueprint parser. ParserProxy services offer higher
 * level methods built on top of the Blueprint parser. 
 *
 */
public interface ParserProxy {

  /**
   * Parse blueprint xml files and extract the parsed ServiceMetadata objects
   * @param blueprintsToParse URLs to blueprint xml files
   * @return List of (wrapped) ServiceMetadata objects
   */
  public List<? extends WrappedServiceMetadata> parse (List<URL> blueprintsToParse) throws Exception;
  
  /**
   * Parse a blueprint xml files and extract the parsed ServiceMetadata objects
   * @param blueprintToParse URL to blueprint xml file
   * @return List of (wrapped) ServiceMetadata objects
   */
  public List<? extends WrappedServiceMetadata> parse (URL blueprintToParse) throws Exception;
  
  /**
   * Parse an InputStream containing blueprint xml and extract the parsed ServiceMetadata objects
   * @param blueprintToParse InputStream containing blueprint xml data. The caller is responsible
   * for closing the stream afterwards. 
   * @return List of (wrapped) ServiceMetadata objects
   */
  public List<? extends WrappedServiceMetadata> parse (InputStream blueprintToParse) throws Exception;
  
  /**
   * Parse an InputStream containing blueprint xml and extract Service, Reference and RefList
   * elements.
   * @return All parsed Service, Reference and RefList elements 
   */
  public ParsedServiceElements parseAllServiceElements (InputStream blueprintToParse) throws Exception;
  
}
