package net.sf.picard.reference;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;

public class FastaSequenceIndexExt extends FastaSequenceIndex {
    public FastaSequenceIndexExt( File indexFile ) {
    	super(indexFile);
    }
	public List<BioSeq> getSequenceList() {
		List<BioSeq> sequenceList = new ArrayList<BioSeq>();
		Iterator<FastaSequenceIndexEntry> iter = iterator();
		while (iter.hasNext()) {
			FastaSequenceIndexEntry ent = iter.next();
			BioSeq seq = new BioSeq(ent.getContig(), "", 0);
			sequenceList.add(seq);
		}
		return sequenceList;
	}
}
