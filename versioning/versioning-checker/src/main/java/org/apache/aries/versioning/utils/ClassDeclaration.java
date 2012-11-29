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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

public class ClassDeclaration extends GenericDeclaration {

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
    private final Map<String, FieldDeclaration> fieldsInUpperChain = new HashMap<String, FieldDeclaration>();
    private final Collection<String> supers = new ArrayList<String>();


    private final URLClassLoader jarsLoader;

    private final SerialVersionClassVisitor serialVisitor;

    public Map<String, FieldDeclaration> getFields() {
        return fields;
    }

    public Map<String, FieldDeclaration> getAllFields() {
        Map<String, FieldDeclaration> allFields = new HashMap<String, FieldDeclaration>(getFields());
        Map<String, FieldDeclaration> fieldsFromSupers = getFieldsInUpperChain();
        putIfAbsent(allFields, fieldsFromSupers);


        return allFields;
    }

    private void putIfAbsent(Map<String, FieldDeclaration> allFields,
                             Map<String, FieldDeclaration> fieldsFromSupers) {
        for (Map.Entry<String, FieldDeclaration> superFieldEntry : fieldsFromSupers.entrySet()) {
            String fieldName = superFieldEntry.getKey();
            FieldDeclaration fd = superFieldEntry.getValue();
            if (allFields.get(fieldName) == null) {
                allFields.put(fieldName, fd);
            }

        }
    }

    /**
     * Get the methods in the current class plus the methods in the upper chain
     *
     * @return map of method name to set of method declarations
     */
    public Map<String, Set<MethodDeclaration>> getAllMethods() {

        Map<String, Set<MethodDeclaration>> methods = new HashMap<String, Set<MethodDeclaration>>(getMethods());
        Map<String, Set<MethodDeclaration>> methodsFromSupers = getMethodsInUpperChain();
        for (Map.Entry<String, Set<MethodDeclaration>> superMethodsEntry : methodsFromSupers.entrySet()) {
            Set<MethodDeclaration> overloadingMethods = methods.get(superMethodsEntry.getKey());
            if (overloadingMethods != null) {
                overloadingMethods.addAll(superMethodsEntry.getValue());
            } else {
                methods.put(superMethodsEntry.getKey(), superMethodsEntry.getValue());
            }

        }


        return methods;
    }

    public Map<String, Set<MethodDeclaration>> getMethods() {
        return methods;
    }


//    public ClassDeclaration(int access, String name, String signature, String superName,
//                            String[] interfaces, URLClassLoader loader) {
//        super(access, name, signature);
//        this.superName = superName;
//        this.interfaces = interfaces;
//        this.fields = new HashMap<String, FieldDeclaration>();
//        this.methods = new HashMap<String, Set<MethodDeclaration>>();
//        this.jarsLoader = loader;
//        this.serialVisitor = null;
//    }

    public ClassDeclaration(int access, String name, String signature, String superName,
                            String[] interfaces, URLClassLoader loader, SerialVersionClassVisitor cv) {
        super(access, name, signature);
        this.superName = superName;
        this.interfaces = interfaces;
        this.fields = new HashMap<String, FieldDeclaration>();
        this.methods = new HashMap<String, Set<MethodDeclaration>>();
        this.jarsLoader = loader;
        this.serialVisitor = cv;
    }

