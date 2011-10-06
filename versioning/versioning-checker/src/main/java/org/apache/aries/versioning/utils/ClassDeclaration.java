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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.versioning.utils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;


public class ClassDeclaration extends GenericDeclaration
{

  // Binary Compatibility - deletion of package-level access field/method/constructors of classes and interfaces in the package
  // will not break binary compatibility when an entire package is updated.

  // Assumptions:
  // 1.  This tool assumes that the deletion of package-level fields/methods/constructors is not break binary compatibility 
  // based on the assumption of the entire package is updated.
  // 



  private final String superName;
  private final String[] interfaces;
  private final Map<String, FieldDeclaration> fields;
  private final Map<String, Set<MethodDeclaration>> methods;

  private final Map<String, Set<MethodDeclaration>> methodsInUpperChain = new HashMap<String, Set<MethodDeclaration>>();
  private final Map<String, Collection<FieldDeclaration>> fieldsInUpperChain = new HashMap<String, Collection<FieldDeclaration>>();
  private final Collection<String> supers = new ArrayList<String> ();


  private final URLClassLoader jarsLoader;

  private final BinaryCompatibilityStatus binaryCompatible = new BinaryCompatibilityStatus(true, null);
  public Map<String, FieldDeclaration> getFields()
  {
    return fields;
  }
  /**
   * Get the methods in the current class plus the methods in the upper chain
   * @return
   */
  public Map<String, Set<MethodDeclaration>> getAllMethods() {

    Map<String, Set<MethodDeclaration>> methods = new HashMap<String, Set<MethodDeclaration>>(getMethods());
    methods.putAll(getMethodsInUpperChain());
    return methods;
  }

  public Map<String, Set<MethodDeclaration>> getMethods()
  {
    return methods;
  }


  public ClassDeclaration(int access, String name, String signature, String superName,
      String[] interfaces, URLClassLoader loader)
  {
    super(access, name, signature);
    this.superName = superName;
    this.interfaces = interfaces;
    this.fields = new HashMap<String, FieldDeclaration>();
    this.methods = new HashMap<String, Set<MethodDeclaration>>();
    this.jarsLoader = loader;
  }

