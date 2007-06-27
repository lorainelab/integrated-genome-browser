/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.util;

import java.io.*;
import java.net.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.NibbleBioSeq;


public class UnibrowControlUtils {

  public static void sendLocationCommand(String igb_loc, SeqSpan span) {
    BioSeq seq = span.getBioSeq();
    String seqid = seq.getID();
    int min = span.getMin();
    int max = span.getMax();
    boolean seq_versioned = (seq instanceof NibbleBioSeq);

    if (seq_versioned) {
      String version = ((NibbleBioSeq)seq).getVersion();
      //  String request = "http://localhost:" + other_igb_port + "/UnibrowControl?" + 
      String request = igb_loc + "/UnibrowControl?" + 
	"seqid=" + seqid + "&version=" + version + 
	"&start=" + min + "&end=" + max;

      System.out.println("request URL: " + request);
      try {
	URL request_url = new URL(request);
	URLConnection con = request_url.openConnection();
	//      con.setDoInput(true);
	// for some reason, need to get input stream and close it to trigger 
	//    "response" at other end...
	InputStream istr = con.getInputStream();
	istr.close();
      }
      catch (Exception ex) {
	ex.printStackTrace();
	System.out.println("problem connecting to requested IGB instance");
      }
    }

  }

}
