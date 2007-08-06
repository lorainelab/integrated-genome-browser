/**
*   Copyright (c) 2007 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genometryImpl.parsers.gchp;

import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.util.*;

/** A parser for the Affymetrix Generic CHP format. */
public class AffyGenericChpFile {
  
  int magic; // magic number.  Always 59.
  int version; // version number.  Always 1.
  int num_groups;
  int group_0_pos;
  Map<String,AffyChpParameter> parameterMap = new LinkedHashMap<String,AffyChpParameter>();
  AffyGenericDataHeader header;
  List<AffyDataGroup> groups;

  /** Creates a new instance of AffyCnChpParser */
  protected AffyGenericChpFile() {
  }
  
  /** Parses a string in UTF-16BE format, with the length specified first as an int. */
  public static String parseWString(DataInputStream istr) throws IOException {
    int len = istr.readInt();
    byte bytes[] = new byte[len * 2];
    istr.readFully(bytes);
    return makeString(bytes, AffyDataType.UTF16);
  }
  
  /** Parses a string in UTF-8 format, with the length specified first as an int. */
  public static CharSequence parseString(DataInputStream istr) throws IOException {
    int len = istr.readInt();
    byte bytes[] = new byte[len];
    istr.readFully(bytes);
    return makeString(bytes, AffyDataType.UTF8);
  }
    
  /** Parses the file.  Does not close the stream. 
   *  @param headerOnly if true, will read the complete header, but will not
   *  read any data groups.
   */
  public static AffyGenericChpFile parse(InputStream istr, boolean headerOnly) throws IOException  {

    AffyGenericChpFile chpFile = new AffyGenericChpFile();
    
    DataInputStream dis = new DataInputStream(istr);
    
    chpFile.magic = dis.readUnsignedByte();
    chpFile.version = dis.readUnsignedByte();
    
    if (chpFile.magic != 59) {
      throw new IOException("Error in chp file format: wrong magic number: " + chpFile.magic);
    }
    
    chpFile.num_groups = dis.readInt();
    chpFile.group_0_pos = dis.readInt(); // TODO: signed vs unsigned?
        
    chpFile.parameterMap = new LinkedHashMap<String,AffyChpParameter>();
    chpFile.header = AffyGenericDataHeader.readHeader(dis);
    
    chpFile.groups = new ArrayList<AffyDataGroup>(chpFile.num_groups);
    if (! headerOnly) {
      for (int i=0; i<chpFile.num_groups; i++) {
        AffyDataGroup group = AffyDataGroup.parse(dis);
        chpFile.groups.add(group);
      }
    }
    
    return chpFile;
  }
  
  /** Dumps a description of the file to the given stream. */
  public void dump(PrintStream str) {    
    str.println("Magic: " + magic + ", version:" + version);
    str.println("Number of Groups: " + num_groups);
    str.println("Group 0 position: " + group_0_pos);
    str.println("Parameters: " + parameterMap.size());

    for (AffyChpParameter param : parameterMap.values()) {
      str.println(param.toString());
    }

    header.dump(str);
    
    for (AffyDataGroup group : groups) {
      List<AffyDataSet> dataSets = group.getDataSets();
      for (AffyDataSet set : dataSets) {
        set.dump(str);
      }
    }
  }
    
  /** Creates a String from the given bytes, using the given Charset and
   *  trimming off any trailing '\0' characters.
   */
  public static String makeString(byte[] bytes, Charset charset) {
    String s = null;
    try {
      //TODO: use new String(byte[], Charset) when we convert all to JDK 1.6
      s = new String(bytes, charset.name());
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
      return "String could not be parsed: charset " + charset.name() + " not known";
    }
    int index = s.indexOf('\0');
    if (index >= 0) {
      s = new String(s.substring(0, index)); // new String() potentially saves memory
    }
    return s;
  }

  
  public void testFullRead(String test_file) {
    FileInputStream fis = null;
    AffyGenericChpFile chpFile = null;

    try {
      File fil = new File(test_file);      
      fis = new FileInputStream(fil);
      System.out.println("START Parse");
      chpFile = parse(fis, false);
      System.out.println("END Parse");
      System.out.println("");
      
      chpFile.dump(System.out);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {fis.close();} catch (Exception e) { e.printStackTrace(); }
    }
  }

  /** A method useful during debugging. */
  private static void showBytes(DataInputStream istr, int count) throws IOException {
    byte bytes[] = new byte[count];
    istr.readFully(bytes);
    System.out.println("Length: " + count);
      try{
        //TODO: use new String(byte[], Charset) when we convert all to JDK 1.6
        System.out.print(new String(bytes, AffyDataType.UTF8.name()));
      } catch (Exception e) {
        e.printStackTrace();
      }
    showBytes(bytes);
  }
  
  /** A method useful during debugging. */
  private static void showBytes(byte[] bytes) {
    System.out.println("");
    for (int i=0; i<bytes.length; i++) {
        System.out.print("(" + new Integer(bytes[i]) + ") ");
    }
    System.out.println("");
  }
 
  /** Tests the parsing of the given filename. */
  public static void main(String[] args) {
    AffyGenericChpFile parser = new AffyGenericChpFile();
    String fileName = "C:\\Documents and Settings\\eerwin\\My Documents\\NA06985_GW6_C.cnchp";
    if (args.length > 0) {
      fileName = args[0];
    }
    System.out.println("Testing reading of file: " + fileName);
    parser.testFullRead(fileName);
  }

  /** Reades the affymetrix-algorithm-param-genome-version header value.
   *  @return a CharSequence or null
   */
  public CharSequence getHeaderVersion() {
    AffyChpParameter param = header.paramMap.get("affymetrix-algorithm-param-genome-version");    
    if (param == null) {
      return null;
    } else {
      return param.getValueString();
    }
  }
}
