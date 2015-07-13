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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;

public class SemanticVersioningUtils {

    public static final String classExt = ".class";
    public static final String javaExt = ".java";
    public static final String schemaExt = ".xsd";
    public static final String jarExt = ".jar";

    
    public static final String CONSTRUTOR = "<init>";
    public static final String MAJOR_CHANGE = "major";
    public static final String MINOR_CHANGE = "minor";
    public static final String NO_CHANGE = "no";
    
    public static final String oneLineBreak = "\r\n";
    public static final String twoLineBreaks = oneLineBreak + oneLineBreak;
    public static final String PROPERTY_FILE_IDENTIFIER = "java/util/ListResourceBundle";
    public static final String CLINIT = "<clinit>";
    public static final String SERIALIZABLE_CLASS_IDENTIFIER = "java/io/Serializable";
    public static final String SERIAL_VERSION_UTD = "serialVersionUID";
    public static final String ENUM_CLASS = "java/lang/Enum";
    public static final int ASM4 = Opcodes.ASM4;

    public static boolean isLessAccessible(GenericDeclaration before, GenericDeclaration after) {

        if (before.getAccess() == after.getAccess()) {
            return false;
        }
        //When it reaches here, the two access are different. Let's make sure the whether the after field has less access than the before field.
        if (before.isPublic()) {
            if (!!!after.isPublic()) {
                return true;
            }
        } else if (before.isProtected()) {
            if (!!!(after.isPublic() || after.isProtected())) {
                return true;
            }
        } else {
            if (!!!before.isPrivate()) {
                // the field is package level.
                if (after.isPrivate()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * ASM Type descriptor look up table
     *
     * @author emily
     */
    private enum TypeDescriptor{
        I("int"), Z("boolean"), C("char"), B("byte"),
                S("short"), F("float"), J("long"), D("double"), V("void");

        String desc;
        TypeDescriptor(String desc)
        {
            this.desc = desc;
        }

    String getDesc() {
        return desc;
    }

    private static final Map<String, TypeDescriptor> stringToEnum = new HashMap<String, TypeDescriptor>();

    static {
        for (TypeDescriptor td : values()) {
            stringToEnum.put(td.toString(), td);
        }
    }

    public static TypeDescriptor fromString(String symbol) {
        return stringToEnum.get(symbol);
    }

}

    /**
     * Transform ASM method desc to a human readable form
     * Method declaration in source file Method descriptor
     * <pre>
     * void m(int i, float f) &lt;= (IF)V
     * int m(Object o) &lt;= (Ljava/lang/Object;)I
     * int[] m(int i, String s) &lt;= (ILjava/lang/String;)[I
     * Object m(int[] i) &lt;= ([I)Ljava/lang/Object;
     * </pre>
     */
    public static String getReadableMethodSignature(String methodName, String methodDesc) {
        // need to find the return type first, which is outside the ()
        int lastBrace = methodDesc.lastIndexOf(")");

        // parameter
        StringBuilder methodSignature = new StringBuilder();
        if (lastBrace == -1) {
            // this is odd, don't attempt to transform. Just return back. Won't happen unless byte code weaving is not behaving.
            return "method " + methodName + methodDesc;
        }
        String param = methodDesc.substring(1, lastBrace);
        if (CONSTRUTOR.equals(methodName)) {
            //This means the method is a constructor. In the binary form, the constructor carries a name 'init'. Let's use the source
            // code proper name
            methodSignature.append("constructor with parameter list ");
        } else {
            String returnType = methodDesc.substring(lastBrace + 1);
            methodSignature.append("method ");
            methodSignature.append(transform(returnType));
            methodSignature.append(" ");
            methodSignature.append(methodName);
        }
        // add the paramether list
        methodSignature.append("(");
        methodSignature.append(transform(param));
        methodSignature.append(")");
        return methodSignature.toString();
    }

    public static String transform(String asmDesc) {
        String separator = ", ";
        int brkCount = 0;
        StringBuilder returnStr = new StringBuilder();
        //remove the '['s

        while (asmDesc.length() > 0) {
            while (asmDesc.startsWith("[")) {
                asmDesc = asmDesc.substring(1);
                brkCount++;
            }
            while (asmDesc.startsWith("L")) {
                //remove the L and ;
                int semiColonIndex = asmDesc.indexOf(";");


                if (semiColonIndex == -1) {
                    //This is odd. The asm binary code is invalid. Do not attempt to transform.
                    return asmDesc;
                }
                returnStr.append(asmDesc.substring(1, semiColonIndex));
                asmDesc = asmDesc.substring(semiColonIndex + 1);
                for (int index = 0; index < brkCount; index++) {
                    returnStr.append("[]");
                }
                brkCount = 0;
                returnStr.append(separator);
            }

            TypeDescriptor td = null;
            while ((asmDesc.length() > 0) && (td = TypeDescriptor.fromString(asmDesc.substring(0, 1))) != null) {

                returnStr.append(td.getDesc());
                for (int index = 0; index < brkCount; index++) {
                    returnStr.append("[]");
                }
                brkCount = 0;
                returnStr.append(separator);
                asmDesc = asmDesc.substring(1);
            }


        }
        String finalStr = returnStr.toString();
        if (finalStr.endsWith(separator)) {
            finalStr = finalStr.substring(0, finalStr.lastIndexOf(separator));
        }
        //replace "/" with "." as bytecode uses / in the package names
        finalStr = finalStr.replaceAll("/", ".");
        return finalStr;
    }

    /**
     * Return whether the binary is property file. If the binary implements the interface of java.util.ListResourceBundle
     *
     * @param cd
     * @return
     */
    public static boolean isPropertyFile(ClassDeclaration cd) {
        Collection<String> supers = cd.getAllSupers();
        return (supers.contains(PROPERTY_FILE_IDENTIFIER));
    }


}
