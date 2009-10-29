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

package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import java.io.*;
import java.util.*;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.parsers.graph.SgrParser;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.parsers.graph.GrParser;
import com.affymetrix.genometryImpl.parsers.graph.BgrParser;
import com.affymetrix.genometryImpl.parsers.graph.WiggleParser;

public final class GraphSymUtils {

	static boolean DEBUG_READ = false;
	static boolean DEBUG_DATA = false;
	static int MAX_INITCAP = 1024*1024;

	/** 8-byte floating-point.  Names of the other data-type constants can be interpreted similarly. */
	public static int BYTE8_FLOAT = 0;
	public static int BYTE4_FLOAT = 1;
	public static int BYTE4_SIGNED_INT = 2;
	public static int BYTE2_SIGNED_INT = 3;
	public static int BYTE1_SIGNED_INT = 4;
	public static int BYTE4_UNSIGNED_INT = 5;
	public static int BYTE2_UNSIGNED_INT = 6;
	public static int BYTE1_UNSIGNED_INT = 7;

	public static String[] valstrings =
	{ "BYTE8_FLOAT", "BYTE4_FLOAT",
		"BYTE4_SIGNED_INT", "BYTE2_SIGNED_INT", "BYTE1_SIGNED_INT",
		"BYTE4_UNSIGNED_INT", "BYTE2_UNSIGNED_INT", "BYTE1_UNSIGNED_INT" };

