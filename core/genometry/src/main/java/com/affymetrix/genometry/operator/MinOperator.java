/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.operator;

import java.util.List;

import com.affymetrix.genometry.GenometryConstants;

/**
 *
 * @author auser
 */
public class MinOperator extends AbstractGraphOperator implements Operator{

	@Override
	protected String getSymbol() {
		return null;
	}

	@Override
	protected float operate(List<Float> operands) {
		float min = Float.MAX_VALUE;
		for (Float f : operands) {
			min = Math.min(min, f);
		}
		return min;
	}
	
	@Override
	public String getName() {
		return "min";
	}

	@Override
	public String getDisplay() {
		return GenometryConstants.BUNDLE.getString("operator_" + getName());
	}
}
