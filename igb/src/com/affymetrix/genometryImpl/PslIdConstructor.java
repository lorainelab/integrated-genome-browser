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

package com.affymetrix.genometryImpl;

import java.util.*;
/**
 *  A configurable way of constructing on-the-fly (maybe unique) id strings from UcscPslSyms.
 *  
 *  Example: id = type + "." + 
 *                queryseq.getVersion() + "_" + queryseq.getID() + ":" + getQueryMin() + "-" + getQueryMax() + "." + 
 *                targetseq.getVersion() + "_" + targetseq.getID() + ":" + getTargetMin() + "-" + getTargetMax()
 *
 *  This example would only give guaranteed unique ids if both the type is 
 *     guaranteed to be unique and for that type of annotation one never gets multiple pairwise alignments that 
 *     have the same query min/max and target min/max.
 *
 *  List to constructor for example above would be:
      { "#type", 
        ".", 
        "#qversion", "_", "#qname", ":", "#qmin", "-", "#qmax", 
        ".",
	"#tversion", "_", "#tname", ":", "#tmin", "-", "#tmax" }
        
 */
public class PslIdConstructor {
  //TODO this is unused.  Keep it?
  List chunks;

  public PslIdConstructor(List clist) {
    chunks = clist;
  }

  public String getID(UcscPslSym sym) {
    StringBuffer sbuf = new StringBuffer();
    int chunkcount = chunks.size();
    for (int i=0; i<chunkcount; i++) {
      String chunk = (String)chunks.get(i);
      if (chunk.startsWith("#")) {
	if (chunk.equals("#type")) {  sbuf.append(sym.getType()); }
	else if (chunk.equals("#qname")) { sbuf.append(sym.getQuerySeq().getID()); }
	else if (chunk.equals("#qmin")) { sbuf.append(sym.getQueryMin()); }
	else if (chunk.equals("#qmax")) { sbuf.append(sym.getQueryMax()); }
	else if (chunk.equals("#tname")) { sbuf.append(sym.getTargetSeq().getID()); }
	else if (chunk.equals("#tmin")) { sbuf.append(sym.getTargetMin()); }
	else if (chunk.equals("#tmax")) { sbuf.append(sym.getTargetMax()); }
	else if (chunk.equals("#orientation")) {
	  if (sym.getSameOrientation()) { sbuf.append("+"); }
	  else { sbuf.append("-"); }
	}
	else if (chunk.equals("#qversion")) {
	  if (sym.getQuerySeq() instanceof NibbleBioSeq) {
	    sbuf.append(((NibbleBioSeq)sym.getQuerySeq()).getVersion()); }
	  else { sbuf.append("unknown"); }
	}
	else if (chunk.equals("#tversion")) {
	  if (sym.getTargetSeq() instanceof NibbleBioSeq) { 
	    sbuf.append(((NibbleBioSeq)sym.getTargetSeq()).getVersion()); 
	  }
	  else { sbuf.append("unknown"); }
	}
	else { sbuf.append(chunk); }  // var in "#var" not recognized, so just adding "#var"
      }
      else {
	sbuf.append(chunk);
      }
    }
    return new String(sbuf);
  }

}