	/**
	 *  Transforms a restricted type of GraphSym based on a SeqSymmetry.
	 *  This is _not_ a general algorithm for transforming GraphSyms with an arbitrary mapping sym --
	 *    it is simpler, and assumes that the mapping symmetry is of depth=2 (or possibly 1?) and
	 *    breadth = 2, and that they're "regular" (parent sym and each child sym have seqspans pointing
	 *    to same two BioSeqs.
	 *  It should work fine on GraphIntervalSym objects as well as regular GraphSym objects.
	 *  ensure_unique_id indicates whether should try to muck with id so it's not same as any GraphSym on the seq
	 *     (including the original_graf, if it's one of seq's annotations)
	 *     For transformed GraphSyms probably should set ensure_unique_id to false, unless result is actually added onto toseq...
	 */
	public static GraphSymFloat transformGraphSym(GraphSym original_graf, SeqSymmetry mapsym, boolean ensure_unique_id) {
		if (original_graf.getPointCount() == 0) {
			return null;
		}
		MutableAnnotatedBioSeq fromseq = original_graf.getGraphSeq();
		SeqSpan fromspan = mapsym.getSpan(fromseq);

		if (fromseq == null || fromspan == null) {
			return null;
		}
		GraphSymFloat new_graf = null;
		MutableAnnotatedBioSeq toseq = SeqUtils.getOtherSeq(mapsym, fromseq);

		SeqSpan tospan = mapsym.getSpan(toseq);
		if (toseq == null || tospan == null) {
			return null;
		}
		double graf_base_length = original_graf.getGraphXCoord(original_graf.getPointCount() - 1) - original_graf.getGraphXCoord(0);

		// calculating graf length from xcoords, since graf's span
		//    is (usually) incorrectly set to start = 0, end = seq.getLength();
		double points_per_base = (double) original_graf.getPointCount() / graf_base_length;
		int initcap = (int) (points_per_base * toseq.getLength() * 1.5);
		if (initcap > MAX_INITCAP) {
			initcap = MAX_INITCAP;
		}
		IntList new_xcoords = new IntList(initcap);
		FloatList new_ycoords = new FloatList(initcap);
		IntList new_wcoords = null;
		if (hasWidth(original_graf)) {
			new_wcoords = new IntList(initcap);
		}

		List<SeqSymmetry> leaf_syms = SeqUtils.getLeafSyms(mapsym);
		for (SeqSymmetry leafsym : leaf_syms) {
			SeqSpan fspan = leafsym.getSpan(fromseq);
			SeqSpan tspan = leafsym.getSpan(toseq);
			if (fspan == null || tspan == null) {
				continue;
			}
			boolean opposite_spans = fspan.isForward() ^ tspan.isForward();
			int ostart = fspan.getStart();
			int oend = fspan.getEnd();
			double scale = tspan.getLengthDouble() / fspan.getLengthDouble();
			if (opposite_spans) {
				scale = -scale;
			}
			double offset = tspan.getStartDouble() - (scale * fspan.getStartDouble());
			int kmax = original_graf.getPointCount();


			int start_index = 0;

			if (!hasWidth(original_graf)) {
				// If there are no width coordinates, then we can speed up the
				// drawing by determining the start_index of the first x-value in range.
				// If there are widths, this is much harder to determine, since
				// even something starting way over to the left but having a huge width
				// could intersect our region.  So when there are wcoords, don't
				// try to determine start_index.  Luckily, when there are widths, there
				// tend to be fewer graph points to deal with.
				// assumes graph is sorted
				start_index = determineBegIndex(original_graf, ostart-1);
			}

			for (int k = start_index; k < kmax; k++) {
				final int old_xcoord = original_graf.getGraphXCoord(k);
				if (old_xcoord >= oend) {
					break; // since the array is sorted, we can stop here
				}
				int new_xcoord = (int) ((scale * old_xcoord) + offset);

				// new_x2coord will represent x + width: initial assumption is width is zero
				int new_x2coord = new_xcoord;
				if (hasWidth(original_graf)) {
					final int old_x2coord = old_xcoord + ((GraphIntervalSym)original_graf).getGraphWidthCoord(k);
					new_x2coord = (int) ((scale * old_x2coord) + offset);
					if (new_x2coord >= tspan.getEnd()) {
						new_x2coord = tspan.getEnd();
					}
				}

				final int tstart = tspan.getStart();
				if (new_xcoord < tstart) {
					if (!hasWidth(original_graf)) {
						continue;
					} else if (new_x2coord > tstart) {
						new_xcoord = tstart;
					} else {
						continue;
					}
				}

				new_xcoords.add(new_xcoord);
				new_ycoords.add(original_graf.getGraphYCoord(k));
				if (hasWidth(original_graf)) {
					int new_wcoord = new_x2coord - new_xcoord;
					new_wcoords.add(new_wcoord);
				}
			}
		}
		String newid = original_graf.getID();
		if (ensure_unique_id) {
			newid = GraphSymUtils.getUniqueGraphID(newid, toseq);
		}

		if (!hasWidth(original_graf)) {
			new_graf = new GraphSymFloat(new_xcoords.copyToArray(), new_ycoords.copyToArray(),
					newid, toseq);
		} else {
			new_graf = new GraphIntervalSym(new_xcoords.copyToArray(), new_wcoords.copyToArray(),
					new_ycoords.copyToArray(), newid, toseq);
		}
		new_graf.setGraphName(original_graf.getGraphName());
		return new_graf;
	}



	/** Detects whether the given filename ends with a recognized ending for
	 *  a graph filetype. Compression endings like gz and zip are removed
	 *  before testing the name.
	 */
	public static boolean isAGraphFilename(String name) {
		String lc = GeneralUtils.stripEndings(name).toLowerCase();
		return (
				lc.endsWith(".gr") ||
				lc.endsWith(".bgr") ||
				lc.endsWith(".bar") ||
				lc.endsWith(".sgr") ||
				lc.endsWith(".wig")
			   );
	}

	/**
	 *  Reads one or more graphs from an input stream.
	 *  Equivalent to a call to the other readGraphs() method using seq = null.
	 */
	public static List<GraphSym> readGraphs(InputStream istr, String stream_name, GenometryModel gmodel, AnnotatedSeqGroup seq_group) throws IOException  {
		return readGraphs(istr, stream_name, gmodel, seq_group, (MutableAnnotatedBioSeq) null);
	}

