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
	private static FileTypeCategory input[] = FileTypeCategory.values();
	private static FileTypeCategory output = null;
	private static final String PARM_NAME = "parameter";
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
		Map<String, Class<?>> parameters = new HashMap<String, Class<?>>();
		parameters.put(PARM_NAME, String.class);
		return parameters;
	}

	@Override
	public boolean setParameters(Map<String, Object> parms) {
		if (parms.size() == 1 && parms.get(PARM_NAME) instanceof String) {
			filter.setParam(parms.get(PARM_NAME));
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
		return output;
	}

	@Override
	public FileTypeCategory[] getInputCategory() {
		return input; 
	}
}
