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

package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.List;

import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.LocalUrlCacher;
import java.awt.geom.Rectangle2D;

/**
 *  AlignControl tries to display other seq of a pairwise alignment in
 *   another instance of IGB (by utilizing UnibrowControlServlet)
 */
public final class AlignControl implements ActionListener, ContextualPopupListener  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  SeqMapView gviewer;
  JMenuItem pushAlignUrlMI;
  JPopupMenu annot_popup;
  IGB uni;
  //  int other_igb_port = 7085;
  int other_igb_port = -1;
  boolean searched_for_igbs = false;

  boolean ADJUST_BASED_ON_VIEW = true;

  public AlignControl(IGB uni, SeqMapView smv) {
    this.uni = uni;
    gviewer = smv;
    annot_popup = gviewer.getSelectionPopup();
    //    pushAlignUrlMI = setUpMenuItem("Push align to other IGB");
    pushAlignUrlMI = new JMenuItem("Push align to other IGB");
    annot_popup.add(pushAlignUrlMI);
    pushAlignUrlMI.addActionListener(this);
    gviewer.addPopupListener(this);
  }

  public void actionPerformed(ActionEvent evt) {
    String com = evt.getActionCommand();
    System.out.println("Event: " + evt);
    if (pushAlignUrlMI.getText().equals(com)) {
      sendControlRequest();
    }
  }

  public void sendControlRequest() {
    if (! searched_for_igbs) {
      System.out.println("looking for other igb");
      other_igb_port = uni.getControlServer().findDifferentUnibrowPort();
      System.out.println("finished looking for other igb, port = " + other_igb_port);
      if (other_igb_port > 0)  { searched_for_igbs = true; }
    }
    if (other_igb_port < 0) {
      int option =
	JOptionPane.showConfirmDialog(gviewer.getFrame(),
				      "Couldn't find another IGB to talk to.  Search again?",
				      "IGB Search", JOptionPane.YES_NO_OPTION);
      if (option == JOptionPane.YES_OPTION) {
	searched_for_igbs = false;
	sendControlRequest();
      }
      else {
	return;
      }
    }
    System.out.println("trying to send control request");
    SeqSymmetry annot_sym = gviewer.getSelectedSymmetry();
    //    SeqUtils.printSymmetry(annot_sym);
    MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();

    // trying to get "other" span in symmetry (the one that doesn't map to aseq)
    SeqSpan curspan = annot_sym.getSpan(aseq);

    // base length of view in other IGB on length of view in current IGB?
    Rectangle2D.Double vbox = gviewer.getSeqMap().getViewBounds();
    int view_length = (int)vbox.width;
    
    SeqSpan other_span = FindOtherSpan(annot_sym, curspan);
    System.out.println("Other span: " + other_span);

    SeqUtils.printSpan(other_span);
    MutableAnnotatedBioSeq other_seq = other_span.getBioSeq();
    String seqid = other_seq.getID();
    String version = "unknown";
    /*
    //  commenting out version discovery because version information
    //   for query is lost in PSL files.  Therefor _must_ have correct
    //   genome version loaded in other IGB instance, because
    //   UnibrowControlServlet in other IGB will go ahead and show it on
    //   _whatever_ version is loaded in other IGB
    //
    if (other_seq instanceof NibbleBioSeq) {
      version = ((NibbleBioSeq)other_seq).getVersion();
    }
    */

    int min = other_span.getMin();
    int max = other_span.getMax();
    int selmin = min;
    int selmax = max;
    int other_length = other_span.getLength();
    // trying to adjust zoom level for other IGB based on zoom level of current IGB
    if (ADJUST_BASED_ON_VIEW && (view_length > other_length)) {
      int center = min + (other_length/2);
      min = center - (view_length/2);
      max = center + (view_length/2);
    }

    String request = "http://localhost:" + other_igb_port + "/UnibrowControl?" +
      "seqid=" + seqid + "&version=" + version +
      "&start=" + min + "&end=" + max +
      "&selectstart=" + selmin + "&selectend=" + selmax;
    System.out.println("request URL: " + request);
		
		InputStream istr = null;
    try {
      URL request_url = new URL(request);
      //      request_url.openConnection().connect();
      //      request_url.getContent();
      URLConnection con = request_url.openConnection();
			con.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
			con.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
      //      con.setDoInput(true);
      // for some reason, need to get input stream and close it to trigger
      //    "response" at other end...
      istr = con.getInputStream();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      int option =
	JOptionPane.showConfirmDialog(gviewer.getFrame(),
				      "Previous IGB unavailable, search for another IGB?",
				      "IGB Search", JOptionPane.YES_NO_OPTION);
      if (option == JOptionPane.YES_OPTION) {
	searched_for_igbs = false;
	sendControlRequest();
      }
    }
		finally {
			GeneralUtils.safeClose(istr);
		}
  }
  
  private static SeqSpan FindOtherSpan(SeqSymmetry annot_sym, SeqSpan curspan) {

        // hack to look for most common case, where the other seq will be
        //    a chromosome (and hence its id will start with "chr")
        //    need this because now starting to look at syms with breadth > 2 (like Psl3Syms)
        SeqSpan other_span = null;
        int spancount = annot_sym.getSpanCount();
        for (int i = 0; i < spancount; i++) {
            SeqSpan ospan = annot_sym.getSpan(i);
            MutableAnnotatedBioSeq oseq = ospan.getBioSeq();
            if (!SeqUtils.spansEqual(curspan, ospan)) {
                if (oseq.getID().startsWith("chr")) {
                    // found a other_span != curspan and likely on a chromosome, so use it
                    other_span = ospan;
                    break;
                }
            }
        }
        if (other_span == null) {
            // no other spans with with seq id that looks like a chromosome,
            //    so just take first one thats not equal...
            for (int i = 0; i < spancount; i++) {
                SeqSpan ospan = annot_sym.getSpan(i);
                if (!SeqUtils.spansEqual(curspan, ospan)) {
                    other_span = ospan;
                    break;
                }
            }
        }
        if (other_span == null) {
            // looks like there _are_ no other unequal spans, so default to same span
            other_span = curspan;
        }

        return other_span;
    }


  /**
   *  implementing ContextualPopupListener to dynamicly modify
   *  right-click popup on SeqMapView to add a curation menu
   */
  public void popupNotify(JPopupMenu popup, List selected_items, SeqSymmetry primary_sym) {
    if (selected_items.size() == 1) {
      SeqSymmetry selected_sym = (SeqSymmetry)selected_items.get(0);
      int spancount = selected_sym.getSpanCount();
      int chrom_count = 0;
      for (int i=0; i<spancount; i++) {
	MutableAnnotatedBioSeq bseq = selected_sym.getSpan(i).getBioSeq();
	if (bseq.getID().startsWith("chr")) {
	  chrom_count++;
	}
      }
      if (chrom_count >= 2) {
	popup.add(pushAlignUrlMI);
      }
    }
  }

}
