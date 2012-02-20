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
public class MeanOperator extends AbstractGraphOperator implements Operator{

	@Override
	protected String getSymbol() {
		return null;
	}

	@Override
	protected float operate(List<Float> operands) {
		float total = 0;
		for (Float f : operands) {
			total += f.floatValue();
		}
		return total / (float)operands.size();
	}
	
	@Override
	public String getName() {
		return "Mean";
	}
	
}
