
package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author auser
 */
public class InverseLogTransform extends AbstractLogTransform implements Operator{
	
	public InverseLogTransform(){
		super("Inverse Log");
	}
	
	public InverseLogTransform(Double base) {
		super(base);
	}

	@Override
	protected String getBaseName() {
		if (base == Math.E) {
			return "Inverse Ln";
		}
		else {
			return "Inverse Log" + (base == 0 ? "" : DF.format(base));
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
