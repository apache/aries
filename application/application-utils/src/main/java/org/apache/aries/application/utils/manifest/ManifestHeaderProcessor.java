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

package org.apache.aries.application.utils.manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.application.utils.internal.MessageUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;


public class ManifestHeaderProcessor
{
  /**
   * A simple class to associate two types.
   *
   * @param <N> The type for the 'Name'
   * @param <V> The type for the 'Value'
   */
  public static class NameValuePair<N,V>{
    private N name;
    private V value;
    public NameValuePair(N name, V value)
    {
      this.name = name;
      this.value = value;
    }
    public N getName()
    {
      return name;
    }
    public void setName(N name)
    {
      this.name = name;
    }
    public V getValue()
    {
      return value;
    }
    public void setValue(V value)
    {
      this.value = value;
    }
    public String toString(){
      return "{"+name.toString()+"::"+value.toString()+"}";
    }
    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj)
    {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      final NameValuePair other = (NameValuePair) obj;
      if (name == null) {
        if (other.name != null) return false;
      } else if (!name.equals(other.name)) return false;
      if (value == null) {
        if (other.value != null) return false;
      } else if (!value.equals(other.value)) return false;
      return true;
    }
  }
  
  /**
   * Intended to provide a standard way to add Name/Value's to 
   * aggregations of Name/Value's.
   *
   * @param <N> Type of 'Name'
   * @param <V> Type of 'Value'
   */
  public static interface NameValueCollection<N,V>{
    /**
     * Add this Name & Value to the collection.
     * @param n
     * @param v
     */
    public void addToCollection(N n,V v);
  }

  /**
   * Map of Name -> Value.
   * 
   * @param <N> Type of 'Name'
   * @param <V> Type of 'Value'
   */
  public static class NameValueMap<N,V> extends HashMap<N,V> implements NameValueCollection<N,V>, Map<N,V>{
    public void addToCollection(N n,V v){
      this.put(n,v);
    }
    public String toString(){
      StringBuffer sb = new StringBuffer();
      sb.append("{");
      boolean first=true;
      for(Map.Entry<N, V> entry : this.entrySet()){
        if(!first)sb.append(",");
        first=false;
        sb.append(entry.getKey()+"->"+entry.getValue());
      }
      sb.append("}");
      return sb.toString();
    }
  }
  
  /**
   * List of Name/Value
   *
   * @param <N> Type of 'Name'
   * @param <V> Type of 'Value'
   */
  public static class NameValueList<N,V> extends ArrayList<NameValuePair<N,V>> implements NameValueCollection<N,V>, List<NameValuePair<N,V>>{    
    public void addToCollection(N n,V v){
      this.add(new NameValuePair<N,V>(n,v));
    } 
    public String toString(){
      StringBuffer sb = new StringBuffer();
      sb.append("{");
      boolean first = true;
      for(NameValuePair<N, V> nvp : this){
        if(!first)sb.append(",");
        first=false;
        sb.append(nvp.toString());        
      }
      sb.append("}");
      return sb.toString();
    }
  }
  
  /**
   * 
   * Splits a delimiter separated string, tolerating presence of non separator commas
   * within double quoted segments.
   * 
   * Eg.
   * com.ibm.ws.eba.helloWorldService;version="[1.0.0, 1.0.0]" &
   * com.ibm.ws.eba.helloWorldService;version="1.0.0"
   * com.ibm.ws.eba.helloWorld;version="2";bundle-version="[2,30)"
   * com.acme.foo;weirdAttr="one;two;three";weirdDir:="1;2;3"
   *  @param value          the value to be split
   *  @param delimiter      the delimiter string such as ',' etc.
   *  @return List<String>  the components of the split String in a list
   */
  public static List<String> split(String value, String delimiter)
  {
    List<String> result = new ArrayList<String>();
    if (value != null) {
      String[] packages = value.split(delimiter);
      
      for (int i = 0; i < packages.length; ) {
        String tmp = packages[i++].trim();
        // if there is a odd number of " in a string, we need to append
        while (count(tmp, "\"") % 2 != 0) {
          // check to see if we need to append the next package[i++]          
            if (i<packages.length)
              tmp = tmp + delimiter + packages[i++].trim();
            else 
              // oops. The double quotes are not paired up. We have reached to the end of the string.
              throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0008E",tmp));        
        }
        
        result.add(tmp);
        
      }
    }
    return result;
  }  
  
 /**
  * count the number of characters in a string
  * @param parent The string to be searched
  * @param subString The substring to be found
  * @return the number of occurrence of the subString
  */
  private static int count(String parent, String subString) {
    
    int count = 0 ;
    int i = parent.indexOf(subString);
    while (i > -1) {
      if (parent.length() >= i+1)
        parent = parent.substring(i+1);
      count ++;
      i = parent.indexOf(subString);
    }
    return count;
  }
  /**
   * Internal method to parse headers with the format<p>
   *   [Name](;[Name])*(;[attribute-name]=[attribute-value])*<br> 
   * Eg.<br>
   *   rumplestiltskin;thing=value;other=something<br>
   *   littleredridinghood
   *   bundle1;bundle2;other=things
   *   bundle1;bundle2
   *   
   * @param s data to parse
   * @return a list of NameValuePair, with the Name being the name component, 
   *         and the Value being a NameValueMap of key->value mappings.   
   */
  private static List<NameValuePair<String, NameValueMap<String, String>>> genericNameWithNameValuePairProcess(String s){    
    String name;
    NameValueMap<String,String> params = null;
    List<NameValuePair<String, NameValueMap<String, String>>> nameValues = new ArrayList<NameValuePair<String, NameValueMap<String, String>>>();
    List<String> pkgs = new ArrayList<String>();
    int index = s.indexOf(";");
    if(index==-1){
      name = s;
      params = new NameValueMap<String, String>();
      pkgs.add(name);
    }else{       
      name = s.substring(0,index).trim();
      String tail = s.substring(index+1).trim();
      
      pkgs.add(name); // add the first package
      StringBuilder parameters = new StringBuilder();
          
      
      // take into consideration of multiple packages separated by ';'
      // while they share the same attributes or directives
      List<String> tailParts = split(tail, ";");
      boolean firstParameter =false;
      
      for (String part : tailParts) {
        // if it is not a parameter and no parameter appears in front of it, it must a package
        if (!!!(part.contains("=")))  {
          // Need to make sure no parameter appears before the package, otherwise ignore this string
          // as this syntax is invalid
          if (!!!(firstParameter))
            pkgs.add(part);
        } else {
          if (!!!(firstParameter)) 
            firstParameter = true;

          parameters.append(part + ";");
        }
      }          
      
      if (parameters.length() != 0) {
        //remove the final ';' if there is one
        if (parameters.toString().endsWith(";")) {
         
          parameters = parameters.deleteCharAt(parameters.length() -1);
        }       
        
        params = genericNameValueProcess(parameters.toString());
      }
      
    }
    for (String pkg : pkgs) {
      nameValues.add(new NameValuePair<String, NameValueMap<String, String>>(pkg,params));
    }  
    
    return nameValues;
   
  }

  /**
   * Internal method to parse headers with the format<p>
   *   [attribute-name]=[attribute-value](;[attribute-name]=[attribute-value])*<br>
   * Eg.<br>
   *   thing=value;other=something<br>
   * <p>
   * Note. Directives (name:=value) are represented in the map with name suffixed by ':'
   *   
   * @param s data to parse
   * @return a NameValueMap, with attribute-name -> attribute-value.
   */
  private static NameValueMap<String,String> genericNameValueProcess(String s){
    NameValueMap<String,String> params = new NameValueMap<String,String>();  
    List<String> parameters = split(s, ";");
    for(String parameter : parameters) {
      List<String> parts = split(parameter,"=");
      // do a check, otherwise we might get NPE   
      if (parts.size() ==2) {
        String second = parts.get(1).trim();
        if (second.startsWith("\"") && second.endsWith("\""))
          second = second.substring(1,second.length()-1);
        params.put(parts.get(0).trim(), second);
      }
    }

    return params;
  }
  
  /**
   * Processes an import/export style header.. <p> 
   *  pkg1;attrib=value;attrib=value,pkg2;attrib=value,pkg3;attrib=value
   * 
   * @param out The collection to add each package name + attrib map to.
   * @param s The data to parse
   */
  private static void genericImportExportProcess(NameValueCollection<String, NameValueMap<String,String>>out, String s){
    List<String> packages = split(s, ",");
    for(String pkg : packages){   
      List<NameValuePair<String, NameValueMap<String, String>>> ps = genericNameWithNameValuePairProcess(pkg);
      for (NameValuePair<String, NameValueMap<String, String>> p : ps) {
        out.addToCollection(p.getName(), p.getValue());
      }
    }    
  }
  
  /**
   * Parse an export style header.<p>
   *   pkg1;attrib=value;attrib=value,pkg2;attrib=value,pkg3;attrib=value2
   * <p>
   * Result is returned as a list, as export does allow duplicate package exports.
   * 
   * @param s The data to parse.
   * @return List of NameValuePairs, where each Name in the list is an exported package, 
   *         with its associated Value being a NameValueMap of any attributes declared. 
   */
  public static List<NameValuePair<String, NameValueMap<String, String>>> parseExportString(String s){
    NameValueList<String, NameValueMap<String, String>> retval = new NameValueList<String, NameValueMap<String, String>>();
    genericImportExportProcess(retval, s);
    return retval;
  }
  
  /**
   * Parse an export style header in a list.<p>
   *   pkg1;attrib=value;attrib=value
   *   pkg2;attrib=value
   *   pkg3;attrib=value2
   * <p>
   * Result is returned as a list, as export does allow duplicate package exports.
   * 
   * @param s The data to parse.
   * @return List of NameValuePairs, where each Name in the list is an exported package, 
   *         with its associated Value being a NameValueMap of any attributes declared. 
   */
  public static List<NameValuePair<String, NameValueMap<String, String>>> parseExportList(List<String> list){
    NameValueList<String, NameValueMap<String, String>> retval = new NameValueList<String, NameValueMap<String, String>>();
    for(String pkg : list){   
      List<NameValuePair<String, NameValueMap<String, String>>> ps = genericNameWithNameValuePairProcess(pkg);
      for (NameValuePair<String, NameValueMap<String, String>> p : ps) {
        retval.addToCollection(p.getName(), p.getValue());
      }
    } 
    return retval;
  }
  
  /**
   * Parse an import style header.<p>
   *   pkg1;attrib=value;attrib=value,pkg2;attrib=value,pkg3;attrib=value
   * <p>
   * Result is returned as a set, as import does not allow duplicate package imports.
   * 
   * @param s The data to parse.
   * @return Map of NameValuePairs, where each Key in the Map is an imported package, 
   *         with its associated Value being a NameValueMap of any attributes declared. 
   */  
  public static Map<String, NameValueMap<String, String>> parseImportString(String s){
    NameValueMap<String, NameValueMap<String, String>> retval = new NameValueMap<String, NameValueMap<String, String>>();
    genericImportExportProcess(retval, s);
    return retval;    
  }
  
  /**
   * Parse a bundle symbolic name.<p>
   *   bundlesymbolicname;attrib=value;attrib=value
   * <p>
   * 
   * @param s The data to parse.
   * @return NameValuePair with Name being the BundleSymbolicName, 
   *         and Value being any attribs declared for the name. 
   */   
  public static NameValuePair<String, NameValueMap<String, String>> parseBundleSymbolicName(String s){
    return genericNameWithNameValuePairProcess(s).get(0); // should just return the first one
  }

  
  /**
   * A simple class to represent Version Range information.<br>
   * Intended to provide version range parsing for callers.
   */
  public static class VersionRange
  {
    private boolean minimumExclusive = false;
    private boolean maximumExclusive = false;
    private Version minimumVersion = null;
    private Version maximumVersion = null;
    /** exact version */
    private boolean exactVersion = false;

    /**
     * Private, constructed via ManifestHeaderProcessor.parseVersionRange
     * @param version The string data to parse.
     */
    private VersionRange(String version) throws IllegalArgumentException
    {
      if (version != null) {
        version = version.trim();
        if(version.startsWith("\"") && version.endsWith("\"")){
          version = version.substring(1,version.length()-1).trim();
        }
        
        if ((version.startsWith("[") || version.startsWith("("))
            && (version.endsWith("]") || version.endsWith(")"))) {
          if (version.startsWith("[")) minimumExclusive = false;
          else if (version.startsWith("(")) minimumExclusive = true;

          if (version.endsWith("]")) maximumExclusive = false;
          else if (version.endsWith(")")) maximumExclusive = true;

          int index = version.indexOf(',');
          String minVersion = version.substring(1, index);
          String maxVersion = version.substring(index + 1, version.length() - 1);

          try {
            minimumVersion = Version.parseVersion(minVersion);
            maximumVersion = Version.parseVersion(maxVersion);
            
            if ((minimumVersion.compareTo(maximumVersion) == 0) 
                && !!!maximumExclusive && !!!minimumExclusive){
              exactVersion = true;             
            }
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0009E", version), nfe);
          }
        } else {
          minimumExclusive = false;
          maximumExclusive = false;
          
          try {
            minimumVersion = Version.parseVersion(version);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0009E", version), nfe);
          }
        }
      }else{
        throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0010E"));        
      }
    }
    
    /**
     * Private, constructed via ManifestHeaderProcessor.parseVersionRange
     * @param version The string data to parse.
     * @param exactVersion whether this is an exact version
     */
    private VersionRange(String version, boolean exactVersion) throws IllegalArgumentException {
      this(version);
      this.exactVersion = exactVersion;
      
      if (this.maximumVersion == null) {
        this.maximumVersion = this.minimumVersion;
      }
      if (exactVersion) {
        if (!!!minimumVersion.equals(maximumVersion) || minimumExclusive || maximumExclusive) {
          throw new IllegalArgumentException(MessageUtil.getMessage("APPUTILS0011E", version));
        }
      }
    }

    /**
     * This method checks that the provided version matches the desired version.
     * 
     * @param version the version.
     * @return        true if the version matches, false otherwise.
     */
    public boolean matches(Version version)
    {
      boolean result;
      if (this.getMaximumVersion() == null) {
        result = this.getMinimumVersion().compareTo(version) <= 0;
      } else {
        int minN = this.isMinimumExclusive() ? 0 : 1;
        int maxN = this.isMaximumExclusive() ? 0 : 1;
        
        result = (this.getMinimumVersion().compareTo(version) < minN) &&
                 (version.compareTo(this.getMaximumVersion()) < maxN);
      }
      return result;
    }
    
    /**
     * Returns true if the Minimum is considered Exclusive<br> 
     * The Range was defined starting with '('
     * @return true if exclusive, false otherwise.
     */
    public boolean isMinimumExclusive()
    {
      return minimumExclusive;
    }
    /**
     * Returns true if the Maximum is considered Exclusive<br> 
     * The Range was defined ending with ')'<p>
     * This has no meaning if getMaximumVersion returns null.<br>
     * @return true if exclusive, false otherwise.
     */
    public boolean isMaximumExclusive()
    {
      return maximumExclusive;
    }
    /**
     * Returns the parsed Minimum Version
     * @return Minimum Version
     */
    public Version getMinimumVersion()
    {
      return minimumVersion;
    }
    /**
     * Returns the parsed Maximum Version. 
     * May return null if none was set, interpretable as Max Unbounded.
     * @return Maximum Version, or null if none was set.
     */
    public Version getMaximumVersion()
    {
      return maximumVersion;
    }
    
    /**
     * check if the versioninfo is the exact version
     * @return
     */
    public boolean isExactVersion() {
      return this.exactVersion;
    }
    
    /**
     * this method returns the exact version from the versionInfo obj.
     * this is used for DeploymentContent only to return a valid exact version
     * otherwise, null is returned.
     * @return
     */
    public Version getExactVersion() {
      return this.exactVersion ? getMinimumVersion() : null;
    }
  }
  
  /**
   * Parse a version range.. 
   * 
   * @param s
   * @return VersionRange object.
   * @throws IllegalArgumentException if the String could not be parsed as a VersionRange
   */
  public static VersionRange parseVersionRange(String s) throws IllegalArgumentException{
    return new VersionRange(s);
  }
  
  /**
   * Parse a version range and indicate if the version is an exact version 
   * 
   * @param s
   * @param exactVersion
   * @return VersionRange object.
   * @throws IllegalArgumentException if the String could not be parsed as a VersionRange
   */
  public static VersionRange parseVersionRange(String s, boolean exactVersion) throws IllegalArgumentException{
    return new VersionRange(s, exactVersion);
  }

  /**
   * This method is temporary here, until VersionRange becomes it's own top level class.
   * 
   * @param type
   * @param name
   * @param attribs
   * @return
   */
  public static String generateFilter(String type, String name, Map<String, String> attribs){
    StringBuffer filter = new StringBuffer();
    StringBuffer realAttribs = new StringBuffer();
    String result;
    //shortcut for the simple case with no attribs.
    boolean realAttrib = false;
    if(attribs.isEmpty())
      filter.append("("+type+"="+name+")");
    else{    
      //process all the attribs passed. 
      //find out whether there are attributes on the filter
      
      filter.append("(&("+type+"="+name+")");      
      for(Map.Entry<String,String> attrib : attribs.entrySet()){
        String attribName = attrib.getKey();
        
        if(attribName.endsWith(":")){
          //skip all directives. It is used to affect the attribs on the filter xml.
        }else if((Constants.VERSION_ATTRIBUTE.equals(attribName)) || (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(attribName))){
          //version and bundle-version attrib requires special conversion.
          realAttrib = true;
          
          VersionRange vr = ManifestHeaderProcessor.parseVersionRange(attrib.getValue());

          filter.append("(" + attribName + ">="+vr.getMinimumVersion());
    
          if(vr.getMaximumVersion()!=null) {
            filter.append(")(" + attribName + "<=");
            filter.append(vr.getMaximumVersion());
          }
    
          if(vr.getMaximumVersion()!=null && vr.isMinimumExclusive()) {
            filter.append(")(!(" + attribName + "=");
            filter.append(vr.getMinimumVersion());
            filter.append(")");
          }
    
          if(vr.getMaximumVersion()!=null && vr.isMaximumExclusive()) {
            filter.append(")(!(" + attribName + "=");
            filter.append(vr.getMaximumVersion());
            filter.append(")");
          }
          filter.append(")"); 
          
        }else{
          //attribName was not version.. 
          realAttrib = true;
          
          filter.append("("+attribName+"="+attrib.getValue()+")");
          // store all attributes in order to build up the mandatory filter and separate them with ", "
          // skip bundle-symbolic-name in the mandatory directive query
          if (!!!Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE.equals(attribName)) {
            realAttribs.append(attribName);
            realAttribs.append(", ");
          }
        }     
      }
      // tidy up realAttribs - remove the final ,
      
      if (realAttribs.length() > 0) {
        String attribStr = (realAttribs.toString()).trim();
        // remove the final ,
        if ((attribStr.length() > 0) && (attribStr.endsWith(","))) {
          attribStr = attribStr.substring(0, attribStr.length() - 1);
        }
        // build the mandatory filter, e.g.(mandatory:&lt;*company, local)
        filter.append("(" + Constants.MANDATORY_DIRECTIVE + ":" + "<*"+attribStr + ")");
      }
      filter.append(")"); 
    }

    
    if (!!!(realAttrib)) {
      result = "("+type+"="+name+")";
    } else {
      result = filter.toString();
    }
    
    return result;
  }

  private static final Pattern FILTER_ATTR = Pattern.compile("(\\(!)?\\((.*?)([<>]?=)(.*?)\\)\\)?");
  private static final String LESS_EQ_OP = "<=";
  private static final String GREATER_EQ_OP = ">=";
  
  private static Map<String,String> parseFilterList(String filter)
  {
    Map<String, String> result = new HashMap<String, String>();
    Set<String> negatedVersions = new HashSet<String>();
    
    String lowerVersion = null;
    String upperVersion = null;
    
    Matcher m = FILTER_ATTR.matcher(filter);
    while (m.find()) {
      boolean negation = m.group(1) != null;
      String attr = m.group(2);
      String op = m.group(3);
      String value = m.group(4);
      
      if (Constants.VERSION_ATTRIBUTE.equals(attr)) {
        if (negation) {
          negatedVersions.add(value);
        } else {
          if (GREATER_EQ_OP.equals(op))
            lowerVersion = value;
          else if (LESS_EQ_OP.equals(op))
            upperVersion = value;
          else
            throw new IllegalArgumentException();
        }
      } else {
        result.put(attr, value);
      }
    }

    if (lowerVersion != null) {
      StringBuilder versionAttr = new StringBuilder(lowerVersion);
      if (upperVersion != null) {
        versionAttr.append(",")
          .append(upperVersion)
          .insert(0, negatedVersions.contains(lowerVersion) ? '(' : '[')
          .append(negatedVersions.contains(upperVersion) ? ')' : ']');
      }
      
      result.put(Constants.VERSION_ATTRIBUTE, versionAttr.toString());
    }
    return result;
  }
  
  public static Map<String,String> parseFilter(String filter) 
  {
    Map<String,String> result;
    if (filter.startsWith("(&")) {
      result = parseFilterList(filter.substring(2, filter.length()-1));
    } else {
      result = parseFilterList(filter);
    }
    return result;
  }
}

