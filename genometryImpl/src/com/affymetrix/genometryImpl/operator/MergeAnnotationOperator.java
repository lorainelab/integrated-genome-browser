package com.affymetrix.genometryImpl.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;

public class MergeAnnotationOperator implements Operator {

	@Override
	public String getName() {
		return "mergeannotation";
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, List<SeqSymmetry> symList) {
		SimpleSymWithProps result = new SimpleSymWithProps();
		result.setProperties(new HashMap<String,Object>());
		for (int i = 0; i < symList.size(); i++) {
			if (symList.get(i) instanceof TypeContainerAnnot) {
				TypeContainerAnnot t = (TypeContainerAnnot)symList.get(i);
				if (result.getProperty("type") == null) {
					result.setProperty("type", t.getType());
				}
				// copy children
				for (int j = 0; j < t.getChildCount(); j++) {
					result.addChild(t.getChild(j));
				}
				// copy spans
				for (int j = 0; j < t.getSpanCount(); j++) {
					result.addSpan(t.getSpan(j));
				}
				// copy properties
				Map<String, Object> properties = t.getProperties();
				for (String key : properties.keySet()) {
					Object val = result.getProperty(key);
					Object loopVal = properties.get(key);
					if (val == null) {
						result.setProperty(key, loopVal);
					}
					else if (val instanceof String && loopVal instanceof String && ("," + val + ",").indexOf("," + loopVal + ",") == -1) {
						result.setProperty(key, ((String)val) + "," + loopVal);
					}
				}
			}
		}
		if (result.getID() == null) {
			result.setProperty("id", "");
		}
		return result;
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? 2 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? Integer.MAX_VALUE : 0;
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
	public boolean supportsTwoTrack() {
		return true;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Annotation;
	}
}
