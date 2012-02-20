/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author auser
 */
public class InverseLogTransform implements Operator{
	
	private static final DecimalFormat DF = new DecimalFormat("#,##0.##");
	double base;
	final String paramPrompt;
	final String name;
	final boolean parameterized;

	public InverseLogTransform() {
		super();
		paramPrompt = "Base";
		name = "Inverse Log";
		parameterized = true;
	}
	public InverseLogTransform(Double base) {
		super();
		this.base = base;
		paramPrompt = null;
		name = getBaseName();
		parameterized = false;
	}
	private String getBaseName() {
		if (base == Math.E) {
			return "Inverse Ln";
		}
		else {
			return "Inverse Log" + DF.format(base);
		}
	}
	@Override
	public String getName() {
		return name;
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
		return FileTypeCategory.Graph;
	}
	
	public boolean setParameter(String s) {
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
		}
		return true;
	}
	
	public float transform(float x) {
		return (float)(Math.pow(base, x));
	}
	
	public String getDisplay() {
		return parameterized ? getBaseName() : name;
	}
}
