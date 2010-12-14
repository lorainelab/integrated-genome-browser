package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

/**
 *
 * @author hiralv
 */
public class BAMSym extends UcscBedSym{

	private final int[] iblockMins, iblockMaxs;

	public BAMSym(String type, BioSeq seq, int txMin, int txMax, String name, float score,
			boolean forward, int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs,
			int iblockMins[], int[] iblockMaxs){
		super(type,seq,txMin,txMax,name,score,forward,cdsMin,cdsMax,blockMins,blockMaxs);
		this.iblockMins = iblockMins;
		this.iblockMaxs = iblockMaxs;
	}

	public int getInsChildCount() {
		if (iblockMins == null)  { return 0; }
		else  { return iblockMins.length; }
	}

	public SeqSpan getInsChild(int index) {
		if (iblockMins == null || (iblockMins.length <= index)) { return null; }
		if (forward) {
			return new SimpleSeqSpan(iblockMins[index], iblockMaxs[index], seq);
		}
		else {
			return new SimpleSeqSpan(iblockMaxs[index], iblockMins[index], seq);
		}
	}
}
