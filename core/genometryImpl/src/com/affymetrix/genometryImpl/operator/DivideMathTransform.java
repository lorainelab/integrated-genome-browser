
package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author hiralv
 */
public class DivideMathTransform extends AbstractMathTransform {
	
	private static final String BASE_NAME = "divide";
	
	public DivideMathTransform(){
		super();
		paramPrompt = "divide by";
		parameterized = true;
	}
	
	@Override
	protected String getBaseName() {
		return BASE_NAME;
	}
	
	@Override
	public float transform(float x) {
		return (float)(x/base);
	}
}
