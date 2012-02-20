/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.operator;

import java.util.List;

/**
 *
 * @author auser
 */
public class RatioOperator extends AbstractGraphOperator implements Operator{

	@Override
	protected String getSymbol() {
		return "/";
	}

	@Override
	protected float operate(List<Float> operands) {
		if (operands.get(1).floatValue() == 0.0) {
			return 0.0f;
		}
		return operands.get(0).floatValue() / operands.get(1).floatValue();
	}
	
	@Override
	public String getName() {
		return "Ratio";
	}
	
}
