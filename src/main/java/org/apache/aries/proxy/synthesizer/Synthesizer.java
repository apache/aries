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

package org.apache.aries.proxy.synthesizer;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * The Synthesizer class can be run from a java command with arguments
 * of paths to class files that should be modified to have the synthetic
 * attribute added.
 *
 */
public class Synthesizer
{
  /**
   * This is the main method for running the Synthesizer
   * 
   * @param args - String[] of file paths to class files
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception
  {
    //add the synthetic modifier for each of the supplied args
    for (String arg : args) {
      FileInputStream classInStream = null;
      ClassWriter writer = null;
     
      try {
        //read in the class
        classInStream = new FileInputStream(arg);
        ClassReader reader = new ClassReader(classInStream);
        //make a ClassWriter constructed with the reader for speed
        //since we are mostly just copying
        //we just need to override the visit method so we can add
        //the synthetic modifier, otherwise we use the methods in
        //a standard writer
        writer =   new ClassWriter(reader, 0) ;
        ClassVisitor cv = new CustomClassVisitor((ClassVisitor)writer);
        //call accept on the reader to start the visits
        //using the writer we created as the visitor
        reader.accept(cv, 0);
      } finally {
        //close the InputStream if it is hanging around
        if (classInStream != null) classInStream.close();
      }
      FileOutputStream classOutStream = null;
      try {
        //write out the new bytes of the class file
        classOutStream = new FileOutputStream(arg);
        if (writer != null) classOutStream.write(writer.toByteArray());
      } finally {
        //close the OutputStream if it is still around
        if (classOutStream != null) classOutStream.close();
      }
    }
  }
  
  public static class CustomClassVisitor extends ClassVisitor
  {

    public CustomClassVisitor( ClassVisitor cv)
    {
      super(Opcodes.ASM5, cv);
      
    }
    @Override
    public void visit(int version, int access, String name, String signature,
        String superName, String[] interfaces)
    {
      cv.visit(version, access | Opcodes.ACC_SYNTHETIC, name, signature, superName,
          interfaces);
    }

  }
}
