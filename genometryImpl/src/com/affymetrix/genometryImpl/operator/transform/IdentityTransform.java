package com.affymetrix.genometryImpl.operator.transform;

public final class IdentityTransform implements FloatTransformer {
	public IdentityTransform() {}
	@Override
	public String getParamPrompt() { return null; }
	@Override
	public String getName() { return "Copy"; }
	@Override
	public String getDisplay() { return "Copy"; }
	@Override
	public float transform(float x) { return x; }
	@Override
	public boolean setParameter(String s) { return true; }
}