	/**
	 *  Reads one or more graphs from an input stream.
	 *  Some graph file formats can contain only one graph, others contain
	 *  more than one.  For consistency, always returns a List (possibly empty).
	 *  Will accept "bar", "bgr", "gr", or "sgr".
	 *  Loaded graphs will be attached to their respective BioSeq's, if they
	 *  are instances of MutableAnnotatedBioSeq.
	 *  @param seq  Ignored in most cases.  But for "gr" files that
	 *   do not specify a BioSeq, use this parameter to specify it.  If null
	 *   then GenometryModel.getSelectedSeq() will be used.
	 */
	public static List<GraphSym> readGraphs(InputStream istr, String stream_name, GenometryModel gmodel, AnnotatedSeqGroup seq_group, MutableAnnotatedBioSeq seq) throws IOException  {
		List<GraphSym> grafs = null;
		StringBuffer stripped_name = new StringBuffer();
		InputStream newstr = GeneralUtils.unzipStream(istr, stream_name, stripped_name);
		String sname = stripped_name.toString().toLowerCase();

		if (seq == null) {
			seq = gmodel.getSelectedSeq();
		}

		if (sname.endsWith(".bar"))  {
			grafs = BarParser.parse(newstr, gmodel, seq_group, stream_name);
		}
		else if (sname.endsWith(".gr")) {
			// If this is a newly-created seq group, then go ahead and add a new 
			// unnamed seq to it if necessary.
			if (seq_group.getSeqCount() == 0) {
				seq = seq_group.addSeq("unnamed", 1000);
			}
			if (seq == null) {
				throw new IOException("Must select a sequence before loading a graph of type 'gr'");
			}
			GraphSym graph = GrParser.parse(newstr, seq, stream_name);
			int max_x = graph.getGraphXCoord(graph.getPointCount()-1);
			MutableAnnotatedBioSeq gseq = graph.getGraphSeq();
			seq_group.addSeq(gseq.getID(), max_x); // this stretches the seq to hold the graph
			grafs = wrapInList(graph);
		}
		else if (sname.endsWith(".bgr")) {
			grafs = wrapInList(BgrParser.parse(newstr, stream_name, seq_group, true));
		}
		else if (sname.endsWith(".sgr")) {
			grafs = SgrParser.parse(newstr, stream_name, seq_group, false);
		}
		else if (sname.endsWith(".wig")) {
			WiggleParser wig_parser = new WiggleParser();
			grafs = wig_parser.parse(newstr, seq_group, false, stream_name);
		} else {
			throw new IOException("Unrecognized filename for a graph file:\n"+stream_name);
		}

		processGraphSyms(grafs, stream_name);

		System.gc();	// this is just for IGB; give a more accurate estimate of how much memory is being used.

		if (grafs == null) {
			grafs = Collections.<GraphSym>emptyList();
		}
		return grafs;
	}
	
	/**
	 * Calls {@link AnnotatedSeqGroup#getUniqueGraphID(String,BioSeq)}.
	 */
	public static String getUniqueGraphID(String id, MutableAnnotatedBioSeq seq) {
		return AnnotatedSeqGroup.getUniqueGraphID(id, seq);
	}


	private static List<GraphSym> wrapInList(GraphSym gsym) {
		List<GraphSym> grafs = null;
		if (gsym != null) {
			grafs = new ArrayList<GraphSym>();
			grafs.add(gsym);
		}
		return grafs;
	}

	/*
	 *  Does some post-load processing of Graph Syms.
	 *  For each GraphSym in the list,
	 *  Adds it as an annotation of the MutableAnnotatedBioSeq it refers to.
	 *  Sets the "source_url" to the given stream name.
	 *  Calls setGraphName() with the given name;
	 *  Converts to a trans frag graph if "TransFrag" is part of the graph name.
	 *  @param grafs  a List, empty or null is OK.
	 */
	private static void processGraphSyms(List<GraphSym> grafs, String original_stream_name) {
		if (grafs == null) {
			return;
		}
		for (GraphSym gsym : grafs) {
			MutableAnnotatedBioSeq gseq = gsym.getGraphSeq();
			if (gseq != null) {
				String gid = gsym.getID();
				String newid = getUniqueGraphID(gid, gseq);
				//TODO: Instead of re-setting the graph ID, a unique ID should have been used in the constructor
				if (!(newid.equals(gid))) {
					gsym.setID(newid);
				}
			}
			gsym.lockID();
			if (gseq instanceof MutableAnnotatedBioSeq) {
				gseq.addAnnotation(gsym);
			}

			gsym.setProperty("source_url", original_stream_name);

			if ((gsym.getGraphName() != null) && (gsym.getGraphName().indexOf("TransFrag") >= 0)) {
				gsym = GraphSymUtils.convertTransFragGraph(gsym);
			}
		}
	}


