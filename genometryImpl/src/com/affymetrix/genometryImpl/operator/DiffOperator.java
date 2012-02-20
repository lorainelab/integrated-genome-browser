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
public class DiffOperator extends AbstractGraphOperator implements Operator{

	@Override
	public String getName() {
		return "Diff" ;
	}

	@Override
	protected float operate(List<Float> operands) {
		return operands.get(0).floatValue() - operands.get(1).floatValue();
	}

	@Override
	protected String getSymbol() {
		return "-";
	}
	
	
}
