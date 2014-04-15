package com.affymetrix.genometryImpl.operator;

public final class InverseTransformer extends AbstractFloatTransformer implements Operator {
	final String paramPrompt;
	final String name;

	public InverseTransformer() {
		super();
		paramPrompt = null;
		name = "Inverse";
	}
	public String getParamPrompt() { return null; }
	public String getName() {
		return name;
	}
	public String getDisplay() {
		return name;
	}
	public float transform(float x) {
		return (float)1.0 / x;
	}
	public boolean setParameter(String s) {
		return true;
	}
}
