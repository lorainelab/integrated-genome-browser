
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.GenometryConstants;

/**
 *
 * @author lfrohman
 */
public abstract class AbstractLogTransform extends AbstractMathTransform {

	public AbstractLogTransform(Double base) {
		super();
		this.base = base;
		paramPrompt = null;
		name = getBaseName() + "_" + base;
		parameterized = false;
	}
	
	protected abstract String getBaseName();
	
	@Override
	public String getDisplay() {
		if (base == Math.E) {
			return GenometryConstants.BUNDLE.getString("operator_" + getBaseName() + "_ln");
		}
		else {
			return GenometryConstants.BUNDLE.getString("operator_" + getBaseName()) + (base == 0 ? "" : " " + DF.format(base));
		}
	}
}
