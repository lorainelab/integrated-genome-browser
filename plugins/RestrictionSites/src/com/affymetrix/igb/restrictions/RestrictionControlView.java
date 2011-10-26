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
package com.affymetrix.igb.restrictions;

import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.DNAUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

public final class RestrictionControlView extends IGBTabPanel
				implements ListSelectionListener, ActionListener {
	private static final long serialVersionUID = 0;

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("restrictions");
	private static final int TAB_POSITION = 8;
	private final Map<String,String> site_hash = new HashMap<String,String>();
	private JList siteList;
	private JPanel labelP;
	private final List<String> sites = new ArrayList<String>();
	private static Color colors[] = {
		Color.magenta,
		new Color(0x00cd00),
		Color.orange,
		new Color(0x00d7d7),
		new Color(0xb50000),
		Color.blue,
		Color.gray,
		Color.pink};//Distinct Colors for View/Print Ease
	private JLabel labels[];
	private JRPButton actionB;
	private JRPButton clearB;

	/**
	 *  keep track of added glyphs
	 */
	private final List<GlyphI> glyphs = new ArrayList<GlyphI>();

	public RestrictionControlView(IGBService igbService) {
		super(igbService, BUNDLE.getString("restrictionSitesTab"), BUNDLE.getString("restrictionSitesTab"), false, TAB_POSITION);
		boolean load_success = true;

		String rest_file = "/rest_enzymes";
		InputStream file_input_str =
						RestrictionControlView.class.getResourceAsStream(rest_file);
		
		if (file_input_str == null) {
			ErrorHandler.errorPanel("Cannot open restriction enzyme file",
							"Cannot find restriction enzyme file '" + rest_file + "'.\n" +
							"Restriction mapping will not be available.");
		}

		BufferedReader d = null;

		if (file_input_str == null) {
			load_success = false;
		} else {
			try {
				//Loading the name of all the restriction sites to GUI
				d = new BufferedReader(new InputStreamReader(file_input_str));
				StringTokenizer string_toks;
				String site_name, site_dna;
				String reply_string;
				//    String reply_string = distr.readLine();
				//int rcount = 0;
				while ((reply_string = d.readLine()) != null) {
					//	System.out.println(reply_string);
					string_toks = new StringTokenizer(reply_string);
					site_name = string_toks.nextToken();
					site_dna = string_toks.nextToken();
					site_hash.put(site_name, site_dna);
					sites.add(site_name);
					//rcount++;
				}
			} catch (Exception ex) {
				load_success = false;
				ErrorHandler.errorPanel("Problem loading restriction site file, aborting load\n" +
								ex.toString());
			} finally {
				GeneralUtils.safeClose(d);
				GeneralUtils.safeClose(file_input_str);
			}
		}

		if (load_success) {
			siteList = new JList(sites.toArray());
			JScrollPane scrollPane = new JScrollPane(siteList);
			labelP = new JPanel();
			labelP.setBackground(Color.white);
			labelP.setLayout(new GridLayout(sites.size(), 1));

			labels = new JLabel[sites.size()];
			JLabel label;
			for (int i = 0; i < labels.length; i++) {//Make a label for the selected pane for each restriction enzyme
				label = new JLabel();
				label.setForeground(colors[i%colors.length]);//We're repeating the colors..deal with it, users.
				label.setText("           ");
				labelP.add(label);
				labels[i] = label;
			}

			this.setLayout(new BorderLayout());
			scrollPane.setPreferredSize(new Dimension(100, 100));

			this.add("West", scrollPane);
			actionB = new JRPButton("RestrictionControlView_actionB", BUNDLE.getString("action"));
			clearB = new JRPButton("RestrictionControlView_clearB", BUNDLE.getString("clear"));
			this.add("Center", new JScrollPane(labelP));

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new GridLayout(2, 1));
			buttonPanel.add(actionB);
			buttonPanel.add(clearB);
			this.add("South", buttonPanel);

			siteList.addListSelectionListener(this);
			actionB.addActionListener(this);
			clearB.addActionListener(this);
		} else {
			this.setLayout(new BorderLayout());
			JLabel lab = new JLabel(BUNDLE.getString("notAvailable"));
			this.add("North", lab);
		}
	}

	@Override
	public TabState getDefaultState() {
		return TabState.COMPONENT_STATE_RIGHT_TAB;
	}

	public void valueChanged(ListSelectionEvent evt) {
		Object src = evt.getSource();
		if (src == siteList) {
			Object[] selected_names = siteList.getSelectedValues();
			for (int i = 0; i < labels.length; i++) {
				if (i < selected_names.length) {
					labels[i].setText((String) (selected_names[i]));
				} else {
					labels[i].setText("");
				}
			}
		}
	}

	private void clearAll() {
		clearGlyphs();
		siteList.clearSelection();
	}

	private void clearGlyphs() {
		igbService.getSeqMap().removeItem(glyphs);
		glyphs.clear();
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == clearB) {
			clearAll();
			igbService.getSeqMap().updateWidget();
			return;
		}

		clearGlyphs();

		final BioSeq vseq = igbService.getSeqMapView().getViewSeq();
		if (vseq == null) {
			ErrorHandler.errorPanel("No Sequence selected. Please select a seqeunce.");
			return;
		}

		final SeqSpan span = igbService.getSeqMapView().getVisibleSpan();	
		boolean loadResidue = false;
		
		if(!vseq.isAvailable(span)){
			loadResidue = igbService.confirmPanel("Residues for " + vseq.getID()
					+ " not loaded.  \nDo you want to load residues?");
			if (!loadResidue) {
				return;
			}
		}
		
		ThreadUtils.getPrimaryExecutor(this).execute(new Thread(new GlyphifyMatchesThread(vseq, span, loadResidue)));
	}

	
	private class GlyphifyMatchesThread implements Runnable
	{
		final SeqSpan span;
		final boolean loadResidues;
		final BioSeq vseq;
		
		GlyphifyMatchesThread(BioSeq vseq, SeqSpan span, boolean loadResidues){
			this.vseq = vseq;
			this.span = span;
			this.loadResidues = loadResidues;
		}
		
		public void run()
		{
			try{
				if(loadResidues){
					igbService.loadResidues(span, true);
					igbService.getSeqMapView().setAnnotatedSeq(vseq, true, true, true);
				}
				
				igbService.addNotLockedUpMsg("Finding Restriction Sites... ");
				if (vseq == null || !vseq.isAvailable(span)) {
					ErrorHandler.errorPanel("Residues for seq not available, search aborted.");
					return;
				}
				int residue_offset = vseq.getMin();
				String residues = vseq.getResidues();
				// Search for reverse complement of query string
				String rev_searchstring = DNAUtils.reverseComplement(residues);

				for (int i = 0; i < labels.length; i++) {
					String site_name = labels[i].getText();
					// done when hit first non-labelled JLabel
					if (site_name == null || site_name.equals("")) {
						break;
					}
					String site_residues = site_hash.get(site_name);
					if (site_residues == null) {
						continue;
					}
					Pattern regex = null;
					try {
						regex = Pattern.compile(site_residues, Pattern.CASE_INSENSITIVE);
					} catch (Exception ex) {
						ex.printStackTrace();
						continue;
					}

					System.out.println("searching for occurrences of \"" + site_residues + "\" in sequence");

					residue_offset = vseq.getMin();
					int hit_count1 = igbService.searchForRegexInResidues(
							true, regex, residues, residue_offset, glyphs, colors[i % colors.length]);

					// Search for reverse complement of query string
					//   flip searchstring around, and redo nibseq search...
					residue_offset = vseq.getMax();
					int hit_count2 = igbService.searchForRegexInResidues(
							false, regex, rev_searchstring, residue_offset, glyphs, colors[i % colors.length]);

					System.out.println(site_residues + ": " + hit_count1 + " forward strand hits and " + hit_count2 + " reverse strand hits");
					igbService.getSeqMap().updateWidget();
				}
			}finally{
				igbService.removeNotLockedUpMsg("Finding Restriction Sites... ");
			}
		}
	}
	
	@Override
	public boolean isEmbedded() {
		return true;
	}

	@Override
	public boolean isCheckMinimumWindowSize() {
		return true;
	}
}
