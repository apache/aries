package org.apache.aries.application.modelling;



public enum ResourceType {BUNDLE, PACKAGE, SERVICE, COMPOSITE, OTHER;
  /**
   * An enum class to represent the resource type, such as bundle, package, service etc.
   */
  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }
}