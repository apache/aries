package org.apache.aries.application.modelling.utils;

import java.util.Collection;
import java.util.Map;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.Provider;

/**
 * Useful functions associated with application modelling 
 *
 */
public interface ModellingHelper {

  /**
   * Check that all mandatory attributes from a Provider are specified by the consumer's attributes
   * @param consumerAttributes
   * @param p
   * @return true if all mandatory attributes are present, or no attributes are mandatory
   */
  boolean areMandatoryAttributesPresent(Map<String,String> consumerAttributes, Provider p);

  /**
   * Create an ImportedBundle from a Fragment-Host string
   * @param fragmentHostHeader
   * @return
   * @throws InvalidAttributeException
   */
  ImportedBundle buildFragmentHost(String fragmentHostHeader) throws InvalidAttributeException;
  
  /**
   * Create a new ImnportedPackage that is the intersection of the two supplied imports.
   * @param p1
   * @param p2
   * @return ImportedPackageImpl representing the intersection, or null. All attributes must match exactly.
   */
  ImportedPackage intersectPackage (ImportedPackage p1, ImportedPackage p2); 
    
  
  /**
   * Factory method for objects implementing the DeployedBundles interface
   *  
   */
  DeployedBundles createDeployedBundles (String assetName, Collection<ImportedBundle> appContentNames, 
      Collection<ImportedBundle> appUseBundleNames, Collection<ModelledResource> fakeServiceProvidingBundles);
}
