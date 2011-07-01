package com.affymetrix.genometryImpl.operator.transform;

import java.text.DecimalFormat;

public final class LogTransform implements FloatTransformer {
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
		return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
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
			LN_BASE = Math.log(base);
			LOG_1 = (float)(Math.log(1)/LN_BASE);
		}
		return true;
	}
}
