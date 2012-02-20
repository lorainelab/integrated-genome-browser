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
public class ProductOperator extends AbstractGraphOperator implements Operator{

	@Override
	protected String getSymbol() {
		return null;
	}

	@Override
	protected float operate(List<Float> operands) {
		float total = 1;
		for (Float f : operands) {
			total *= f.floatValue();
		}
		return total;
	}
	
	@Override
	public String getName() {
		return "Product";
	}
	
	
}
