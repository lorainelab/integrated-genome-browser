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

package com.affymetrix.genometryImpl;

import java.util.*;
/**
 *  A configurable way of constructing on-the-fly link
 *  Strings from UcscPslSym's.
 *
 *
 *  Example: link = "http://genome.ucsc.edu/cgi-bin/hgc?g=blastzBestMouse&i=chr11:78468037-78468062+chr22:22943527-22943552&db=hg12"
 *           link = "http://genome.ucsc.edu/cgi-bin/hgc?" + "g=" + type +
 *                    "&i=" + queryseq.getID() + ":" + getQueryMin() + "-" + getQueryMax() +
 *                     "+" +  targetseq.getID() + ":" + getTargetMin() + "-" + getTargetMax() + "&db=hg12"
 *
 *
 *  in current system, list to pass in to addLink() for example above would be:
   { "http://genome.ucsc.eud/cgi-bin/hgc?",
     "g=",
     "#type",
     "&i=",
     "#qname",
     ":",
     "#qmin",
     "-",
     "#qmax",
     "+",
     "#tname",
     ":",
     "#tmin",
     "-",
     "#tmax",
     "&db=hg12" }



 */
public class PslLinkConstructor {
  List<List<String>> link_chunklists = new ArrayList<List<String>>();
  List<String> link_labels = new ArrayList<String>();
  Map<String,List<String>> label2chunks = new HashMap<String,List<String>>();

  public PslLinkConstructor()  {
  }

  public void addLink(String link_label, List<String> chunklist) {
    link_labels.add(link_label);
    link_chunklists.add(chunklist);
    label2chunks.put(link_label, chunklist);
  }

  public int getLinkCount() { return link_labels.size(); }

  public String getLinkLabel(int index) {
    return link_labels.get(index);
  }

  public String getLink(int link_index, UcscPslSym sym) {
    return this.getLink(link_chunklists.get(link_index), sym);
  }

  public String getLink(String link_label, UcscPslSym sym) {
    return this.getLink(label2chunks.get(link_label), sym);
  }

  protected String getLink(List<String> chunks, UcscPslSym sym) {
    StringBuffer sbuf = new StringBuffer();
    int chunkcount = chunks.size();
    for (int i=0; i<chunkcount; i++) {
      String chunk = chunks.get(i);
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
