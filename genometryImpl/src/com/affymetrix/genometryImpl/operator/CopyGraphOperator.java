package com.affymetrix.genometryImpl.operator;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public final class CopyGraphOperator implements Operator {
	public CopyGraphOperator() {}
	@Override
	public String getName() { return "copygraph"; }
	@Override
	public SeqSymmetry operate(List<SeqSymmetry> symList) {
		if (symList.size() != 1 || !(symList.get(0) instanceof GraphSym)) {
			return null;
		}
		GraphSym sourceSym = (GraphSym)symList.get(0);
		GraphSym graphSym;
		int[] x = new int[sourceSym.getGraphXCoords().length];
		System.arraycopy(sourceSym.getGraphXCoords(), 0, x, 0, sourceSym.getGraphXCoords().length);
		float[] y = new float[sourceSym.getGraphYCoords().length];
		System.arraycopy(sourceSym.getGraphYCoords(), 0, y, 0, sourceSym.getGraphYCoords().length);
		String id = sourceSym.getID();
		BioSeq seq = sourceSym.getGraphSeq();
		if (sourceSym.hasWidth()) {
			int[] w = new int[sourceSym.getGraphWidthCoords().length];
			System.arraycopy(sourceSym.getGraphWidthCoords(), 0, w, 0, sourceSym.getGraphWidthCoords().length);
			graphSym = new GraphSym(x, w, y, id, seq);
		}
		else {
			graphSym = new GraphSym(x, y, id, seq);
		}
		return graphSym;
	}
	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Graph ? 1 : 0;
	}
	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Graph ? 1 : 0;
	}
	@Override
	public Map<String, Class<?>> getParameters() {
		return null;
	}
	@Override
	public boolean setParameters(Map<String, Object> obj) {
		return false;
	}
}
