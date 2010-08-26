package org.apache.aries.application.modelling;


/**
 * Information about a parsed blueprint reference
 */
public interface WrappedReferenceMetadata
{
  /**
   * Get the properties of the associated blueprint service
   * @return The filter, or null for no filter
   */
  String getFilter();
  
  /**
   * Get the interface required by the reference
   * @return the interface, or null if unspecified
   */
  String getInterface();
  
  /**
   * Get the component-name attribute.
   * @return Service name
   */
  String getComponentName();
 
  
  /**
   * Is this a reference list or a reference
   * @return true if a reference list
   */
  boolean isList();

  /**
   * Is this an optional reference
   * @return true if optional
   */
  boolean isOptional();

  /**
   * Get the reference's id as defined in the blueprint
   * @return the blueprint reference id
   */
  String getId();
}
