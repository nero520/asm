/**
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000,2002,2003 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.objectweb.asm.attrs;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Annotation data contains an annotated type and its array of the element-value
 * pairs. Structure is in the following format:
 * <pre>
 *   annotation {
 *     u2 type_index;
 *     u2 num_element_value_pairs;
 *     {
 *       u2 element_name_index;
 *       element_value value;
 *     } element_value_pairs[num_element_value_pairs];
 *   }
 * </pre>
 * The items of the annotation structure are as follows:
 * <dl>
 * <dt>type_index</dt>
 * <dd>The value of the type_index item must be a valid index into the constant_pool
 *     table. The constant_pool entry at that index must be a CONSTANT_Utf8_info
 *     structure representing a field descriptor representing the annotation 
 *     interface corresponding to the annotation represented by this annotation 
 *     structure.</dd>
 * <dt>num_element_value_pairs</dt>
 * <dd>The value of the num_element_value_pairs item gives the number of element-value
 *     pairs in the annotation represented by this annotation structure. Note that a
 *     maximum of 65535 element-value pairs may be contained in a single annotation.</dd>
 * <dt>element_value_pairs</dt>
 * <dd>Each value of the element_value_pairs table represents a single element-value
 *     pair in the annotation represented by this annotation structure.
 *     Each element_value_pairs entry contains the following two items:
 *     <dt>element_name_index</dt>
 *     <dd>The value of the element_name_index item must be a valid index into the
 *         constant_pool table. The constant_pool entry at that index must be a
 *         CONSTANT_Utf8_info structure representing the name of the annotation type
 *         element corresponding to this element_value_pairs entry.</dd>
 *     <dt>value</dt>
 *     <dd>The value item represents the value in the element-value pair represented by
 *         this element_value_pairs entry.</dd>
 *     </dl>
 *     </dd>
 * </dl>
 *
 * @see <a href="http://www.jcp.org/en/jsr/detail?id=175">JSR 175 : A Metadata
 * Facility for the Java Programming Language</a>
 *
 * @author Eugene Kuleshov
 */

public class Annotation {

  public String type;

  public List elementValues = new ArrayList();

  public void add (String name, Object value) {
    elementValues.add(new Object[]{name, value});
  }

  /**
   * Reads annotation data structures.
   *
   * @param cr the class that contains the attribute to be read.
   * @param off index of the first byte of the data structure.
   * @param buf buffer to be used to call {@link ClassReader#readUTF8 readUTF8},
   *      {@link ClassReader#readClass(int,char[]) readClass} or {@link
   *      ClassReader#readConst readConst}.
   *
   * @return offset position in bytecode after reading annotation
   */

  public int read (ClassReader cr, int off, char[] buf) {
    type = cr.readUTF8(off, buf);
    int numElementValuePairs = cr.readUnsignedShort(off + 2);
    off += 4;
    for (int i = 0; i < numElementValuePairs; i++) {
      String elementName = cr.readUTF8(off, buf);
      AnnotationElementValue value = new AnnotationElementValue();
      off = value.read(cr, off + 2, buf);
      elementValues.add(new Object[]{elementName, value});
    }
    return off;
  }

  /**
   * Writes annotation data structures.
   *
   * @param bv the byte array form to store data structures.
   * @param cw the class to which this attribute must be added. This parameter
   *      can be used to add to the constant pool of this class the items that
   *      corresponds to this attribute.
   */

  public void write (ByteVector bv, ClassWriter cw) {
    bv.putShort(cw.newUTF8(type));
    bv.putShort(elementValues.size());
    for (int i = 0; i < elementValues.size(); i++) {
      Object[] value = (Object[])elementValues.get(i);
      bv.putShort(cw.newUTF8((String)value[0]));
      ((AnnotationElementValue)value[1]).write(bv, cw);
    }
  }

  /**
   * Utility method to read List of annotations. Each element of annotations
   * List will have Annotation instance.
   *
   * @param annotations the List to store parameters annotations.
   * @param cr the class that contains the attribute to be read.
   * @param off index of the first byte of the data structure.
   * @param buf buffer to be used to call {@link ClassReader#readUTF8 readUTF8},
   *      {@link ClassReader#readClass(int,char[]) readClass} or {@link
   *      ClassReader#readConst readConst}.
   *
   * @return offset position in bytecode after reading annotations
   */

