package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author hiralv
 */
public class AddMathTransform extends AbstractMathTransform {
	
	private static final String BASE_NAME = "add";
	
	public AddMathTransform(){
		super();
		paramPrompt = "add";
		parameterized = true;
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
