package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author hiralv
 */
public class MultiplyMathTransform extends AbstractMathTransform {
	
	private static final String BASE_NAME = "multiply";
	
	public MultiplyMathTransform(){
		super();
		paramPrompt = "multiply by";
		parameterized = true;
	}
	
	@Override
	protected String getBaseName() {
		return BASE_NAME;
	}
	
	@Override
	public float transform(float x) {
		return (float)(x * base);
	}
}
