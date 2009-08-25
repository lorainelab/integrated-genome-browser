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

package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.IndexedSingletonSym;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.FloatList;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 *  This class (and file format) have been replaced by ScoredIntervalParser (and sin file format)
 *  Kept now only to parse in older data files.
 */
public final class ScoredMapParser {

	static Pattern line_regex  = Pattern.compile("\t");
	//boolean attach_graphs = ScoredIntervalParser.default_attach_graphs;

	public void parse(InputStream istr, String stream_name, MutableAnnotatedBioSeq aseq, AnnotatedSeqGroup seq_group) {
		//attach_graphs = UnibrowPrefsUtil.getBooleanParam(ScoredIntervalParser.PREF_ATTACH_GRAPHS,
		//                                             ScoredIntervalParser.default_attach_graphs);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(istr));
			String line = null;

			String unique_container_name = AnnotatedSeqGroup.getUniqueGraphID(stream_name, seq_group);
			ScoredContainerSym parent = new ScoredContainerSym();
			parent.setID(unique_container_name);
			parent.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
			parent.setProperty("method", stream_name);
			parent.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);

			// assuming first line is header
			line = br.readLine();
			String[] headers = line_regex.split(line);
			List<String> score_names = new ArrayList<String>();
			List<FloatList> score_arrays = new ArrayList<FloatList>();
			System.out.println("headers: " + line);
			for (int i=2; i<headers.length; i++) {
				//        System.out.println("header " + i + ": " + headers[i]);
				score_names.add(headers[i]);
				score_arrays.add(new FloatList());
			}

			int line_count = 0;
			while ((line = br.readLine()) != null) {
				String[] fields = line_regex.split(line);
				int min = Integer.parseInt(fields[0]);
				int max = Integer.parseInt(fields[1]);
				SeqSymmetry child = new IndexedSingletonSym(min, max, aseq);
				parent.addChild(child);   // ScoredContainerSym.addChild() handles setting of child index and parent fields
				for (int field_index = 2; field_index < fields.length; field_index++) {
					FloatList flist = score_arrays.get(field_index-2);
					float score = Float.parseFloat(fields[field_index]);
					flist.add(score);
				}
				line_count++;
			}
			System.out.println("data lines in file: " + line_count);
			// System.out.println("child syms: " + parent.getChildCount());
			int score_count = score_names.size();
			for (int i=0; i<score_count; i++) {
				String score_name = score_names.get(i);
				FloatList flist = score_arrays.get(i);
				float[] scores = flist.copyToArray();
				// System.out.println("adding scores for " + score_name + ", score count = " + scores.length);
				// System.out.println("first 3 scores:\t" + scores[0] + "\t" + scores[1] + "\t" + scores[2]);
				parent.addScores(score_name, scores);
			}
			aseq.addAnnotation(parent);
			//      if (attach_graphs) {
			//        // make a GraphSym for each scores column, and add as an annotation to aseq
			//        for (int i=0; i<score_count; i++) {
			//          String score_name = parent.getScoreName(i);
			//          GraphSym gsym = parent.makeGraphSym(score_name, true);
			//          aseq.addAnnotation(gsym);
			//        }
			//        // System.out.println("finished attaching graphs");
			//      }
		}
		catch (Exception ex) { ex.printStackTrace(); }
	}

	public static void main(String[] args) {
		String test_file = System.getProperty("user.dir") + "/testdata/fromAntonio/exp_files/maps_chr22_3-26-2004/tau0.2-pp0.map";
		String test_name = "tau0_test";
		System.out.println("testing ScoredMapParser, parsing file: " + test_file);
		ScoredMapParser tester = new ScoredMapParser();
		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
		MutableAnnotatedBioSeq aseq = new BioSeq("test_seq", "test_version", 50000000);
		try {
			FileInputStream fis = new FileInputStream(new File(test_file));
			tester.parse(fis, test_name, aseq, seq_group);
		}
		catch (Exception ex) { ex.printStackTrace(); }
		System.out.println("done testing ScoredMapParser");
	}

}
