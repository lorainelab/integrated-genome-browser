package com.affymetrix.genometryImpl.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;

public class FilterOperator implements Operator {
	private final FileTypeCategory category;
	private final SymmetryFilterI filter;

	public FilterOperator(FileTypeCategory category, SymmetryFilterI filter) {
		super();
		this.category = category;
		this.filter = filter;
	}

	@Override
	public String getName() {
		return "filter_operator_" + category.toString() + "_" + filter.getName();
	}

	@Override
	public String getDisplay() {
		return getName();
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		if (symList.size() != 1) {
			return null;
		}
		SimpleSymWithProps sym = new SimpleSymWithProps();
		for (int i = 0; i < symList.get(0).getChildCount(); i++) {
			SeqSymmetry child = symList.get(0).getChild(i);
			if (filter.filterSymmetry(aseq, child)) {
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
		return category == this.category ? Integer.MAX_VALUE : 0;
	}

	@Override
	public Map<String, Class<?>> getParameters() {
		Map<String, Class<?>> parameters = new HashMap<String, Class<?>>();
		parameters.put(filter.getName(), String.class);
		return parameters;
	}

	@Override
	public boolean setParameters(Map<String, Object> parms) {
		if (parms.size() == 1 && parms.get(filter.getName()) instanceof String) {
			filter.setParam(parms.get(filter.getName()));
			return true;
		}
		return false;
	}

	@Override
	public boolean supportsTwoTrack() {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return category;
	}
}
