/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package tutorial.genoviz;

import java.applet.*;
import java.awt.*;
import java.io.*;
import java.util.*;

import com.affymetrix.genoviz.widget.NeoAssembler;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.glyph.AlignedResiduesGlyph;


public class TutorialAssmblr extends Applet {

  NeoAssembler assmblr;

  public void init() {
    assmblr = new NeoAssembler();
    assmblr.setAutoSort(false);
    NeoPanel pan = new NeoPanel();
    pan.setLayout(new BorderLayout());
    this.setLayout(new BorderLayout());
    pan.add("Center", assmblr);
    this.add("Center", pan);
    String param;
    param = getParameter("assembly");
    if (param != null) {
      parseInputString(param);
    }

  }

  public void parseInputString(String theSource) {
    this.assmblr.clearWidget();
    try {
      parseInput(new BufferedReader(new StringReader(theSource)));
    }
    catch (Exception e) {
    }
    this.assmblr.updateWidget();
  }

  public void parseInput(Reader in) throws Exception {
    BufferedReader buf = new BufferedReader(in);
    boolean first_seq = true;
    String line;
    StringBuffer params = new StringBuffer();
    int offset = 0;
    int numlines;
    boolean forward = true;

    while ((line = buf.readLine()) != null) {
      params.append ( line );
    }

    StringTokenizer tokenize = new StringTokenizer ( params.toString(), "\\", false );

    while ( tokenize.hasMoreTokens() ) {
      line = tokenize.nextToken();
      String gapped_residues = line.substring(line.indexOf(':')+1);
      if (first_seq) {
        String name = line.substring(0, line.indexOf(' '));
        assmblr.setGappedConsensus(name, gapped_residues,
            offset, forward);
        first_seq = false;
      }
      else {
        String name = line.substring(1, line.indexOf(' '));
        assmblr.addGappedSequence(name, gapped_residues, offset, forward);
      }
    }
  }

}
