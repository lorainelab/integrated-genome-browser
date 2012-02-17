package com.affymetrix.genometryImpl.operator;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public final class LogTransform implements Operator {
	private static FileTypeCategory input[] = new FileTypeCategory[]{FileTypeCategory.Graph};
	private static FileTypeCategory output = FileTypeCategory.Graph;
	private static final DecimalFormat DF = new DecimalFormat("#,##0.##");
	double base;
	double LN_BASE;
	float LOG_1;
	final String paramPrompt;
	final String name;
	final boolean parameterized;

	public LogTransform() {
		super();
		paramPrompt = "Base";
		name = "Log";
		parameterized = true;
	}
	public LogTransform(Double base) {
		super();
		this.base = base;
		LN_BASE = Math.log(base);
		LOG_1 = (float)(Math.log(1)/LN_BASE);
		paramPrompt = null;
		name = getBaseName();
		parameterized = false;
	}
	private String getBaseName() {
		if (base == Math.E) {
			return "Natural Log";
		}
		else {
			return "Log" + DF.format(base);
		}
	}
	@Override
	public String getName() {
		return parameterized ? getBaseName() : name;
	}
	private float transform(float x) {
		return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
	}

	private boolean setParameter(String s) {
		if (parameterized) {
			if ("e".equals(s.trim().toLowerCase())) {
				base = Math.E;
			}
			else {
				try {
					base = Double.parseDouble(s);
					if (base <= 0) {
						return false;
					}
				}
				catch (Exception x) {
					return false;
				}
			}
			LN_BASE = Math.log(base);
			LOG_1 = (float)(Math.log(1)/LN_BASE);
		}
		return true;
	}
	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		if (symList.size() != 1 || !(symList.get(0) instanceof GraphSym)) {
			return null;
		}
		GraphSym sourceSym = (GraphSym)symList.get(0);
		GraphSym graphSym;
		int[] x = new int[sourceSym.getGraphXCoords().length];
		System.arraycopy(sourceSym.getGraphXCoords(), 0, x, 0, sourceSym.getGraphXCoords().length);
		float[] sourceY = sourceSym.getGraphYCoords();
		float[] y = new float[sourceY.length];
		for (int i = 0; i < sourceY.length; i++) {
			y[i] = transform(sourceY[i]);
		}
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
		if (paramPrompt == null) {
			return null;
		}
		Map<String, Class<?>> parameters = new HashMap<String, Class<?>>();
		parameters.put(paramPrompt, String.class);
		return parameters;
	}
	@Override
	public boolean setParameters(Map<String, Object> parms) {
		if (paramPrompt != null && parms.size() == 1 && parms.get(paramPrompt) instanceof String) {
			setParameter((String)parms.get(paramPrompt));
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
