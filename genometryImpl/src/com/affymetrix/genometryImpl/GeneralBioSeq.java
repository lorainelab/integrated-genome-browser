package com.affymetrix.genometryImpl;

import com.affymetrix.genometry.seq.SimpleCompAnnotBioSeq;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;

/**
 *  <pre>
 *  Implements SearchableCharacterIterator to allow for regex matching
 *    without having to build a String out of the byte array (which would kind of
 *    defeat the purpose of saving memory...)
 * </pre>
 */
public abstract class GeneralBioSeq extends SimpleCompAnnotBioSeq
	implements SearchableCharIterator {

	protected String version;
	protected SearchableCharIterator residues_provider;

	public GeneralBioSeq(String seqid, String seqversion, int length) {
		super(seqid, length);
		this.version = seqversion;
	}


	/** Gets residues.
	 *  @param fillchar  Character to use for missing residues;
	 *     warning: this parameter is used only if {@link #getResiduesProvider()} is null.
	 */
	@Override
	public String getResidues(int start, int end, char fillchar) {
		String result = null;
		if (residues_provider == null)  {
			// fall back on SimpleCompAnnotSeq (which will try both residues var and composition to provide residues)
			//      result = super.getResidues(start, end, fillchar);
			result = super.getResidues(start, end, '-');
		}
		else {
			result = residues_provider.substring(start, end);
		}
		return result;
	}


	@Override
	public boolean isComplete(int start, int end) {
		if (residues_provider != null) { return true; }
		else { return super.isComplete(start, end); }
	}

	public char charAt(int pos) {
		if (residues_provider == null) {
			String str = super.getResidues(pos, pos+1, '-');
			if (str == null) { return '-'; }
			else { return str.charAt(0); }
		}
		else {
			return residues_provider.charAt(pos);
		}
	}


	public String substring(int start, int end) {
		if (residues_provider == null) {
			return super.getResidues(start, end);
		}
		return residues_provider.substring(start, end);
	}

	public int indexOf(String str, int fromIndex) {
		// TODO: this will fail if residues_provider is null, so may need to call inside try/catch clause
		if (residues_provider != null) {
			return residues_provider.indexOf(str, fromIndex);
		}
		return this.getResidues().indexOf(str, fromIndex);
	}
}



