package com.affymetrix.genometry.util;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * Used to index the symmetries for interval searches.
 */
public final class IndexedSyms {
	public final File file;
	public final String typeName;
	public final String ext;
	public final IndexWriter iWriter;
	public final int[] min;
	public final int[] max;
	public final BitSet forward;
	public final long[] filePos;
	// for each sym, we have an array of ids generated from the group's id2symhash.
	// Each of these ids is in a byte array instead of a String to save memory
	public final byte[][][] id;

	public IndexedSyms(int resultSize, File file, String typeName, String ext, IndexWriter iWriter) {
		min = new int[resultSize];
		max = new int[resultSize];
		forward = new BitSet(resultSize);
		id = new byte[resultSize][][];
		filePos = new long[resultSize + 1];
		this.file = file;
		this.typeName = typeName;
		this.iWriter = iWriter;
		this.ext = ext;
	}

	public void setIDs(AnnotatedSeqGroup group, String symID, int i) {
		if (symID == null) {
			// no IDs
			this.id[i] = null;
			return;
		}
		// determine list of IDs for this symmetry index.
		Set<String> extraNames = group.getSymmetryIDs(symID.toLowerCase());
		List<String> ids = new ArrayList<String>(1 + (extraNames == null ? 0 : extraNames.size()));
		ids.add(symID);
		if (extraNames != null) {
			ids.addAll(extraNames);
		}
		int idSize = ids.size();
		this.id[i] = new byte[idSize][];
		for (int j = 0; j < idSize; j++) {
			this.id[i][j] = ids.get(j).getBytes();
		}
	}

	public SimpleSymWithProps convertToSymWithProps(int i, BioSeq seq, String type) {
		SimpleSymWithProps sym = new SimpleSymWithProps();
		String id = this.id[i] == null ? "" : new String(this.id[i][0]);
		sym.setID(id);
		sym.setProperty("name", id);
		sym.setProperty("method", type);
		if (this.forward.get(i)) {
			sym.addSpan(new SimpleSeqSpan(this.min[i], this.max[i], seq));
		} else {
			sym.addSpan(new SimpleSeqSpan(this.max[i], this.min[i], seq));
		}
		return sym;
	}
    
}
