package org.apache.aries.application.modelling;


import java.util.Collection;



/**
 * A simple data structure containing two immutable Collections, 
 * one each of ImportedServiceImpl and ExportedServiceImpl
 */
public interface  ParsedServiceElements 
{
 
 


  /**
   * Get the ImportedServices
   * @return imported services
   */
  public Collection<ImportedService> getReferences();

  /**
   * Get the exported services
   * @return exported services
   */
  public Collection<ExportedService> getServices();
}