    private void getFieldsRecursively(String superClass) {

        if ((superClass != null)) {
            // load the super class of the cd
            try {
                SerialVersionClassVisitor cv = new SerialVersionClassVisitor(null);
                SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader, cv);
                ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(superClass + SemanticVersioningUtils.classExt));
                cr.accept(svc, 0);
                ClassDeclaration cd = svc.getClassDeclaration();
                if (cd != null) {
                    addFieldInUpperChain(cd.getFields());
                    getFieldsRecursively(cd.getSuperName());
                    for (String iface : cd.getInterfaces()) {
                        getFieldsRecursively(iface);
                    }
                }
            } catch (IOException ioe) {
                // not a problem
            }
        }
    }

    private void getMethodsRecursively(String superClass) {
        if ((superClass != null)) {
            // load the super class of the cd
            SerialVersionClassVisitor cv = new SerialVersionClassVisitor(null);

            SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader, cv);
            // use URLClassLoader to load the class
            try {
                ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(superClass + SemanticVersioningUtils.classExt));
                cr.accept(svc, 0);
                ClassDeclaration cd = svc.getClassDeclaration();
                if (cd != null) {
                    addMethodsInUpperChain(cd.getMethods());
                    getMethodsRecursively(cd.getSuperName());
                    for (String iface : cd.getInterfaces()) {
                        getMethodsRecursively(iface);
                    }
                }
            } catch (IOException ioe) {
                // not a deal
            }
        }
    }


    public Map<String, FieldDeclaration> getFieldsInUpperChain() {
        if (fieldsInUpperChain.isEmpty()) {
            getFieldsRecursively(getSuperName());
            for (String ifs : getInterfaces()) {
                getFieldsRecursively(ifs);
            }
        }
        return fieldsInUpperChain;
    }

    private void addFieldInUpperChain(Map<String, FieldDeclaration> fields) {
        putIfAbsent(fieldsInUpperChain, fields);

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

        if (className != null) {
            // load the super class of the cd
            SerialVersionClassVisitor cv = new SerialVersionClassVisitor(null);

            SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader, cv);
            try {
                ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(className + SemanticVersioningUtils.classExt));
                cr.accept(svc, 0);
                clazz.add(className);
                if (svc.getClassDeclaration() != null) {
                    String superName = svc.getClassDeclaration().getSuperName();
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

    public String getSuperName() {
        return superName;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public void addFields(FieldDeclaration fd) {
        fields.put(fd.getName(), fd);
    }


    public void addMethods(MethodDeclaration md) {
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

        BinaryCompatibilityStatus reasons = new BinaryCompatibilityStatus();
        if (old == null) {
            return reasons;
        }
        getClassSignatureBinaryCompatibleStatus(old, reasons);
        getAllMethodsBinaryCompatibleStatus(old, reasons);
        getAllFieldsBinaryCompatibleStatus(old, reasons);
        getAllSuperPresentStatus(old, reasons);
        getSerializableBackCompatable(old, reasons);
        return reasons;
    }


    public boolean isAbstract() {
        return Modifier.isAbstract(getAccess());
    }

    private void getClassSignatureBinaryCompatibleStatus(ClassDeclaration originalClass, List<String> reasons) {
        // if a class was not abstract but changed to abstract
        // not final changed to final
        // public changed to non-public
        String prefix = " The class " + getName();
        if (!!!originalClass.isAbstract() && isAbstract()) {
            reasons.add(prefix + " was not abstract but is changed to be abstract.");
        }
        if (!!!originalClass.isFinal() && isFinal()) {
            reasons.add(prefix + " was not final but is changed to be final.");
        }
        if (originalClass.isPublic() && !!!isPublic()) {
            reasons.add(prefix + " was public but is changed to be non-public.");
        }
    }

    private void getAllFieldsBinaryCompatibleStatus(ClassDeclaration originalClass, List<String> reasons) {
        // for each field to see whether the same field has changed
        // not final -> final
        // static <-> nonstatic
        Map<String, FieldDeclaration> oldFields = originalClass.getAllFields();
        Map<String, FieldDeclaration> newFields = getAllFields();
        areFieldsBinaryCompatible(oldFields, newFields, reasons);
    }

    private void areFieldsBinaryCompatible(Map<String, FieldDeclaration> oldFields, Map<String, FieldDeclaration> currentFields, List<String> reasons) {

        for (Map.Entry<String, FieldDeclaration> entry : oldFields.entrySet()) {
            FieldDeclaration bef_fd = entry.getValue();
            FieldDeclaration cur_fd = currentFields.get(entry.getKey());

            isFieldBinaryCompatible(reasons, bef_fd, cur_fd);
        }
    }

    private boolean isFieldBinaryCompatible(List<String> reasons,
                                            FieldDeclaration bef_fd, FieldDeclaration cur_fd) {
        String fieldName = bef_fd.getName();
        //only interested in the public or protected fields

        boolean compatible = true;

        if (bef_fd.isPublic() || bef_fd.isProtected()) {
            String prefix = "The " + (bef_fd.isPublic() ? "public" : "protected") + " field " + fieldName;


            if (cur_fd == null) {
                reasons.add(prefix + " has been deleted.");
                compatible = false;
            } else {

                if ((!!!bef_fd.isFinal()) && (cur_fd.isFinal())) {
                    // make sure it has not been changed to final
                    reasons.add(prefix + " was not final but has been changed to be final.");
                    compatible = false;

                }
                if (bef_fd.isStatic() != cur_fd.isStatic()) {
                    // make sure it the static signature has not been changed
                    reasons.add(prefix + " was static but is changed to be non static or vice versa.");
                    compatible = false;
                }
                // check to see the field type is the same
                if (!isFieldTypeSame(bef_fd, cur_fd)) {
                    reasons.add(prefix + " has changed its type.");
                    compatible = false;

                }
                if (SemanticVersioningUtils.isLessAccessible(bef_fd, cur_fd)) {
                    // check whether the new field is less accessible than the old one
                    reasons.add(prefix + " becomes less accessible.");
                    compatible = false;
                }

            }
        }
        return compatible;
    }

    /**
     * Return whether the serializable class is binary compatible. The serial verison uid change breaks binary compatibility.
     *
     *
     * @param old Old class declaration
     * @param reasons list of binary compatibility problems
     */
    private void getSerializableBackCompatable(ClassDeclaration old, List<String> reasons) {
        // It does not matter one of them is not serializable.
        if ((getAllSupers().contains(SemanticVersioningUtils.SERIALIZABLE_CLASS_IDENTIFIER)) && (old.getAllSupers().contains(SemanticVersioningUtils.SERIALIZABLE_CLASS_IDENTIFIER))) {
            // check to see whether the serializable id is the same
            //ignore if it is enum
            if ((!getAllSupers().contains(SemanticVersioningUtils.ENUM_CLASS) && (!old.getAllSupers().contains(SemanticVersioningUtils.ENUM_CLASS)))) {
                long oldValue = getSerialVersionUID(old);
                long curValue = getSerialVersionUID(this);
                if ((oldValue != curValue)) {
                    reasons.add("The serializable class is no longer back compatible as the value of SerialVersionUID has changed from " + oldValue + " to " + curValue + ".");
                }
            }
        }

    }

    private Long getSerialVersionUID(ClassDeclaration cd) {
        FieldDeclaration serialID = cd.getAllFields().get(SemanticVersioningUtils.SERIAL_VERSION_UTD);
        if (serialID != null) {
            if (serialID.isFinal() && serialID.isStatic() && Type.LONG_TYPE.equals(Type.getType(serialID.getDesc()))) {
                if (serialID.getValue() != null) {
                    return (Long) (serialID.getValue());
                }
            }
        }
        // get the generated value
        return cd.getSerialVisitor() == null? null: cd.getSerialVisitor().getComputeSerialVersionUID();
    }

    private boolean isFieldTypeSame(FieldDeclaration bef_fd, FieldDeclaration cur_fd) {
        boolean descSame = bef_fd.getDesc().equals(cur_fd.getDesc());
        if (descSame) {
            // check whether the signatures are the same
            if ((bef_fd.getSignature() == null) && (cur_fd.getSignature() == null)) {

                return true;
            }
            if ((bef_fd.getSignature() != null) && (bef_fd.getSignature().equals(cur_fd.getSignature()))) {
                return true;
            }
        }
        return false;

    }

    private void getAllMethodsBinaryCompatibleStatus(ClassDeclaration originalClass, List<String> reasons) {
        //  for all methods
        // no methods should have deleted
        // method return type has not changed
        // method changed from not abstract -> abstract
        Map<String, Set<MethodDeclaration>> oldMethods = originalClass.getAllMethods();
        Map<String, Set<MethodDeclaration>> newMethods = getAllMethods();
        areMethodsBinaryCompatible(oldMethods, newMethods, reasons);
    }

    private void areMethodsBinaryCompatible(
            Map<String, Set<MethodDeclaration>> oldMethods, Map<String, Set<MethodDeclaration>> newMethods, List<String> reasons) {

        boolean compatible = true;
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
            outer:
            for (MethodDeclaration md : oldMDSigs) {
                String mdName = md.getName();

                String prefix = "The " + SemanticVersioningUtils.getReadableMethodSignature(mdName, md.getDesc());
                if (md.isProtected() || md.isPublic()) {
                    boolean found = false;
                    if (newMDSigs != null) {
                        // try to find it in the current class
                        for (MethodDeclaration new_md : newMDSigs) {
                            // find the method with the same return type, parameter list
                            if ((md.equals(new_md))) {
                                found = true;
                                // If the old method is final but the new one is not or vice versa
                                // If the old method is static but the new one is non static
                                // If the old method is not abstract but the new is

                                if (!!!Modifier.isFinal(md.getAccess()) && !!!Modifier.isStatic(md.getAccess()) && Modifier.isFinal(new_md.getAccess())) {
                                    compatible = false;
                                    reasons.add(prefix + " was not final but has been changed to be final.");
                                }
                                if (Modifier.isStatic(md.getAccess()) != Modifier.isStatic(new_md.getAccess())) {
                                    compatible = false;
                                    reasons.add(prefix + " has changed from static to non-static or vice versa.");
                                }
                                if ((Modifier.isAbstract(new_md.getAccess())) && (!Modifier.isAbstract(md.getAccess()))) {
                                    compatible = false;
                                    reasons.add(prefix + " has changed from non abstract to abstract.");
                                }
                                if (SemanticVersioningUtils.isLessAccessible(md, new_md)) {
                                    compatible = false;
                                    reasons.add(prefix + " is less accessible.");
                                }

                                if (compatible) {
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
                    if (!found) {
                        if (!isMethodInSuperClass(md)) {

                            compatible = false;
                            reasons.add(prefix + " has been deleted or its return type or parameter list has changed.");
                        } else {
                            if (newMDSigs != null) {
                                for (MethodDeclaration new_md : newMDSigs) {
                                    // find the method with the same return type, parameter list
                                    if ((md.equals(new_md))) {
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
        }

        // Check the newly added method has not caused binary incompatibility
        for (Map.Entry<String, Collection<MethodDeclaration>> extraMethodSet : extraMethods.entrySet()) {
            for (MethodDeclaration md : extraMethodSet.getValue()) {
                String head = "The " + SemanticVersioningUtils.getReadableMethodSignature(md.getName(), md.getDesc());
                isNewMethodSpecialCase(md, head, reasons);
            }
        }
    }

    /**
     * Return the newly added fields
     *
     * @param old old class declaration
     * @return FieldDeclarations for fields added to new class
     */
    public Collection<FieldDeclaration> getExtraFields(ClassDeclaration old) {
        Map<String, FieldDeclaration> oldFields = old.getAllFields();
        Map<String, FieldDeclaration> newFields = getAllFields();
        Map<String, FieldDeclaration> extraFields = new HashMap<String, FieldDeclaration>(newFields);
        for (String key : oldFields.keySet()) {
            extraFields.remove(key);
        }
        return extraFields.values();
    }

    /**
     * Return the extra non-private methods
     *
     * @param old old class declaration
     * @return method declarations for methods added to new class
     */
    public Collection<MethodDeclaration> getExtraMethods(ClassDeclaration old) {
        // Need to find whether there are new methods added.
        Collection<MethodDeclaration> extraMethods = new HashSet<MethodDeclaration>();
        Map<String, Set<MethodDeclaration>> currMethodsMap = getAllMethods();
        Map<String, Set<MethodDeclaration>> oldMethodsMap = old.getAllMethods();

        for (Map.Entry<String, Set<MethodDeclaration>> currMethod : currMethodsMap.entrySet()) {
            String methodName = currMethod.getKey();
            Collection<MethodDeclaration> newMethods = currMethod.getValue();

            // for each  method, we look for whether it exists in the old class
            Collection<MethodDeclaration> oldMethods = oldMethodsMap.get(methodName);
            for (MethodDeclaration new_md : newMethods) {
                if (!new_md.isPrivate()) {
                    if (oldMethods == null) {
                        extraMethods.add(new_md);
                    } else {
                        if (!oldMethods.contains(new_md)) {
                            extraMethods.add(new_md);
                        }
                    }
                }
            }
        }
        return extraMethods;
    }

    public boolean isMethodInSuperClass(MethodDeclaration md) {
        // scan the super class and interfaces
        String methodName = md.getName();
        Collection<MethodDeclaration> overloaddingMethods = getMethodsInUpperChain().get(methodName);
        if (overloaddingMethods != null) {
            for (MethodDeclaration value : overloaddingMethods) {
                // method signature and name same and also the method should not be less accessible
                if (md.equals(value) && (!!!SemanticVersioningUtils.isLessAccessible(md, value)) && (value.isStatic() == md.isStatic())) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * The newly added method is less accessible than the old one in the super or is a static (respectively instance) method.
     *
     *
     * @param md method declaration
     * @param prefix beginning of incompatibility message
     * @param reasons list of binary incompatibility reasons
     * @return whether new method is less accessible or changed static-ness compared to old class
     */
    private boolean isNewMethodSpecialCase(MethodDeclaration md, String prefix, List<String> reasons) {
        // scan the super class and interfaces
        String methodName = md.getName();
        boolean special = false;
        Collection<MethodDeclaration> overloaddingMethods = getMethodsInUpperChain().get(methodName);
        if (overloaddingMethods != null) {
            for (MethodDeclaration value : overloaddingMethods) {
                // method signature and name same and also the method should not be less accessible
                if (!SemanticVersioningUtils.CONSTRUTOR.equals(md.getName())) {
                    if (md.equals(value)) {
                        if (SemanticVersioningUtils.isLessAccessible(value, md)) {
                            special = true;
                            reasons.add(prefix + " is less accessible than the same method in its parent.");
                        }
                        if (value.isStatic()) {
                            if (!md.isStatic()) {
                                special = true;
                                reasons.add(prefix + " is non-static but the same method in its parent is static.");
                            }
                        } else {
                            if (md.isStatic()) {
                                special = true;
                                reasons.add(prefix + " is static but the same method is its parent is not static.");
                            }
                        }
                    }
                }
            }
        }
        return special;
    }

    private void getAllSuperPresentStatus(ClassDeclaration old, List<String> reasons) {
        Collection<String> oldSupers = old.getAllSupers();
        boolean containsAll = getAllSupers().containsAll(oldSupers);
        if (!!!containsAll) {
            oldSupers.removeAll(getAllSupers());
            reasons.add("The superclasses or superinterfaces have stopped being super: " + oldSupers.toString() + ".");
        }
    }

    public SerialVersionClassVisitor getSerialVisitor() {
        return serialVisitor;
    }

}
