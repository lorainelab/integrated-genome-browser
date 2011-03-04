package com.affymetrix.genometryImpl.util;

import java.text.DecimalFormat;

public final class PowerTransformer implements FloatTransformer {
	private static final DecimalFormat DF = new DecimalFormat("#,##0.##");
	double exponent;
	final String paramPrompt;
	final String name;
	final boolean parameterized;

	public PowerTransformer() {
		super();
		paramPrompt = "Exponent";
		name = "Power";
		parameterized = true;
	}
	public PowerTransformer(Double exponent) {
		super();
		this.exponent = exponent;
		paramPrompt = null;
		name = getBaseName();
		parameterized = false;
	}
	private String getBaseName() {
		if (exponent == 0.5) {
			return "Sqrt";
		}
		else {
			return "Power" + DF.format(exponent);
		}
	}
	public String getParamPrompt() { return paramPrompt; }
	public String getName() {
		return name;
	}
	public String getDisplay() {
		return parameterized ? getBaseName() : name;
	}
	public float transform(float x) {
		return (float)Math.pow(x, exponent);
	}
	public float inverseTransform(float x) {
		return (float)Math.pow(x, 1.0 / exponent);
	}
	public boolean isInvertible() { return true; }
	public boolean setParameter(String s) {
		if (parameterized) {
			if ("sqrt".equals(s.trim().toLowerCase())) {
				exponent = 0.5;
			}
			else {
				try {
					exponent = Double.parseDouble(s);
				}
				catch (Exception x) {
					return false;
				}
			}
		}
		return true;
	}
}
