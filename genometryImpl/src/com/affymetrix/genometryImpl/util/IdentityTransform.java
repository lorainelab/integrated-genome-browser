package com.affymetrix.genometryImpl.util;

public final class IdentityTransform implements FloatTransformer {
	public IdentityTransform() {}
	public String getParamPrompt() { return null; }
	public String getName() { return "Copy"; }
	public String getDisplay() { return "Copy"; }
	public float transform(float x) { return x; }
	public float inverseTransform(float x) { return x; }
	public boolean isInvertible() { return true; }
	public boolean setParameter(String s) { return true; }
}
