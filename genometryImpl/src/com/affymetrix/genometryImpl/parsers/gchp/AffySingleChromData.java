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

package com.affymetrix.genometryImpl.parsers.gchp;

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.GraphSymByte;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.GraphSymInt;
import com.affymetrix.genometryImpl.GraphSymShort;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.util.ByteList;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;
import com.affymetrix.genometryImpl.util.ShortList;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public final class AffySingleChromData {

	int start;
	int rowCount;
	int chromNum; // in chp file, chromosome is indicated both by an arbitrary byte
	String displayName; // AND by a display name.

	List<AffyChpColumnData> columns = new ArrayList<AffyChpColumnData>();
	AffyGenericChpFile chpFile;
	AffyDataSet dataSet;

	/** Creates a new instance of SingleChromosomeData */
	public AffySingleChromData(AffyGenericChpFile chpFile, AffyDataSet dataSet, 
			int chromNum, String chromDisplayName, int start, int count, 
			List<AffyChpColumnData> columns) {
		this.chpFile = chpFile;
		this.dataSet = dataSet;
		this.chromNum = chromNum;
		this.displayName = chromDisplayName;
		this.start = start;
		this.rowCount = count;
		this.columns = columns;
	}

	@Override
		public String toString() {
			return this.getClass().getName() + " [displayName=" + displayName 
				+ ", start=" + start + ", count="
				+ rowCount + ", columns=" + columns.size()+"]";
		}

	void parse(DataInputStream dis) throws IOException {
		SingletonGenometryModel.logDebug("Parsing chromData: " + this.displayName + ", " + this.rowCount);
		for (int row=0; row < rowCount; row++) {
			for (AffyChpColumnData col : columns) {
				col.addData(dis);
			}
		}
	}

	int totalRowSize() {
		int rowSize = 0;
		for (AffyChpColumnData col : columns) {
			rowSize += col.getByteLength();
		}
		return rowSize;
	}

	void skip(DataInputStream dis) throws IOException {
		int rowSize = totalRowSize();
		long skipSize = (long)rowCount * rowSize;	// cast to long before multiplying to avoid possible overflow

		while (skipSize > 0) {
			long skipped = dis.skip(skipSize);
			skipSize -= skipped;
		}     
	}

	void parseOrSkip(DataInputStream dis) throws IOException {
		if (this.chpFile.getLoadPolicy().shouldLoadChrom(displayName)) {
			parse(dis);
		} else {
			skip(dis);
		}
	}

	/** Creates GraphSyms that can be added as annotations to the BioSeq. */
	public List<SeqSymmetry> makeGraphs(AnnotatedBioSeq seq) throws IOException {
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>(columns.size());

		ArrayList<CharSequence> probeSetNames = (ArrayList<CharSequence>) columns.get(0).getData();
		probeSetNames.trimToSize();

		// column 2 contains chromosome number, but we already know that information so ignore it.

		IntList positions = (IntList) columns.get(2).getData();
		positions.trimToSize();


		if (positions.size() > -1) { 
			// add a graph even if the data is of length 0
			// because we want something to be visible in the display, even if it is
			// simply a graph handle and axis with no graph data to draw.
			boolean addSingletonSyms = false;
			if (addSingletonSyms) {
				String theMethod = "posistions";
				//        TypeContainerAnnot tca = new TypeContainerAnnot(theMethod);
				//        tca.setProperty("method", theMethod);
				//        tca.setID(theMethod);
				//        results.add(sym);
				////        seq.addAnnotation(tca);
				for (int i = 0; i < positions.size(); i++) {
					//TODO: insn't there a class that accepts an IntList as the set of positions?
					final int start_pos = positions.get(i);
					SingletonSymWithProps sym = new SingletonSymWithProps(probeSetNames.get(i), start_pos, start_pos + 1, seq);
					sym.setProperty("method", theMethod);
					//          tca.addChild(sym);
					results.add(sym);
					//          seq.addAnnotation(sym);
				}
			}

			// In a cnchp file, the first three columns contain non-graph data 
			// so skip them and make graphs from all the other columns
			//TODO: maybe make this more generic for all "generic" chp files
			for (AffyChpColumnData colData : columns.subList(3,columns.size())) {
				String graphId = colData.name;
				if (colData.getData() instanceof FloatList) {
					List<Object> trimmedXandY = trimNaN(positions, (FloatList) colData.getData());
					IntList xlist = (IntList) trimmedXandY.get(0);
					FloatList flist = (FloatList) trimmedXandY.get(1);

					xlist.trimToSize();
					flist.trimToSize();
					GraphSymFloat gsym = new GraphSymFloat(xlist.getInternalArray(), flist.getInternalArray(), graphId, seq);
					//          seq.addAnnotation(gsym);
					results.add(gsym);
				} else if (colData.getData() instanceof IntList) {
					IntList ilist = (IntList) colData.getData();
					ilist.trimToSize();
					GraphSymInt gsym = new GraphSymInt(positions.getInternalArray(), ilist.getInternalArray(), graphId, seq);
					//          seq.addAnnotation(gsym);
					results.add(gsym);
				} else if (colData.getData() instanceof ShortList) {
					GraphSymShort gsym;
					if (colData.name.startsWith("CNState")) {
						// In the "CNStateMin" and "CNStateMax" graphs, the number "255"
						// is used to represent "unknown".  These x,y pairs should be discarded.
						List<Object> trimmedXandY = trim255(positions, (ShortList) colData.getData());
						IntList xlist = (IntList) trimmedXandY.get(0);
						ShortList ilist = (ShortList) trimmedXandY.get(1);

						xlist.trimToSize();
						ilist.trimToSize();
						gsym = new GraphSymShort(xlist.getInternalArray(), ilist.getInternalArray(), graphId, seq);
					} else {
						ShortList ilist = (ShortList) colData.getData();
						ilist.trimToSize();
						gsym = new GraphSymShort(positions.getInternalArray(), ilist.getInternalArray(), graphId, seq);
					}
					//          seq.addAnnotation(gsym);
					results.add(gsym);
				} else if (colData.getData() instanceof ByteList) {
					ByteList ilist = (ByteList) colData.getData();
					ilist.trimToSize();
					GraphSymByte gsym = new GraphSymByte(positions.getInternalArray(), ilist.getInternalArray(), graphId, seq);
					//          seq.addAnnotation(gsym);
					results.add(gsym);
				} else {
					SingletonGenometryModel.logError("Don't know how to make a graph for data of type: " + colData.type);
				}
			}
		}

		return results;
	}

	/** Removes x,y pairs where the y-value is invalid (NaN or Infinite).
	 *  Returns a List containing one IntList and one FloatList. 
	 *  If there were no invalid values of y, the output IntList and FloatList
	 *  will be the same objects as the input, otherwise they will both be
	 *  new objects.  If the given FloatList contains ONLY invalid values,
	 *  then the returned IntList and FloatList will both be empty.
	 */
	List<Object> trimNaN(IntList x, FloatList y) {
		if (x.size() != y.size()) {
			throw new IllegalArgumentException("Lists must be the same size " + x.size() + " != " + y.size());
		}

		boolean had_bad_values = false;
		IntList x_out = new IntList(x.size());
		FloatList y_out = new FloatList(y.size());

		for (int i=0; i<x.size(); i++) {
			float f = y.get(i);
			if (Float.isNaN(f) || Float.isInfinite(f)) {
				had_bad_values = true;
			} else {
				x_out.add(x.get(i));
				y_out.add(f);
			}
		}

		if (had_bad_values) {
			return Arrays.<Object>asList(x_out, y_out);
		} else {
			return Arrays.<Object>asList(x, y);
		}
	}

	/** Removes x,y pairs where the y-value is invalid (byte = 255).
	 *  Returns a List containing one IntList and one ShortList. 
	 *  If there were no invalid values of y, the output IntList and ShortList
	 *  will be the same objects as the input, otherwise they will both be
	 *  new objects.  If the given ShortList contains ONLY invalid values,
	 *  then the returned IntList and 3hortList will both be empty.
	 */
	List<Object> trim255(IntList x, ShortList y) {
		if (x.size() != y.size()) {
			throw new IllegalArgumentException("Lists must be the same size " + x.size() + " != " + y.size());
		}

		boolean had_bad_values = false;
		IntList x_out = new IntList(x.size());
		ShortList y_out = new ShortList(y.size());

		for (int i=0; i<x.size(); i++) {
			short f = y.get(i);
			if (f == 255) {
				had_bad_values = true;
			} else {
				x_out.add(x.get(i));
				y_out.add(f);
			}
		}

		if (had_bad_values) {
			return Arrays.<Object>asList(x_out, y_out);
		} else {
			return Arrays.<Object>asList(x, y);
		}
	}
}
