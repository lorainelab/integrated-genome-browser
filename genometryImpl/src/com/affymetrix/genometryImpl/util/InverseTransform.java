package com.affymetrix.genometryImpl.util;

/**
 *  invert the transform
 */
public final class InverseTransform implements FloatTransformer {
	FloatTransformer inner_trans;
	public InverseTransform(FloatTransformer originalTransformer) {
		inner_trans = originalTransformer;
	}
	public String getParamPrompt() { return null; }
	public String getName() {
		return "Inverse " + inner_trans.getName();
	}
	public String getDisplay() {
		return "Inverse " + inner_trans.getDisplay();
	}
	public float transform(float x) { return inner_trans.inverseTransform(x); }
	public float inverseTransform(float x) { return inner_trans.transform(x); }
	public boolean isInvertible() { return true; }
	public boolean setParameter(String s) {
		return inner_trans.setParameter(s);
	}
}
