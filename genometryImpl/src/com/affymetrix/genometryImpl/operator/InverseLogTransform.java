
package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author auser
 */
public class InverseLogTransform extends AbstractLogTransform implements Operator{
	private static final String BASE_NAME = "inverse_log";
	
	public InverseLogTransform(){
		super();
	}
	
	public InverseLogTransform(Double base) {
		super(base);
	}

	@Override
	protected String getBaseName() {
		return BASE_NAME;
	}
	
	@Override
	public float transform(float x) {
		return (float)(Math.pow(base, x));
	}
}