	/** Writes out in a variety of possible formats depending
	 *  on the suffix of the filename.
	 *  Formats include ".gr", ".sgr", ".sin" == ".egr", ".bgr".
	 *  @param seq_group the AnnotatedSeqGroup the graph is on, needed for ".wig", ".egr", and ".sin" formats.
	 */
	public static void writeGraphFile(GraphSym gsym, AnnotatedSeqGroup seq_group, String file_name) throws IOException {
		BufferedOutputStream bos = null;
		try {
			if (file_name.endsWith(".bgr")) {
				bos = new BufferedOutputStream(new FileOutputStream(file_name));
				BgrParser.writeBgrFormat(gsym, bos);
			} else if (file_name.endsWith(".gr")) {
				bos = new BufferedOutputStream(new FileOutputStream(file_name));
				GrParser.writeGrFormat(gsym, bos);
			} else if (file_name.endsWith(".sgr")) {
				bos = new BufferedOutputStream(new FileOutputStream(file_name));
				SgrParser.writeSgrFormat(gsym, bos);
			} else if (file_name.endsWith(".egr") || file_name.endsWith(".sin")) {
				if (gsym instanceof GraphIntervalSym) {
					String genome_name = null;
					if (seq_group != null) {
						genome_name = seq_group.getID();
					}
					bos = new BufferedOutputStream(new FileOutputStream(file_name));
					ScoredIntervalParser.writeEgrFormat((GraphIntervalSym) gsym, genome_name, bos);
				} else {
					throw new IOException("Not the correct graph type for the '.egr' format.");
				}
			} else if (file_name.endsWith(".wig")) {
				if (gsym instanceof GraphIntervalSym) {
					GraphIntervalSym gisym = (GraphIntervalSym) gsym;
					String genome_name = null;
					if (seq_group != null) {
						genome_name = seq_group.getID();
					}
					bos = new BufferedOutputStream(new FileOutputStream(file_name));
					WiggleParser.writeBedFormat(gisym, genome_name, bos);
				} else {
					throw new IOException("Not the correct graph type for the '.wig' format.");
				}
			} else {
				throw new IOException("Graph file name does not have the correct extension");
			}
		} finally {
			GeneralUtils.safeClose(bos);
		}
	}

	/**
	 *  Calculate percentile rankings of graph values.
	 *  In the resulting array, the value of scores[i] represents
	 *  the value at percentile (100 * i)/(scores.length - 1).
	 *
	 *  This is an expensive calc, due to sort of copy of scores array
	 *    Plan to change this to a sampling strategy if scores.length greater than some cutoff (maybe 100,000 ?)
	 */
	public static float[] calcPercents2Scores(float[] scores, float bins_per_percent) {
		boolean USE_SAMPLING = true;
		int max_sample_size = 100000;
		float abs_max_percent = 100.0f;
		float percents_per_bin = 1.0f / bins_per_percent;

		int num_scores = scores.length;
		float[] ordered_scores;
		// sorting a large array is an expensive operation timewise, so if scores array is
		//   larger than a certain size, do approximate ranking instead by sampling the scores array
		//   and ranking over smaple
		//
		// in performance comparisons of System.arraycopy() vs. piecewise loop,
		//     piecewise takes about twice as long for copying same number of elements,
		//     but this 2x performance hit should be overwhelmed by time taken for larger array sort
		//tim.start();
		if (USE_SAMPLING && (num_scores > (2 * max_sample_size)) ) {
			int sample_step = num_scores / max_sample_size;
			int sample_index = 0;
			ordered_scores = new float[max_sample_size];
			for (int i=0; i<max_sample_size; i++) {
				ordered_scores[i] = scores[sample_index];
				sample_index += sample_step;
			}
		}
		else {
			ordered_scores = new float[num_scores];
			System.arraycopy(scores, 0, ordered_scores, 0, num_scores);
		}
		Arrays.sort(ordered_scores);
		int num_percents = (int)(abs_max_percent * bins_per_percent + 1);
		float[] percent2score = new float[num_percents];

		float scores_per_percent = ordered_scores.length / 100.0f;
		for (float percent = 0.0f; percent <= abs_max_percent; percent += percents_per_bin) {
			int score_index = (int)(percent * scores_per_percent);
			if (score_index >= ordered_scores.length) { score_index = ordered_scores.length -1; }
			percent2score[Math.round(percent * bins_per_percent)] = ordered_scores[score_index];
		}
		// just making sure max 100% is really 100%...
		percent2score[percent2score.length - 1] = ordered_scores[ordered_scores.length - 1];
		return percent2score;
	}


