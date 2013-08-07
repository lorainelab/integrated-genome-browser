package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author hiralv
 */
public class AddMathTransform extends AbstractMathTransform {
	
	private static final String BASE_NAME = "add";
	private static final String PARAMETER_NAME = "base";
	
	@Override
	protected String getParameterName(){
		return PARAMETER_NAME;
	}
	
	@Override
	protected String getBaseName() {
		return BASE_NAME;
	}
	
	@Override
	public float transform(float x) {
		return (float)(x + base);
	}
}
