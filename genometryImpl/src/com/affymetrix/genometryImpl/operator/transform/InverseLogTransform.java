package com.affymetrix.genometryImpl.operator.transform;

import java.text.DecimalFormat;

public final class InverseLogTransform implements FloatTransformer {
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
	public String getParamPrompt() { return paramPrompt; }
	@Override
	public String getName() {
		return name;
	}
	@Override
	public String getDisplay() {
		return parameterized ? getBaseName() : name;
	}
	@Override
	public float transform(float x) {
		return (float)(Math.pow(base, x));
	}
	@Override
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
}
