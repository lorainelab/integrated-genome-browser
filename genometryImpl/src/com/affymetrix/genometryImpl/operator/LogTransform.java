package com.affymetrix.genometryImpl.operator;


public final class LogTransform extends AbsractLogTransform implements Operator{
	double LN_BASE;
	float LOG_1;

	public LogTransform() {
		super("Log");
	}
	
	public LogTransform(Double base) {
		super(base);
		LN_BASE = Math.log(base);
		LOG_1 = (float)(Math.log(1)/LN_BASE);
	}
	
	@Override
	protected String getBaseName() {
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
	
	public float transform(float x) {
		return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
	}

	@Override
	protected boolean setParameter(String s) {
		if (parameterized && super.setParameter(s)) {
			if (!("e".equals(s.trim().toLowerCase()))) {
				LN_BASE = Math.log(base);
				LOG_1 = (float) (Math.log(1) / LN_BASE);
			}
		}
		return true;
	}
}