  private void getFieldsRecursively(String superClass) {

    if ((superClass != null) ) {
      // load the super class of the cd
      try {
        SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader);
        ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(superClass + SemanticVersioningUtils.classExt));
        if (cr != null) {
          cr.accept(svc, 0);
          ClassDeclaration cd = svc.getClassDeclaration();
          if (cd != null) {
            addFieldInUpperChain(cd.getFields());
            getFieldsRecursively(cd.getSuperName());
            for (String iface : cd.getInterfaces()) {
              getFieldsRecursively(iface);
            }
          }
        }
      } catch (IOException ioe) {
        // not a problem
      }
    }
  }

  private void getMethodsRecursively(String superClass)
  {
    if ((superClass != null) ) {
      // load the super class of the cd
      SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader);
      // use URLClassLoader to load the class
      try {
        ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(superClass  + SemanticVersioningUtils.classExt));
        if (cr !=  null) {
          cr.accept(svc, 0);
          ClassDeclaration cd = svc.getClassDeclaration();
          if (cd != null) {
            addMethodsInUpperChain(cd.getMethods());
            getMethodsRecursively(cd.getSuperName());
            for (String iface : cd.getInterfaces()) {
              getMethodsRecursively(iface);
            }
          }
        }
      } catch (IOException ioe) {
        // not a deal
      }
    }
  }


  public Map<String, Collection<FieldDeclaration>> getFieldsInUpperChain() {
    if (fieldsInUpperChain.isEmpty()) {
      getFieldsRecursively(getSuperName());
      for (String ifs : getInterfaces()) {
        getFieldsRecursively(ifs);
      }
    }
    return fieldsInUpperChain;
  }

  private void addFieldInUpperChain(Map<String, FieldDeclaration> fields) {
    for (Map.Entry<String, FieldDeclaration> field : fields.entrySet()) {
      String fieldName = field.getKey();
      Set<FieldDeclaration> fds = new HashSet<FieldDeclaration>();
      if (fieldsInUpperChain.get(fieldName) != null) {
        fds.addAll(fieldsInUpperChain.get(fieldName));
      } 

      fds.add(fields.get(fieldName));
      fieldsInUpperChain.put(fieldName, fds);
    }
  }
  public Map<String, Set<MethodDeclaration>> getMethodsInUpperChain() {
    if (methodsInUpperChain.isEmpty()) {
      getMethodsRecursively(getSuperName());
      for (String ifs : getInterfaces()) {
        getMethodsRecursively(ifs);
      }
    }
    return methodsInUpperChain;
  }

  private void addMethodsInUpperChain(Map<String, Set<MethodDeclaration>> methods) {
    for (Map.Entry<String, Set<MethodDeclaration>> method : methods.entrySet()) {
      String methodName = method.getKey();
      Set<MethodDeclaration> mds = new HashSet<MethodDeclaration>();
      if (methodsInUpperChain.get(methodName) != null) {
        mds.addAll(methodsInUpperChain.get(methodName));
      }
      mds.addAll(method.getValue());
      methodsInUpperChain.put(methodName, mds);
    }
  }
  public Collection<String> getUpperChainRecursively(String className) {
    Collection<String> clazz = new HashSet<String>();

    if (className!= null)  {
      // load the super class of the cd
      SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader);
      try {
        ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(className + SemanticVersioningUtils.classExt));
        cr.accept(svc, 0);
        clazz.add(className);
        if (svc.getClassDeclaration() != null) {
          String superName = svc.getClassDeclaration().getSuperName();
          className = superName;
          clazz.addAll(getUpperChainRecursively(superName));
          if (svc.getClassDeclaration().getInterfaces() != null) {
            for (String iface : svc.getClassDeclaration().getInterfaces()) {
              clazz.addAll(getUpperChainRecursively(iface));
            }
          }
        }
      } catch (IOException ioe) {
        // not to worry about this. terminate.
      }
    }
    return clazz;
  }

  public Collection<String> getAllSupers() {
    if (supers.isEmpty()) {
      supers.addAll(getUpperChainRecursively(getSuperName()));
      for (String iface : getInterfaces()) {
        supers.addAll(getUpperChainRecursively(iface));
      }
    }
    return supers;
  }
  public String getSuperName()
  {
    return superName;
  }
  public String[] getInterfaces()
  {
    return interfaces;
  }

  public void addFields(FieldDeclaration fd) {
    fields.put(fd.getName(), fd);
  }


  public void addMethods(MethodDeclaration md)
  {
    String key = md.getName();
    Set<MethodDeclaration> overloaddingMethods = methods.get(key);
    if (overloaddingMethods != null) {
      overloaddingMethods.add(md);
      methods.put(key, overloaddingMethods);
    } else {
      Set<MethodDeclaration> mds = new HashSet<MethodDeclaration>();
      mds.add(md);
      methods.put(key, mds);
    }
  }


  public BinaryCompatibilityStatus getBinaryCompatibleStatus(ClassDeclaration old) {
    // check class signature, fields, methods
    if (old == null) {
      return binaryCompatible;
    }
    BinaryCompatibilityStatus bcs = getClassSignatureBinaryCompatibleStatus(old);
    if (!!!bcs.isCompatible()) {
      return bcs;
    } else {
      bcs = getAllMethodsBinaryCompatibleStatus(old);
      if (!!!bcs.isCompatible()) {
        return bcs;
      } else {
        bcs = getFieldsBinaryCompatibleStatus(old);
        if (!!!bcs.isCompatible()) {
          return bcs;
        } else {
          bcs = areAllSuperPresent(old);
          if (!!!bcs.isCompatible()) {
            return bcs;
          }
        }
      }
    }
    return binaryCompatible;

  }


  public boolean isAbstract() {
    return Modifier.isAbstract(getAccess());
  }

  private BinaryCompatibilityStatus getClassSignatureBinaryCompatibleStatus(ClassDeclaration originalClass) {
    // if a class was not abstract but changed to abstract
    // not final changed to final
    // public changed to non-public
    StringBuilder reason = new StringBuilder("The class "  + getName() );
    boolean compatible = false;
    if (!!!originalClass.isAbstract() && isAbstract()) {
      reason.append(" was not abstract but is changed to be abstract.") ;
    } else if (!!!originalClass.isFinal() && isFinal()){
      reason.append( " was not final but is changed to be final.");
    } else if (originalClass.isPublic() && !!! isPublic()) {
      reason.append(" was public but is changed to be non-public.");
    } else {
      compatible = true;
    }
    return new BinaryCompatibilityStatus(compatible, compatible? null: reason.toString());
  }

  public BinaryCompatibilityStatus getFieldsBinaryCompatibleStatus(ClassDeclaration originalClass) {
    // for each field to see whether the same field has changed
    // not final -> final
    // static <-> nonstatic

    for (Map.Entry<String, FieldDeclaration> entry : originalClass.getFields().entrySet()) {

      FieldDeclaration bef_fd = entry.getValue();
      FieldDeclaration cur_fd = getFields().get(entry.getKey());

      String fieldName = bef_fd.getName();
      //only interested in the public or protected fields
      boolean compatible = true;
      if (bef_fd.isPublic() || bef_fd.isProtected()) {
        StringBuilder reason = new StringBuilder("The public or protected field "  +fieldName);

        if (cur_fd == null) {
          reason.append(" has been deleted.");
          compatible =  false;
        } else if ((!!!bef_fd.isFinal()) && (cur_fd.isFinal())) {
          // make sure it has not been changed to final
          reason.append(" was not final but has been changed to be final.");
          compatible = false;

        } else if (bef_fd.isStatic() != cur_fd.isStatic()) {
          // make sure it the static signature has not been changed
          reason.append( " was static but is changed to be non static or vice versa.");
          compatible = false;
        }
        // check to see the field type is the same 
        else if (!!!Type.getType(bef_fd.getDesc()).equals(Type.getType(cur_fd.getDesc()))) {
          reason.append(" has different type.");
          compatible = false;

        } else if (SemanticVersioningUtils.isLessAccessible(bef_fd, cur_fd)) {
          // check whether the new field is less accessible than the old one         
          reason.append(" is less accessible.");
          compatible = false;
        }
        if (!!!compatible) {
          return new BinaryCompatibilityStatus(compatible, reason.toString());
        }
      }
    }
    // need to check the newly added fields do not cause binary compatibility issue:
    // e.g. the new fields has less access than the old one
    // the new field is static(instance) while the old one is instance(static) respectively.
    Collection<String> curFields = getFields().keySet();
    Collection<String> oldFields = originalClass.getFields().keySet();
    curFields.removeAll(oldFields);
    Map<String, Collection<FieldDeclaration>> superFields = new HashMap<String, Collection<FieldDeclaration>>();
    if (!!!(curFields.isEmpty())) {
      superFields = getFieldsInUpperChain();
    }
    // for each new field we need to find out whether it may cause binary incompatibility
    for ( String newFieldName : curFields) {
      // check whether the new field has the same field name in the super with the same type
      boolean existInSuper = false;
      if (superFields.containsKey(newFieldName)) {
        FieldDeclaration newfd = getFields().get(newFieldName);
        Collection<FieldDeclaration> superfd = superFields.get(newFieldName);
        FieldDeclaration oldfd = null;
        if ((superfd != null) ) {
          for (FieldDeclaration fd : superfd) {
            if (newfd.equals(fd)) {
              oldfd = fd;
              existInSuper = true;
              break;
            }
          }
        }
        if ((existInSuper) && ((SemanticVersioningUtils.isLessAccessible(oldfd, newfd)) || (oldfd.isStatic() != newfd.isStatic()))){

          return new BinaryCompatibilityStatus(false, "The new field " + newfd.getName() + " conflicts with the same field in its super class. For more details, check the Binary Compatibility section(Chapter 13) of the Java Specification.");
        }
      }
    }
    return binaryCompatible;
  }

  private BinaryCompatibilityStatus getAllMethodsBinaryCompatibleStatus(ClassDeclaration originalClass) {
    //  for all methods
    // no methods should have deleted
    // method return type has not changed
    // method changed from not abstract -> abstract
    Map<String, Set<MethodDeclaration>> oldMethods = originalClass.getMethods();
    Map<String, Set<MethodDeclaration>> newMethods = getMethods();
    return  areMethodsBinaryCompatible(oldMethods, newMethods) ;
  }

  public BinaryCompatibilityStatus areMethodsBinaryCompatible(
      Map<String, Set<MethodDeclaration>> oldMethods, Map<String, Set<MethodDeclaration>> newMethods)
  {

    Map<String, Collection<MethodDeclaration>> extraMethods = new HashMap<String, Collection<MethodDeclaration>>();

    for (Map.Entry<String, Set<MethodDeclaration>> me : newMethods.entrySet()) {
      Collection<MethodDeclaration> mds = new ArrayList<MethodDeclaration>(me.getValue());
      extraMethods.put(me.getKey(), mds);
    }

    for (Map.Entry<String, Set<MethodDeclaration>> methods : oldMethods.entrySet()) {
      // all overloading methods, check against the current class
      String methodName = methods.getKey();
      Collection<MethodDeclaration> oldMDSigs = methods.getValue();
      // If the method cannot be found in the current class, it means that it has been deleted.
      Collection<MethodDeclaration> newMDSigs = newMethods.get(methodName);
      // for each overloading methods
      outer: for (MethodDeclaration md : oldMDSigs) {
        String mdName = md.getName();
        StringBuilder reason = new StringBuilder("The "  + SemanticVersioningUtils.getReadableMethodSignature(mdName, md.getDesc()) );
        if (md.isProtected() || md.isPublic()) {
          if (newMDSigs !=  null) {
            // try to find it in the current class
            for (MethodDeclaration new_md : newMDSigs) {
              // find the method with the same return type, parameter list 
              if ((md.equals(new_md))) {
                // If the old method is final but the new one is not or vice versa
                // If the old method is static but the new one is non static
                // If the old method is not abstract but the new is


                boolean compatible = true;
                if ( !!!Modifier.isFinal(md.getAccess()) && !!!Modifier.isStatic(md.getAccess()) && Modifier.isFinal(new_md.getAccess())) {
                  compatible = false;
                  reason.append(" was not final but has been changed to be final.");
                } else if (Modifier.isStatic(md.getAccess()) != Modifier.isStatic(new_md.getAccess())){
                  compatible = false;
                  reason.append(" has changed from static to non-static or vice versa.");
                } else if ((Modifier.isAbstract(new_md.getAccess()) == true) && (Modifier.isAbstract(md.getAccess()) == false)) {
                  compatible = false;
                  reason.append( " has changed from non abstract to abstract. ");
                }
                else if (SemanticVersioningUtils.isLessAccessible(md, new_md)) {
                  compatible = false;
                  reason.append(" is less accessible.");
                }
                if (!!!compatible) {
                  return new BinaryCompatibilityStatus(compatible, reason.toString());
                }
                else {
                  // remove from the extra map
                  Collection<MethodDeclaration> mds = extraMethods.get(methodName);
                  mds.remove(new_md);
                  extraMethods.put(methodName, mds);
                  continue outer;
                }
              }
            }
          }

          // 
          // if we are here, it means that we have not found the method with the same description and signature
          // which means that the method has been deleted. Let's make sure it is not moved to its upper chain.
          if (!!!isMethodInSuperClass(md)) {

            reason.append(" has been deleted or its return type or parameter list has changed.");
            return new BinaryCompatibilityStatus(false, reason.toString());


          } else {
            if (newMDSigs != null) {
              for (MethodDeclaration new_md : newMDSigs) {
                // find the method with the same return type, parameter list 
                if ((md.equals(new_md)))  {
                  Collection<MethodDeclaration> mds = extraMethods.get(methodName);
                  mds.remove(new_md);
                  extraMethods.put(methodName, mds);
                }
              }
            }
          }
        }
      }
    }

    // Check the newly added method has not caused binary incompatibility
    for (Map.Entry<String, Collection<MethodDeclaration>> extraMethodSet : extraMethods.entrySet()){
      for (MethodDeclaration md : extraMethodSet.getValue()) {
        if (isNewMethodSpecialCase(md)){
          String reason = "The newly added " + SemanticVersioningUtils.getReadableMethodSignature(md.getName(), md.getDesc()) + " conflicts with the same method in its super class. For more details, check the Binary Compatibility section(Chapter 13) of the Java Specification.";
          return new BinaryCompatibilityStatus(false, reason);
        }
      }
    }


    return  binaryCompatible;
  }
  public MethodDeclaration getExtraMethods(ClassDeclaration old ) {
    // Need to find whether there are new abstract methods added.

    if (Modifier.isAbstract(getAccess())) {
      Map<String, Set<MethodDeclaration>> currMethodsMap = getAllMethods();

      Map<String, Set<MethodDeclaration>> oldMethodsMap = old.getAllMethods();
      // only interested in an abstract class

      for (Map.Entry<String, Set<MethodDeclaration>> currMethod : currMethodsMap.entrySet()) {
        String methodName = currMethod.getKey();
        Collection<MethodDeclaration> newMethods = currMethod.getValue();

        // for each abstract method, we look for whether it exists in the old class
        Collection<MethodDeclaration> oldMethods = oldMethodsMap.get(methodName);
        for (MethodDeclaration new_md : newMethods) {
          if (oldMethods == null) {
            return new_md;
          } else {
            if (oldMethods.contains(new_md)) {
              continue;
            } else {
              return new_md;
            }
          }
        }
      }
      // if we reach here, it means we have scanned all methods in the new classes. All of them are in the original class. No new method is added!
      return null;
    }
    // not to worry as it is not abstract class:o
    return null;
  }

  public boolean isMethodInSuperClass(MethodDeclaration md){
    // scan the super class and interfaces
    String methodName = md.getName();
    Collection<MethodDeclaration> overloaddingMethods = getMethodsInUpperChain().get(methodName);
    if (overloaddingMethods != null) {
      for (MethodDeclaration value : overloaddingMethods) {
        // method signature and name same and also the method should not be less accessible
        if (md.equals(value) && (!!!SemanticVersioningUtils.isLessAccessible(md, value)) && (value.isStatic()==md.isStatic())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * The newly added method is less accessible than the old one in the super or is a static (respectively instance) method.
   * @param md
   * @return
   */
  public boolean isNewMethodSpecialCase(MethodDeclaration md){
    // scan the super class and interfaces
    String methodName = md.getName();
    Collection<MethodDeclaration> overloaddingMethods = getMethodsInUpperChain().get(methodName);
    if (overloaddingMethods != null) {
      for (MethodDeclaration value : overloaddingMethods) {
        // method signature and name same and also the method should not be less accessible
        if (md.equals(value) && (SemanticVersioningUtils.isLessAccessible(md, value) || value.isStatic()!=md.isStatic())) {
          return true;
        }
      }
    }
    return false;
  }
  public BinaryCompatibilityStatus areAllSuperPresent(ClassDeclaration old) {
    Collection<String> oldSupers = old.getAllSupers();
    boolean containsAll = getAllSupers().containsAll(oldSupers);
    if (!!!containsAll) {
      oldSupers.removeAll(getAllSupers());
      return new BinaryCompatibilityStatus(false, "The superclasses or superinterfaces have stopped being super: " + oldSupers.toString());
    }
    return binaryCompatible;
  }
}
