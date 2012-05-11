/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.operator;

import java.util.List;

import com.affymetrix.genometryImpl.GenometryConstants;

/**
 *
 * @author auser
 */
public class MaxOperator extends AbstractGraphOperator implements Operator{

	@Override
	protected String getSymbol() {
		return null;
	}

	@Override
	protected float operate(List<Float> operands) {
		float max = Float.MIN_VALUE;
		for (Float f : operands) {
			max = Math.max(max, f);
		}
		return max;
	}
	
	@Override
	public String getName() {
		return "max";
	}

	@Override
	public String getDisplay() {
		return GenometryConstants.BUNDLE.getString("operator_" + getName());
	}
}