	private static GraphSymFloat convertTransFragGraph(GraphSym trans_frag_graph) {
		int xcount = trans_frag_graph.getPointCount();
		if (xcount < 2) { return null; }

		int transfrag_max_spacer = 20;
		MutableAnnotatedBioSeq seq = trans_frag_graph.getGraphSeq();
		IntList newx = new IntList();
		FloatList newy = new FloatList();

		// transfrag ycoords should be irrelevant
		int xmin = trans_frag_graph.getGraphXCoord(0);
		float y_at_xmin = trans_frag_graph.getGraphYCoord(0);
		int prevx = xmin;
		float prevy = y_at_xmin;
		int curx = xmin;
		float cury = y_at_xmin;
		for (int i=1; i<xcount; i++) {
			curx = trans_frag_graph.getGraphXCoord(i);
			cury = trans_frag_graph.getGraphYCoord(i);
			if ((curx - prevx) > transfrag_max_spacer) {
				newx.add(xmin);
				newy.add(y_at_xmin);
				newx.add(prevx);
				newy.add(prevy);
				if (i == (xcount - 2)) {
					System.out.println("breaking, i = " + i + ", xcount = " + xcount);
					break;
				}
				xmin = curx;
				y_at_xmin = cury;
				i++;
			}
			prevx = curx;
			prevy = cury;
		}
		newx.add(xmin);
		newy.add(y_at_xmin);
		newx.add(curx);
		newy.add(cury);
		String newid = GraphSymUtils.getUniqueGraphID(trans_frag_graph.getGraphName(), seq);
		GraphSymFloat span_graph = new GraphSymFloat(newx.copyToArray(), newy.copyToArray(), newid, seq);

		// copy properties over...
		span_graph.setProperties(trans_frag_graph.cloneProperties());

		if (DEBUG_DATA) {
			for (int i = 0; i < span_graph.getPointCount(); i++) {
				System.out.println("TransFrag graph point: x = " + span_graph.getGraphXCoord(i)
						+ ", y = " + span_graph.getGraphXCoord(i));
			}
		}

		// add transfrag property...
		span_graph.setProperty("TransFrag", "TransFrag");
		return span_graph;

	}

	/**
	 * Find last point with value <= xmin.
	 * @param xmin
	 * @return
	 */
	public final static int determineBegIndex(GraphSym graf, double xmin) {
		int xCoordLength = graf.getPointCount();
		for (int i=0;i<xCoordLength;i++) {
			if (graf.getGraphXCoord(i) > (int)xmin) {
				return Math.max(0, i-1);
			}
		}
		return 0;
	}

	/**
	 * Find first point with value >= xmax.
	 * @param xmax
	 * @return
	 */
	public final static int determineEndIndex(GraphSym graf, double xmax) {
		int xCoordLength = graf.getPointCount();
		for (int i=0;i<xCoordLength;i++) {
			if (graf.getGraphXCoord(i) >= (int)xmax) {
				return i;
			}
		}
		return xCoordLength-1;
	}

	public final static boolean hasWidth(GraphSym graf) {
		return (graf instanceof GraphIntervalSym) && ((GraphIntervalSym)graf).getGraphWidthCount() > 0;
	}
}
