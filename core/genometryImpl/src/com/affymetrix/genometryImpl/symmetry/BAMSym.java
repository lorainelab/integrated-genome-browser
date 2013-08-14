package com.affymetrix.genometryImpl.symmetry;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;

/**
 *
 * @author hiralv
 */
public class BAMSym extends BasicSeqSymmetry implements SymWithResidues, SearchableCharIterator {
	public static final int NO_MAPQ = 255;
	
	private static final char DELETION_CHAR = '_';
	private static final char N_CHAR = '-';
	private static final char PADDING_CHAR = '*';
	private static final char ERROR_CHAR = '.';
	
	private final int[] iblockMins, iblockMaxs;
	private final Cigar cigar;
	private final int min;
	private final String residues;
	private final int mapq;
	
	//Residues residues;
	private String insResidues;

	public BAMSym(String type, BioSeq seq, int txMin, int txMax, String name,
			boolean forward, int[] blockMins, int[] blockMaxs, int iblockMins[], 
			int[] iblockMaxs, Cigar cigar, String residues) {
		this(type, seq, txMin, txMax, name, NO_MAPQ, forward, blockMins, 
				blockMaxs, iblockMins, iblockMaxs, cigar, residues);
	}

	public BAMSym(String type, BioSeq seq, int txMin, int txMax, String name, 
			int mapq, boolean forward, int[] blockMins, int[] blockMaxs,
			int iblockMins[], int[] iblockMaxs, Cigar cigar, String residues){
		super(type, seq, txMin, txMax, name, forward, blockMins, blockMaxs);
		this.iblockMins = iblockMins;
		this.iblockMaxs = iblockMaxs;
		this.cigar = cigar;
		this.residues = residues;
		this.min  = Math.min(txMin, txMax);
		this.mapq = mapq;
	}
	
	public int getMapq() {
		return mapq;
	}
	
	public int getInsChildCount() {
		if (iblockMins == null)  { return 0; }
		else  { return iblockMins.length; }
	}

