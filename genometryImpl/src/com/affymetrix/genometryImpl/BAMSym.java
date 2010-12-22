package com.affymetrix.genometryImpl;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author hiralv
 */
public class BAMSym extends UcscBedSym implements SymWithResidues{

	private final int[] iblockMins, iblockMaxs;
	Residues residues;
	String insResidues;

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

	public SeqSymmetry getInsChild(int index) {
		if (iblockMins == null || (iblockMins.length <= index)) { return null; }
		if (forward) {
			return new BedChildSingletonSeqSym(iblockMins[index], iblockMaxs[index], seq);
		}
		else {
			return new BedChildSingletonSeqSym(iblockMaxs[index], iblockMins[index], seq);
		}
	}

	@Override
	public SeqSymmetry getChild(int index) {
		if (blockMins == null || (blockMins.length <= index)) { return null; }
		if (forward) {
			return new BamChildSingletonSeqSym(blockMins[index], blockMaxs[index], seq);
		}
		else {
			return new BamChildSingletonSeqSym(blockMaxs[index], blockMins[index], seq);
		}
	}

	class BamChildSingletonSeqSym extends BedChildSingletonSeqSym implements SymWithResidues {

		public BamChildSingletonSeqSym(int start, int end, BioSeq seq) {
			super(start, end, seq);
		}
		
		public void setResidues(String residues) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public String getResidues() {
			return BAMSym.this.getResidues(this.getMin(), this.getMax());
		}

		public String getResidues(int start, int end) {
			return BAMSym.this.getResidues(start, end);
		}

	}

	public void setResidues(String residuesStr){
		if(residues == null){
			residues = new Residues(residuesStr);
			residues.setStart(txMin);
		}else{
			residues.setResidues(residuesStr);
		}
	}

	public String getResidues(){
		if(residues != null){
			return residues.getResidues();
		}
		return getEmptyString(txMax - txMin);
	}

	public String getResidues(int start, int end){
		if(residues != null){
			return residues.getResidues(start, end);
		}
		return getEmptyString(end - start);
	}

	private static String getEmptyString(int length){
		char[] tempArr = new char[length];
		Arrays.fill(tempArr, '-');

		return new String(tempArr);
	}

	public void setInsResidues(String residues){
		this.insResidues = residues;
	}

	public String getInsResidue(int childNo){
		if(childNo > iblockMins.length){
			return "";
		}

		int start = 0;
		for(int i = 0; i < childNo; i++){
			start += (iblockMaxs[i] - iblockMins[i]);
		}
		int end = start + (iblockMaxs[childNo] - iblockMins[childNo]);

		return insResidues.substring(start, end);
	}

	@Override
	public Map<String,Object> cloneProperties() {
		if(props == null){
			props = new HashMap<String, Object>();
		}
		props.put("residues", getResidues());

		return super.cloneProperties();
	}

	@Override
	public Object getProperty(String key){
		if("residues".equalsIgnoreCase(key)){
			return getResidues();
		}
		return super.getProperty(key);
	}
}
