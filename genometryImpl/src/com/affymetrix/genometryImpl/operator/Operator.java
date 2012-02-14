package com.affymetrix.genometryImpl.operator;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface Operator  {
	public String getName();
	public SeqSymmetry operate(List<SeqSymmetry> symList);
	public int getOperandCountMin(FileTypeCategory category);
	public int getOperandCountMax(FileTypeCategory category);
	public Map<String, Class<?>> getParameters();
	public boolean setParameters( Map<String, Object> obj);
}
