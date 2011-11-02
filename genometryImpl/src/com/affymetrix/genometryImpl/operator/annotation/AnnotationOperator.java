package com.affymetrix.genometryImpl.operator.annotation;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *  A simple interface for arbitrary operations of annotation glyphs.
 */
public interface AnnotationOperator  {
	public String getName();
	// only one operate method should be implemented, the other should return null
	public SeqSymmetry operate(List<SeqSymmetry> symList);
	public SeqSymmetry operate(BioSeq seq, List<List<SeqSymmetry>> symList);
	public int getOperandCountMin();
	public int getOperandCountMax();
}
