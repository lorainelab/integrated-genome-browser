package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author hiralv
 */
public class SubtractMathTransform extends AbstractMathTransform {
	
	private static final String BASE_NAME = "subtract";
	
	public SubtractMathTransform(){
		super();
		paramPrompt = "subtract by";
		parameterized = true;
	}
	
	@Override
	protected String getBaseName() {
		return BASE_NAME;
	}
	
	@Override
	public float transform(float x) {
		return (float)(x - base);
	}
}
