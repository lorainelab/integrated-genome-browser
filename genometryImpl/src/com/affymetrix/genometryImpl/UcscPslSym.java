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

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.EfficientPairSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;

import java.util.*;
import java.io.*;

/**
 *  A SeqSymmetry optimized for holding data from PSL files.
 *  Target span is at span index {@link #QUERY_INDEX} = 0.
 *  Query span is at span {@link #TARGET_INDEX} = 1.
 */
public class UcscPslSym
	implements TypedSym, SearchableSeqSymmetry, SymWithProps {

	public static final int QUERY_INDEX = 0;
	public static final int TARGET_INDEX = 1;

	String type;
	int matches;
	int mismatches;
	int repmatches; // should be derivable w/o residues
	int ncount;
	int qNumInsert;  // should be derivable w/o residues
	int qBaseInsert; // should be derivable w/o residues
	int tNumInsert;  // should be derivable w/o residues
	int tBaseInsert; // should be derivable w/o residues
	boolean same_orientation;
	boolean overlapping_query_coords = false;
	boolean overlapping_target_coords = false;

	MutableAnnotatedBioSeq queryseq;
	int qmin;
	int qmax;
	MutableAnnotatedBioSeq targetseq;
	int tmin;
	int tmax;
	int[] blockSizes;
	int[] qmins;
	int[] tmins;
	Map<String,Object> props;

	/**
	 *  @param blockcount  ignored, uses blockSizes.length
	 */
	public UcscPslSym(String type,
			int matches,
			int mismatches,
			int repmatches,
			int ncount,
			int qNumInsert,
			int qBaseInsert,
			int tNumInsert,
			int tBaseInsert,
			boolean same_orientation,
			MutableAnnotatedBioSeq queryseq,
			int qmin,
			int qmax,
			MutableAnnotatedBioSeq targetseq,
			int tmin,
			int tmax,
			int blockcount,  // now ignored, uses blockSizes.length
			int[] blockSizes,
			int[] qmins,
			int[] tmins
			) {

		this.type = type;
		this.matches = matches;
		this.mismatches = mismatches;
		this.repmatches = repmatches;
		this.ncount = ncount;
		this.qNumInsert = qNumInsert;
		this.qBaseInsert = qBaseInsert;
		this.tNumInsert = tNumInsert;
		this.tBaseInsert = tBaseInsert;
		this.same_orientation = same_orientation;
		this.queryseq = queryseq;
		this.qmin = qmin;
		this.qmax = qmax;
		this.targetseq = targetseq;
		this.tmin = tmin;
		this.tmax = tmax;
		this.blockSizes = blockSizes;
		this.qmins = qmins;
		this.tmins = tmins;

		// calculating whether any of the query coords overlap (assumes query coords are sorted in ascending order)
		// (should probably also do this for target coords???)
		int count = qmins.length-1;
		int prevmin = 0;
		for (int i=0; i<count; i++) {
			if ((qmins[i] < prevmin) || ((qmins[i] + blockSizes[i]) > qmins[i+1])) {
				overlapping_query_coords = true;
				break;
			}
			prevmin = qmins[i];
		}
			}

	public boolean supportsFastIntervalQuery() {
		return (! overlapping_query_coords);
	}

	public String getType() { return type; }

	/**
	 *  Returns the queryseq id.
	 */
	public String getID() { return queryseq.getID(); }

	/** Always returns 2. */
	public int getSpanCount() { return 2; }

	public SeqSpan getSpan(MutableAnnotatedBioSeq bs) {
		SeqSpan span = null;
		if (bs.equals(targetseq)) {
			if (same_orientation) { span = new SimpleSeqSpan(tmin, tmax, targetseq); }
			else { span = new SimpleSeqSpan(tmax, tmin, targetseq); }
		}
		else if (bs.equals(queryseq)) {
			span = new SimpleSeqSpan(qmin, qmax, queryseq);
		}
		return span;
	}

	public boolean getSpan(MutableAnnotatedBioSeq bs, MutableSeqSpan span) {
		if (bs.equals(targetseq)) {
			if (same_orientation) { span.set(tmin, tmax, targetseq); }
			else { span.set(tmax, tmin, targetseq); }
			return true;
		}
		else if (bs.equals(queryseq)) {
			span.set(qmin, qmax, queryseq);
		}
		return false;
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == QUERY_INDEX) {
			span.set(qmin, qmax, queryseq);
		}
		else if (index == TARGET_INDEX) {
			if (same_orientation) { span.set(tmin, tmax, targetseq); }
			else { span.set(tmax, tmin, targetseq); }
		}
		return false;
	}

	public SeqSpan getSpan(int index) {
		SeqSpan span = null;
		if (index == QUERY_INDEX) {
			span = new SimpleSeqSpan(qmin, qmax, queryseq);
		}
		else if (index == TARGET_INDEX) {
			if (same_orientation) { span = new SimpleSeqSpan(tmin, tmax, targetseq); }
			else { span = new SimpleSeqSpan(tmax, tmin, targetseq); }
		}
		return span;
	}

	public MutableAnnotatedBioSeq getSpanSeq(int index) {
		if (index == QUERY_INDEX) { return queryseq; }
		else if (index == TARGET_INDEX) { return targetseq; }
		return null;
	}

	public int getChildCount() { return blockSizes.length; }

	public boolean getChildSpan(int child_index, MutableAnnotatedBioSeq span_seq, MutableSeqSpan mutspan) {
		boolean success = false;
		if (span_seq == queryseq) {
			mutspan.set(qmins[child_index], qmins[child_index]+blockSizes[child_index], queryseq);
			success = true;
		}
		else if (span_seq == targetseq) {
			if (same_orientation) {
				mutspan.set(tmins[child_index], tmins[child_index]+blockSizes[child_index], targetseq);
			}
			else {
				mutspan.set(tmins[child_index]+blockSizes[child_index], tmins[child_index], targetseq);
			}
			success = true;
		}
		return success;
	}

	public SeqSymmetry getChild(int i) {
		if (same_orientation) {
			return new EfficientPairSeqSymmetry(qmins[i], qmins[i]+blockSizes[i], queryseq,
					tmins[i], tmins[i]+blockSizes[i], targetseq);
		}
		else {
			return new EfficientPairSeqSymmetry(qmins[i], qmins[i]+blockSizes[i], queryseq,
					tmins[i] + blockSizes[i], tmins[i], targetseq);
		}
	}

	// SearchableSeqSymmetry interface
	public List<SeqSymmetry> getOverlappingChildren(SeqSpan input_span) {
		final boolean debug = false;
		List<SeqSymmetry> results = null;
		if (input_span.getBioSeq() != this.getQuerySeq()) {
			results =  SeqUtils.getOverlappingChildren(this, input_span);
			if (debug) System.out.println("input span != query seq, doing normal SeqUtils.getOverlappingChildren() call");
		}
		else if (overlapping_query_coords) {
			results =  SeqUtils.getOverlappingChildren(this, input_span);
			// or maybe do a smarter binary search with constrained scan???
			if (debug) System.out.println("query children overlap, doing normal SeqUtils.getOverlappingChildren() call");
		}
		else {
			//      System.out.println("trying to do a binary search on qmins");
			// do a simple binary search on qmins to find first qmin with coord >= input_span.getMin()
			// then scan till qmin coord >= input_span.getMax();
			// collect all qmins in between and use as basis for creating syms...
			int input_min = input_span.getMin();
			int input_max = input_span.getMax();
			int beg_index = Arrays.binarySearch(qmins, input_min);
			if (debug) {
				System.out.println("map symmetry:");
				SeqUtils.printSymmetry(this);
			}
			if (debug) { System.out.println("initial beg_index: " + beg_index); }
			if (beg_index < 0)  { beg_index = -beg_index -1; }
			//      if (beg_index < 0)  { beg_index = (-beg_index -1) -1; }
			else {
				// backtrack beg_index in case hit duplicates???
				// well, only way this can happen when (! overlapping_query_coords) is when have weird
				//   entries where blockSize is zero...
				while ((beg_index > 0) && (qmins[beg_index-1] == qmins[beg_index])) { beg_index--; }
			}
			while ((beg_index > 0) && ((qmins[beg_index-1] + blockSizes[beg_index-1]) > input_min)) {
				beg_index--;
			}
			if (debug) {
				System.out.println("binary search, final beg_index: " + beg_index);
				System.out.println("binary search, child count: " + this.getChildCount());
			}

			if ((beg_index < qmins.length) && (qmins[beg_index] < input_max)) {
				results = new ArrayList<SeqSymmetry>();
				int index = beg_index;
				// now scan forward till qmin[index] >= input_max and collect list of symmetries
				while ((index < qmins.length) && (qmins[index] < input_max)) {
					results.add(this.getChild(index));
					index++;
				}
			}
		}
		return results;
	}

	public int getMatches() { return matches; }
	public void setMatches(int count) { matches = count; }
	public int getMisMatches() { return mismatches; }
	public int getRepMatches() { return repmatches; }
	public int getNCount() { return ncount; }
	public int getQueryNumInserts() { return qNumInsert; }
	public int getQueryBaseInserts() { return qBaseInsert; }
	public int getTargetNumInserts() { return tNumInsert; }
	public int getTargetBaseInserts() { return tBaseInsert; }
	public boolean getSameOrientation() { return same_orientation; }
	public MutableAnnotatedBioSeq getQuerySeq() { return queryseq; }
	public int getQueryMin() { return qmin; }
	public int getQueryMax() { return qmax; }
	public MutableAnnotatedBioSeq getTargetSeq() { return targetseq; }
	public int getTargetMin() { return tmin; }
	public int getTargetMax() { return tmax; }

	public Map<String,Object> getProperties() {
		return cloneProperties();
	}

	public Map<String,Object> cloneProperties() {
		HashMap<String,Object> tprops = new HashMap<String,Object>();

		tprops.put("id", getQuerySeq().getID());
		tprops.put("type", "Pairwise Alignment");
		tprops.put("same orientation", new Boolean(getSameOrientation()));
		tprops.put("# matches", new Integer(getMatches()));
		tprops.put("query length", new Integer(queryseq.getLength()));
		tprops.put("# query inserts", new Integer(getQueryNumInserts()));
		tprops.put("# query bases inserted", new Integer(getQueryBaseInserts()));
		tprops.put("# target inserts", new Integer(getTargetNumInserts()));
		tprops.put("# target bases inserted", new Integer(getTargetBaseInserts()));
		//tprops.put("query seq", getQuerySeq().getID());
		//tprops.put("target seq", getTargetSeq().getID());
		if (props != null) {
			tprops.putAll(props);
		}
		return tprops;
	}

	public Object getProperty(String key) {
		if (key.equals("id")) {  return getQuerySeq().getID(); }
		else if (key.equals("method")) {  return getType(); }
		else if (key.equals("type")) {  return "Pairwise Alignment"; }
		else if (key.equals("same orientation")) { return getSameOrientation()?"true":"false"; }
		else if (key.equals("# matches")) { return Integer.toString(getMatches()); }
		else if (key.equals("query length")) { return Integer.toString(queryseq.getLength()); }
		else if (key.equals("# query inserts")) { return Integer.toString(getQueryNumInserts()); }
		else if (key.equals("# query bases inserted")) { return Integer.toString(getQueryBaseInserts()); }
		else if (key.equals("# target inserts")) { return Integer.toString(getTargetNumInserts()); }
		else if (key.equals("# target bases inserted")) { return Integer.toString(getTargetBaseInserts()); }

		//else if (key.equals("query seq")) { return getQuerySeq().getID(); }
		//else if (key.equals("target seq")) { return  getTargetSeq().getID(); }

		// then try to match with any extras
		else if (props != null) {
			return props.get(key);
		}
		else { return null; }
	}

	public boolean setProperty(String name, Object val) {
		if (props == null) {
			props = new Hashtable<String,Object>();
		}
		props.put(name, val);
		return true;
	}

	/**
	 *  Writes a line of PSL to a writer, including property tag values.
	 */
	public void outputPslFormat(Writer out) throws IOException  {
		outputStandardPsl(out, false);
		outputPropTagVals(out);
		out.write('\n');
	}

	/**
	 *  Writes a line of PSL to a writer, NOT including property tag values.
	 *  @param include_newline  whether to add a newline at the end.
	 */
	protected void outputStandardPsl(Writer out, boolean include_newline)  throws IOException {
		out.write(Integer.toString(matches));
		out.write('\t');
		out.write(Integer.toString(mismatches));
		out.write('\t');
		out.write(Integer.toString(repmatches));
		out.write('\t');
		out.write(Integer.toString(ncount));
		out.write('\t');
		out.write(Integer.toString(qNumInsert));
		out.write('\t');
		out.write(Integer.toString(qBaseInsert));
		out.write('\t');
		out.write(Integer.toString(tNumInsert));
		out.write('\t');
		out.write(Integer.toString(tBaseInsert));
		out.write('\t');
		if (same_orientation) { out.write("+"); }
		else { out.write("-"); }
		out.write('\t');
		out.write(queryseq.getID());
		out.write('\t');
		out.write(Integer.toString(queryseq.getLength()));
		out.write('\t');
		out.write(Integer.toString(qmin));
		out.write('\t');
		out.write(Integer.toString(qmax));
		out.write('\t');
		out.write(targetseq.getID());
		out.write('\t');
		out.write(Integer.toString(targetseq.getLength()));
		out.write('\t');
		out.write(Integer.toString(tmin));
		out.write('\t');
		out.write(Integer.toString(tmax));
		out.write('\t');
		int blockcount = this.getChildCount();
		out.write(Integer.toString(blockcount));
		out.write('\t');
		for (int i=0; i<blockcount; i++) {
			out.write(Integer.toString(blockSizes[i]));
			out.write(',');
		}
		out.write('\t');

		for (int i=0; i<blockcount; i++) {
			if (same_orientation) {
				out.write(Integer.toString(qmins[i]));
			}
			else {
				// dealing with reverse issue
				int mod_qmin = queryseq.getLength() - qmins[i] - blockSizes[i];
				out.write(Integer.toString(mod_qmin));
			}
			out.write(',');
		}
		out.write('\t');
		for (int i=0; i<blockcount; i++) {
			out.write(Integer.toString(tmins[i]));
			out.write(',');
		}
		//out.write('\t');	Unnecessary tab.
		if (include_newline) {
			out.write('\n');
		}
	}

	protected void outputPropTagVals(Writer out)  throws IOException {
		if (props != null) {
			Iterator iter = props.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry)iter.next();
				out.write((String)entry.getKey());
				out.write('=');
				out.write(entry.getValue().toString());
				out.write('\t');
			}
		}
	}

	public void outputBpsFormat(DataOutputStream dos) throws IOException  {
		dos.writeInt(matches);
		dos.writeInt(mismatches);
		dos.writeInt(repmatches);
		dos.writeInt(ncount);
		dos.writeInt(qNumInsert);
		dos.writeInt(qBaseInsert);
		dos.writeInt(tNumInsert);
		dos.writeInt(tBaseInsert);
		dos.writeBoolean(same_orientation);
		dos.writeUTF(queryseq.getID());
		dos.writeInt(queryseq.getLength());
		dos.writeInt(qmin);
		dos.writeInt(qmax);
		dos.writeUTF(targetseq.getID());
		dos.writeInt(targetseq.getLength());
		dos.writeInt(tmin);
		dos.writeInt(tmax);
		int blockcount = this.getChildCount();
		dos.writeInt(blockcount);
		for (int i=0; i<blockcount; i++) {
			dos.writeInt(blockSizes[i]);
		}
		for (int i=0; i<blockcount; i++) {
			dos.writeInt(qmins[i]);
		}
		for (int i=0; i<blockcount; i++) {
			dos.writeInt(tmins[i]);
		}
	}

	/**
	 *  Return a UcscPslSym that is the result of switching
	 *    target and query.
	 */
	public UcscPslSym flipTargetAndQuery() {
		//  if target and query are same orientation, then just need to switch t** with q**
		//  if ! same_orientation, could still just switch, but then can lose advantage
		//      quick interval search over query, since qmins (which used to be tmins)
		//      will likely no longer be in ascending order
		//  therefore to allow one to take advantage of quick query interval search,
		//      if (! same_orientation) then also flipping order of  tmins, qmins, and blocksizes
		int[] new_blockSizes;
		int[] new_qmins;
		int[] new_tmins;

		new_blockSizes = blockSizes;
		new_qmins = tmins;
		new_tmins = qmins;

		if (same_orientation) {
			new_blockSizes = blockSizes;
			new_qmins = tmins;
			new_tmins = qmins;
		}
		else {
			int bcount = blockSizes.length;
			new_blockSizes = new int[bcount];
			new_qmins = new int[bcount];
			new_tmins = new int[bcount];
			for (int i=0; i<bcount; i++) {
				new_blockSizes[i] = blockSizes[bcount-i-1];
				new_qmins[i] = tmins[bcount-i-1];
				new_tmins[i] = qmins[bcount-i-1];
			}
		}

		UcscPslSym new_sym =
			new UcscPslSym(
					type, // String type,
					matches, // int matches,
					mismatches, // int mismatches,
					repmatches, // int repmatches,
					ncount, // int ncount,
					tNumInsert,     // int qNumInsert,
					tBaseInsert, // int qBaseInsert,
					qNumInsert, // int tNumInsert,
					qBaseInsert, // int tBaseInsert,
					same_orientation, // boolean same_orientation,
					targetseq, // MutableAnnotatedBioSeq queryseq,
					tmin, // int qmin,
					tmax, // int qmax,
					queryseq, // MutableAnnotatedBioSeq targetseq,
					qmin, // int tmin,
					qmax, // int tmax,
					blockSizes.length, // int blockcount
					new_blockSizes,  // int[] blockSizes,
					new_qmins,
					new_tmins
						);
		return new_sym;
	}
}
