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

import com.affymetrix.genometryImpl.parsers.*;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class AffyChpParameter {
  String name;
  byte[] valueBytes;
  AffyDataType type;

  public String toString() {
    return this.getClass().toString() + 
        ":  Name: " + name + " type: " + type.affyMimeType + " value: " + getValue();
  }
  
  public Object getValue() {
    Object result = valueBytes;
    
    ByteBuffer bb = ByteBuffer.wrap(valueBytes);
    bb.order(ByteOrder.BIG_ENDIAN);
    
    switch (type) {
      case INT8:
        result = type.name() + ": " + (int) (char) valueBytes[0]; break; // untested
      case UINT8:
        result = type.name() + ": " +  (int) (char) valueBytes[0]; break; // untested (seems to be wrong)
      case INT16:
        result = type.name() + ": " + bb.getShort(); break; // untested
      case UINT16:
        result = type.name() + ": " + (int) (char) bb.getShort(); break; // untested
      case INT32:
        result = type.name() + ": " + bb.getInt(0); break; //OK
      case UINT32:
        result = type.name() + ": " + valueBytes; break;
      case FLOAT:
        result = new Float(bb.getFloat(0)); break; //OK
      case TEXT_ASCII:
        result = AffyGenericChpFile.makeString(valueBytes, AffyDataType.UTF8); break;//OK
      case TEXT_UTF16BE:
        result = AffyGenericChpFile.makeString(valueBytes, AffyDataType.UTF16); break;//OK
      default:
        result = type.name() + ": " + valueBytes; break;
    }
    
    return result;
  }

  public void dump(PrintStream str) {
    str.println(this.toString());
  }
  
}
