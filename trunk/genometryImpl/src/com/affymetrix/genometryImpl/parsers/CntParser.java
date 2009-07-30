/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 *  A parser for textual ".cnt" files from the Affymetrix CNAT program.
 */
public final class CntParser {

	static Pattern tag_val = Pattern.compile("(.*)=(.*)");
	static Pattern line_regex = Pattern.compile("\\t");
	static Pattern section_regex = Pattern.compile("\\[.*\\]");

	static final String SECTION_HEADER = "[Header]";
	static final String SECTION_COL_NAME = "[ColumnName]";
	static final String SECTION_DATA = "[Data]";

	// index of the first column containing data
	static final int FIRST_DATA_COLUMN = 3;

	public CntParser() {
	}

	public void parse(InputStream dis, AnnotatedSeqGroup seq_group)
		throws IOException  {

		String line;

		Thread thread = Thread.currentThread();
		BufferedReader reader = new BufferedReader(new InputStreamReader(dis));

		Matcher section_regex_matcher = section_regex.matcher("");
		Matcher tag_val_matcher = tag_val.matcher("");
		String current_section = "";
		Map<String,Object> headerData = new HashMap<String,Object>();


		// First read the header
		while ((line = reader.readLine()) != null && (! thread.isInterrupted())) {
			section_regex_matcher.reset(line);
			if (section_regex_matcher.matches()) {
				current_section = line;
				if (SECTION_HEADER.equals(current_section)) {
					continue;
				} else {
					break;
				}
			}

			if (SECTION_HEADER.equals(current_section)) {
				tag_val_matcher.reset(line);
				if (tag_val_matcher.matches()) {
					String tag = tag_val_matcher.group(1);
					String val = tag_val_matcher.group(2);
					headerData.put(tag, val);
				}
			} else {
				break; // finished with header, move to next section
			}
		}

		String[] column_names = null;

		while ((line = reader.readLine()) != null && (! thread.isInterrupted())) {
			section_regex_matcher.reset(line);
			if (section_regex_matcher.matches()) {
				current_section = line;
				if (SECTION_COL_NAME.equals(current_section)) {
					continue;
				} else {
					break;
				}
			}

			if (SECTION_COL_NAME.equals(current_section)) {
				column_names = line_regex.split(line);
			} else {
				break; // finished section, move to next section
			}
		}

		if (column_names == null) {
			throw new IOException("Column names were missing or malformed");
		}

		int numScores = column_names.length - FIRST_DATA_COLUMN;
		if (numScores < 1) {
			throw new IOException("No score columns in file");
		}


		//    SeqSymmetry foo = new SingletonSeqSymmetry();

		while ((line = reader.readLine()) != null && (! thread.isInterrupted())) {

			String[] fields = line_regex.split(line);
			int field_count = fields.length;

			if (field_count != column_names.length) {
				throw new IOException("Line has wrong number of data columns.");
			}

			String snpId = fields[0];
			String seqid = fields[1];
			int x = Integer.parseInt(fields[2]);

			MutableAnnotatedBioSeq aseq = seq_group.getSeq(seqid);
			if (aseq == null) { aseq = seq_group.addSeq(seqid, x); }
			if (x > aseq.getLength()) { aseq.setLength(x); }

			//        SingletonSymWithProps child = new SingletonSymWithProps(x, x, aseq);
			//        child.setProperty("method", "SNP IDs");
			//        aseq.addAnnotation(child);
			//        seq_group.addToIndex(snpId, child);

			IntList xVals = getXCoordsForSeq(aseq);
			xVals.add(x);

			FloatList[] floats = getFloatsForSeq(aseq, numScores);
			for (int j=0; j<numScores; j++) {
				FloatList floatList = floats[j];
				float floatVal = parseFloat(fields[FIRST_DATA_COLUMN+j]);
				floatList.add(floatVal);
			}
		}   // end of line-reading loop


		Iterator seqids = thing2.keySet().iterator();
		while (seqids.hasNext()) {
			String seqid = (String) seqids.next();
			IntList x = (IntList) thing2.get(seqid);
			x.trimToSize();
			FloatList[] ys = (FloatList[]) thing.get(seqid);
			MutableAnnotatedBioSeq seq = seq_group.getSeq(seqid);
			for (int i=0; i<ys.length; i++) {
				FloatList y = ys[i];
				String id = column_names[i+FIRST_DATA_COLUMN];
				if ("ChipNum".equals(id)) {
					continue;
				}
				id = getGraphIdForColumn(id, seq_group);
				GraphSymFloat graf = new GraphSymFloat(x.getInternalArray(), y.copyToArray(), id, seq);
				seq.addAnnotation(graf);
			}
		}

	}

	Map<String,String> unique_gids = new HashMap<String,String>();
	String getGraphIdForColumn(String column_id, AnnotatedSeqGroup seq_group) {
		String gid = unique_gids.get(column_id);
		if (gid == null) {
			gid = AnnotatedSeqGroup.getUniqueGraphID(column_id, seq_group);
			unique_gids.put(column_id, gid);
		}
		return gid;
	}

	public static float parseFloat(String s) {
		float val = 0.0f;
		try {
			val = Float.parseFloat(s);
		} catch (NumberFormatException nfe) {
			val = 0.0f;
		}
		return val;
	}

	Map<String,Object> thing = new HashMap<String,Object>();
	Map<String,Object> thing2 = new HashMap<String,Object>();

	FloatList[] getFloatsForSeq(MutableAnnotatedBioSeq seq, int numScores) {
		FloatList[] floats = (FloatList[]) thing.get(seq.getID());

		if (floats == null) {
			floats = new FloatList[numScores];
			for (int i=0; i<numScores; i++) {
				floats[i] = new FloatList();
			}
			thing.put(seq.getID(), floats);
		}

		return floats;
	}

	IntList getXCoordsForSeq(MutableAnnotatedBioSeq seq) {
		IntList xcoords = (IntList) thing2.get(seq.getID());

		if (xcoords == null) {
			xcoords = new IntList();
			thing2.put(seq.getID(), xcoords);
		}

		return xcoords;
	}
}
