/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.parsers;

import java.io.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.igb.genometry.*;

public class ProbeSetParser {

  public static void main(String[] args) {
    String test_file = args[0];
    ProbeSetParser tester = new ProbeSetParser();
    try {
      FileInputStream fis = new FileInputStream(new File(test_file));
      MutableAnnotatedBioSeq aseq = new SimpleCompAnnotBioSeq("testseq", 50222333);
      tester.readBinary(fis, aseq);
    }
    catch (Exception ex) { ex.printStackTrace(); }
  }

  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq aseq) {
    return readBinary(istr, aseq);
  }

  public MutableAnnotatedBioSeq readBinary(InputStream istr, MutableAnnotatedBioSeq aseq) {
    try {
      System.out.println("ProbeSetParser starting parsing");
      BufferedInputStream bis = null;
      if (istr instanceof BufferedInputStream) {
	bis = (BufferedInputStream)istr;
      }
      else {
	bis = new BufferedInputStream(istr);
      }
      DataInputStream dis = new DataInputStream(bis);
      int set_count = 0;
      SimpleSymWithProps method_sym = new SimpleSymWithProps();
      method_sym.setProperty("method", "U133 Probe Sets");
      method_sym.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
      while (set_count < 621) {
	String set_name = dis.readUTF();
	SimpleSymWithProps setsym = new SimpleSymWithProps();
	setsym.setProperty("id", set_name);
	setsym.setProperty("method", "U133 Probe Sets");
        setsym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
	//	System.out.println("" + set_count + ", " + set_name);
	int probe_count = dis.readInt();
	for (int k=0; k<probe_count; k++) {
	  int pos = dis.readInt();
	  SeqSymmetry probespan = new SingletonSeqSymmetry(pos, pos+25, aseq);
	  setsym.addChild(probespan);
	}
	SeqSpan setspan = SeqUtils.getChildBounds(setsym, aseq);
	setsym.addSpan(setspan);
	method_sym.addChild(setsym);
	set_count++;
      }
      aseq.addAnnotation(method_sym);
      System.out.println("ProbeSetParser finished parsing");
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    return aseq;
  }

}
