/**
 *   Copyright (c) 2001-2006 Affymetrix, Inc.
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
package com.affymetrix.igb.menuitem;

import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.SwingUtilities;

import com.affymetrix.igb.event.ThreadProgressMonitor;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.useq.USeqUtilities;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

public final class OpenGraphAction {

	private static GenometryModel gmodel = GenometryModel.getGenometryModel();

	public static Thread loadAndShowGraphs(final URL[] files, final BioSeq aseq, final SeqMapView gviewer) {
		Thread t = new Thread() {

			ThreadProgressMonitor monitor = null;

			@Override
			public void run() {
				monitor = new ThreadProgressMonitor(
						gviewer.getFrame(),
						"Loading graphs...",
						"Loading graphs",
						this,
						false, false);
				monitor.showDialogEventually();
				try {
					AnnotatedSeqGroup seq_group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
					loadGraphFiles(files, seq_group, aseq, true, monitor, gviewer);
				} catch (final Throwable t) { // catch Out-Of-Memory Errors, etc.
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {
							if (monitor != null) {
								monitor.closeDialog();
							}
							ErrorHandler.errorPanel("Error loading graphs", t);
						}
					});
				}
				if (monitor != null) {
					monitor.closeDialogEventually();
					monitor = null;
				}
			}
		};
		return t;
	}

	private static void updateViewer(final SeqMapView gviewer) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				gviewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
			}
		});
	}

	public static List<GraphSym> loadGraphFile(URL furl, AnnotatedSeqGroup seq_group, BioSeq aseq) throws IOException, OutOfMemoryError {
		InputStream fis = null;
		try {
			String path = furl.getPath();
			//check again to see if it is a graph?  why this is checked prior to calling! 
			if (!GraphSymUtils.isAGraphFilename(path) && !path.endsWith(USeqUtilities.USEQ_EXTENSION_NO_PERIOD)) {
				throw new IOException("Filename does not match any known type of graph:\n" + path);
			}
			String graph_url = furl.toExternalForm();
			fis = LocalUrlCacher.getInputStream(graph_url);
			String graph_name = GraphSymUtils.getGraphNameForURL(furl);
			List<GraphSym> graphs = GraphSymUtils.readGraphs(fis, graph_url, seq_group, null);
			GraphSymUtils.processGraphSyms(graphs, graph_url);
			GraphSymUtils.setName(graphs, graph_name);
			return graphs;
		} finally {
			GeneralUtils.safeClose(fis);
		}
	}



	/** Loads graphs from a set of files.
	 *  The return List will contain all sucessfully loaded GraphSym objects;
	 *  there may be more or fewer graphs than given files.
	 *  @param aseq  a BioSeq. If null, this routine will return an empty List
	 *  @param update_viewer  If true, this will call {@link #updateViewer(SeqMapView)} after
	 *   each graph file is loaded.
	 *  @throws OutOfMemoryError  Any routine can throw an OutOfMemoryError, but
	 *  this is a reminder that this is fairly common here.  (You have to catch
	 *  "Throwable" rather than "Exception" to catch these.)
	 */
	private static List<GraphSym> loadGraphFiles(URL[] files, AnnotatedSeqGroup seq_group, BioSeq aseq, boolean update_viewer, ThreadProgressMonitor monitor, SeqMapView gviewer)
			throws IOException, OutOfMemoryError {
		List<GraphSym> graphs = Collections.<GraphSym>emptyList();
		if (aseq != null) {
			for (URL file : files) {
				if (monitor != null) {
					monitor.setMessageEventually("Loading graph from: " + file.getPath());
				}
				graphs = loadGraphFile(file, seq_group, aseq);
				if (update_viewer && !graphs.isEmpty()) {
					updateViewer(gviewer);
				}
			}
		}
		return graphs;
	}

}
