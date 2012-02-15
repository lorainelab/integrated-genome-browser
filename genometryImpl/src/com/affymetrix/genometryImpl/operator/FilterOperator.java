package com.affymetrix.genometryImpl.operator;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;

public class FilterOperator implements Operator {
	private final SymmetryFilterI filter;

	public FilterOperator(SymmetryFilterI filter) {
		super();
		this.filter = filter;
	}

	@Override
	public String getName() {
		return "filter operator " + filter.getName();
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		if (symList.size() != 1) {
			return null;
		}
		SimpleSymWithProps sym = new SimpleSymWithProps();
		for (int i = 0; i < symList.get(0).getChildCount(); i++) {
			SeqSymmetry child = symList.get(0).getChild(i);
			if (filter.filterSymmetry(child)) {
				sym.addChild(child);
			}
		}
		return sym;
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return 0;
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
	public boolean setParameters(Map<String, Object> obj) {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return null;
	}
}