	public SeqSymmetry getInsChild(int index) {
		if (iblockMins == null || (iblockMins.length <= index)) { return null; }
		if (forward) {
			return new BamInsChildSingletonSeqSym(iblockMins[index], iblockMaxs[index], index, seq);
		}
		else {
			return new BamInsChildSingletonSeqSym(iblockMaxs[index], iblockMins[index], index, seq);
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
	
	public String substring(int start, int end) {
		return getResidues(start, end);
	}

	public int indexOf(String searchstring, int offset) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	class BamChildSingletonSeqSym extends SingletonSeqSymmetry implements SymWithResidues {

		public BamChildSingletonSeqSym(int start, int end, BioSeq seq) {
			super(start, end, seq);
		}
		
		public String getResidues() {
			return interpretCigar(this.getMin(), this.getMax(), false);
		}

		public String getResidues(int start, int end) {
			return interpretCigar(start, end, false);
		}

		// For the web links to be constructed properly, this class must implement getID(),
		// or must NOT implement SymWithProps.
		@Override public String getID() {return BAMSym.this.getID();}
		@Override public Object getProperty(String key) {return BAMSym.this.getProperty(key);}
		@Override public boolean setProperty(String key, Object val) {return false; }
		@Override public Map<String,Object> getProperties() {return cloneProperties();}
		
		@Override
		public Map<String,Object> cloneProperties() {
			HashMap<String,Object> tprops = new HashMap<String,Object>();
			tprops.putAll(BAMSym.this.cloneProperties());
			tprops.put("id", name);
			tprops.put("residues", getResidues());
			tprops.put("forward", this.isForward());
			return tprops;
		}
	}

	class BamInsChildSingletonSeqSym extends SingletonSeqSymmetry implements SymWithResidues {
		final int index;
		public BamInsChildSingletonSeqSym(int start, int end, int index, BioSeq seq) {
			super(start, end, seq);
			this.index = index;
		}

		public String getResidues(int start, int end) {
			throw new UnsupportedOperationException("Not supported yet.");
		}
		
		public String getResidues() {
			return interpretCigar(this.getMin(), this.getMax(), true);
		}

		// For the web links to be constructed properly, this class must implement getID(),
		// or must NOT implement SymWithProps.
		@Override public String getID() {return BAMSym.this.getID();}
		@Override public Object getProperty(String key) {return BAMSym.this.getProperty(key);}
		@Override public boolean setProperty(String key, Object val) {return false; }
		@Override public Map<String,Object> getProperties() {return cloneProperties();}
		
		@Override
		public Map<String,Object> cloneProperties() {
			HashMap<String,Object> tprops = new HashMap<String,Object>();
			tprops.putAll(BAMSym.this.cloneProperties());
			tprops.put("id", name);
			tprops.put("residues", getResidues());
			tprops.put("forward", this.isForward());
			tprops.put("feature_type", "insertion"); 
			return tprops;
		}
	}

	public String getResidues(){
		if(residues != null){
			return residues;
		}
		return getEmptyString(txMax - txMin);
	}

	public String getResidues(int start, int end){
		if(residues != null){
			
			return interpretCigar(start, end, false);
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
		props.put("residues", getResidues().replaceAll("-", ""));
		props.put("mapq", mapq);
		
		return super.cloneProperties();
	}

	@Override
	public Object getProperty(String key){
		if("residues".equalsIgnoreCase(key)){
			return getResidues();
		}
		return super.getProperty(key);
	}
	
	public Cigar getCigar() {
		return cigar;
	}

	private String interpretCigar(int start, int end, boolean isIns) {
		return interpretCigar(residues, start, end, isIns, DELETION_CHAR, N_CHAR, PADDING_CHAR, ERROR_CHAR);
	}

	private String interpretCigar(String str, int start, int end, boolean isIns,
			char D, char N, char P, char E) {
		if (cigar == null || cigar.numCigarElements() == 0 || str == null) {
			return "";
		}
		start = Math.max(start, txMin);
		end = Math.min(txMax, end);

		start = start - min;
		end = end - min;

		if (start > end) {
			return "";
		}

		char[] sb = new char[end - start];
		int stringPtr = 0;
		int currentPos = 0;
		int offset = 0;
		int celLength;
		char[] tempArr;
		for (CigarElement cel : cigar.getCigarElements()) {
			try {
				if (offset >= sb.length) {
					return String.valueOf(sb);
				}

				celLength = cel.getLength();
				tempArr= new char[celLength];

				if (cel.getOperator() == CigarOperator.INSERTION) {
					if (isIns && currentPos == start) {
						return str.substring(stringPtr, stringPtr + celLength);
					} else {
						stringPtr += celLength;
						continue;
					}
				} else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
					stringPtr += celLength;	// skip over soft clip
					continue;
				} else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
					continue;				// hard clip can be ignored
				} else if (cel.getOperator() == CigarOperator.DELETION) {
					Arrays.fill(tempArr, D);		// print deletion as '_'
					currentPos += celLength;
				} else if (cel.getOperator() == CigarOperator.M) {
					tempArr = str.substring(stringPtr, stringPtr + celLength).toCharArray();
					stringPtr += celLength;	// print matches
					currentPos += celLength;
				} else if (cel.getOperator() == CigarOperator.N) {
					Arrays.fill(tempArr, N);
					currentPos += celLength;
				} else if (cel.getOperator() == CigarOperator.PADDING) {
					Arrays.fill(tempArr, P);		// print padding as '*'
					stringPtr += celLength;
					currentPos += celLength;
				}

				if (currentPos > start) {
					int tempOffset = Math.max(tempArr.length - (currentPos - start), 0);
					int len = Math.min(tempArr.length - tempOffset, sb.length - offset);
					System.arraycopy(tempArr, tempOffset, sb, offset, len);
					offset += len;
				}

			} catch (Exception ex) {
				ex.printStackTrace();
				if ((end - start) - stringPtr > 0) {
					tempArr = new char[(end - start) - stringPtr];
					Arrays.fill(tempArr, E);
					System.arraycopy(tempArr, 0, sb, 0, tempArr.length);
				}
			}
		}

		return String.valueOf(sb);
	}
}
