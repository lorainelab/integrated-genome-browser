
package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author auser
 */
public class InverseLogTransform extends AbsractLogTransform implements Operator{
	
	@Override
	protected String getBaseName() {
		if (base == Math.E) {
			return "Inverse Ln";
		}
		else {
			return "Inverse Log" + DF.format(base);
		}
	}
	
	@Override
	public String getName() {
		return parameterized ? getBaseName() : name;
	}
	
	@Override
	public float transform(float x) {
		return (float)(Math.pow(base, x));
	}
			
}
