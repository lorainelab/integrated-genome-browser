package com.affymetrix.genometryImpl.util;

public final class InverseTransformer implements FloatTransformer {
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
	public float inverseTransform(float x) {
		return 0.0f;
	}
	public boolean isInvertible() { return false; }
	public boolean setParameter(String s) {
		return true;
	}
}
