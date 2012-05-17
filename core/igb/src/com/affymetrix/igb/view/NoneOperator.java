
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.tiers.TrackConstants;
import java.util.List;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class NoneOperator implements Operator{

	@Override
	public String getName() {
		return TrackConstants.default_operator;
	}

	@Override
	public String getDisplay() {
		return TrackConstants.default_operator;
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		return null;
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return 1;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return 1;
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
		return true;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return null;
	}
	
}
