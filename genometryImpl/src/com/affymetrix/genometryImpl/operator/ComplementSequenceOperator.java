package com.affymetrix.genometryImpl.operator;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;

/**
 * operation for sequenc tracks that returns the complement base at each location
 */
public class ComplementSequenceOperator implements Operator {
	private static final char[] COMPLEMENT = new char[256];
	static {
		COMPLEMENT['A'] = 'T';
		COMPLEMENT['T'] = 'A';
		COMPLEMENT['C'] = 'G';
		COMPLEMENT['G'] = 'C';
		COMPLEMENT['N'] = 'N';
		COMPLEMENT['a'] = 't';
		COMPLEMENT['t'] = 'a';
		COMPLEMENT['c'] = 'g';
		COMPLEMENT['g'] = 'c';
		COMPLEMENT['n'] = 'n';
	}

	@Override
	public String getName() {
		return "complement";
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		SimpleSymWithResidues residueSym = (SimpleSymWithResidues)symList.get(0).getChild(0);
		String residues = residueSym.getResidues();
		StringBuffer complementResidues = new StringBuffer(residues.length());
		for (int i = 0; i < residues.length(); i++) {
			complementResidues.append(COMPLEMENT[residues.charAt(i)]);
		}
		return new SimpleSymWithResidues("complement:" + residueSym.getType(), residueSym.getBioSeq(), residueSym.getMin(),
				residueSym.getMax(), residueSym.getName(), residueSym.getScore(), residueSym.isForward(),
				residueSym.hasCdsSpan() ? residueSym.getCdsSpan().getMin() : Integer.MIN_VALUE,
				residueSym.hasCdsSpan() ? residueSym.getCdsSpan().getMax() : Integer.MAX_VALUE,
				residueSym.getBlockMins(), residueSym.getBlockMaxs(), complementResidues.toString());
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Sequence ? 1 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Sequence ? 1 : 0;
	}

	@Override
	public Map<String, Class<?>> getParameters() {
		return null;
	}

	@Override
	public boolean setParameters(Map<String, Object> parms) {
		return false;
	}

	@Override
	public boolean supportsTwoTrack() {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Sequence;
	}
}