  public static int readAnnotations (
    List annotations, ClassReader cr, int off, char[] buf) {
    int size = cr.readUnsignedShort(off);
    off += 2;
    for (int i = 0; i < size; i++) {
      Annotation ann = new Annotation();
      off = ann.read(cr, off, buf);
      annotations.add(ann);
    }
    return off;
  }

  /**
   * Utility method to read List of parameters annotations.
   *
   * @param parameters the List to store parameters annotations.
   *     Each element of the parameters List will have List of Annotation
   *     instances.
   * @param cr the class that contains the attribute to be read.
   * @param off index of the first byte of the data structure.
   * @param buf buffer to be used to call {@link ClassReader#readUTF8 readUTF8},
   *      {@link ClassReader#readClass(int,char[]) readClass} or {@link
   *      ClassReader#readConst readConst}.
   */

  public static void readParameterAnnotations (
    List parameters, ClassReader cr, int off, char[] buf) {
    int numParameters = cr.b[off++] & 0xff;
    for (int i = 0; i < numParameters; i++) {
      List annotations = new ArrayList();
      off = Annotation.readAnnotations(annotations, cr, off, buf);
      parameters.add(annotations);
    }
  }

  /**
   * Utility method to write List of annotations.
   *
   * @param bv the byte array form to store data structures.
   * @param annotations the List of annotations to write.
   *     Elements should be instances of the Annotation class.
   * @param cw the class to which this attribute must be added. This parameter
   *     can be used to add to the constant pool of this class the items that
   *     corresponds to this attribute.
   *
   * @return the byte array form with saved annotations.
   */

  public static ByteVector writeAnnotations (ByteVector bv,
                                             List annotations, ClassWriter cw) {
    bv.putShort(annotations.size());
    for (int i = 0; i < annotations.size(); i++) {
      ((Annotation)annotations.get(i)).write(bv, cw);
    }
    return bv;
  }

  /**
   * Utility method to write List of parameters annotations.
   *
   * @param bv the byte array form to store data structures.
   * @param parameters the List of parametars to write. Elements should be
   *     instances of the List that contains instances of the Annotation class.
   * @param cw the class to which this attribute must be added. This parameter
   *     can be used to add to the constant pool of this class the items that
   *     corresponds to this attribute.
   *
   * @return the byte array form with saved annotations.
   */

  public static ByteVector writeParametersAnnotations (ByteVector bv,
                                                       List parameters,
                                                       ClassWriter cw) {
    bv.putByte(parameters.size());
    for (int i = 0; i < parameters.size(); i++) {
      writeAnnotations(bv, (List)parameters.get(i), cw);
    }
    return bv;
  }

  /**
   * Returns annotation values in the format described in JSR-175 for Java
   * source code.
   * 
   * @param annotations a list of annotations.
   * @return annotation values in the format described in JSR-175 for Java
   *      source code.
   */

  public static String stringAnnotations (List annotations) {
    StringBuffer sb = new StringBuffer();
    if (annotations.size() > 0) {
      for (int i = 0; i < annotations.size(); i++) {
        sb.append('\n').append(annotations.get(i));
      }
    }
    return sb.toString();
  }

  /**
   * Returns parameter annotation values in the format described in JSR-175
   * for Java source code.
   * 
   * @param parameters a list of parameter annotations.
   * @return parameter annotation values in the format described in JSR-175
   *      for Java source code.
   */

  public static String stringParameterAnnotations (List parameters) {
    StringBuffer sb = new StringBuffer();
    String sep = "";
    for (int i = 0; i < parameters.size(); i++) {
      sb.append(sep).append(stringAnnotations((List)parameters.get(i)));
      sep = ", ";
    }
    return sb.toString();
  }

  /**
   * Returns value in the format described in JSR-175 for Java source code.
   * 
   * @return value in the format described in JSR-175 for Java source code.
   */

  public String toString () {
    StringBuffer sb = new StringBuffer("@").append(type);
    // shorthand syntax for marker annotation
    if (elementValues.size() > 0) {
      sb.append(" ( ");
      String sep = "";
      for (int i = 0; i < elementValues.size(); i++) {
        Object[] value = (Object[])elementValues.get(i);
        // using shorthand syntax for single-element annotation
        if ( !( elementValues.size()==1 || "value".equals( elementValues.get( 0)))) {
          sb.append(sep).append(value[0]).append(" = ");
        }
        sb.append(value[1]);
        sep = ", ";
      }
      sb.append(" )");
    }
    return sb.toString();
  }
}
