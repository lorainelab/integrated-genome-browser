package com.affymetrix.genometryImpl.operator;

import java.util.List;

public class SumOperator extends AbstractGraphOperator implements Operator {

	@Override
	public String getName() {
		return "Sum";
	}

	@Override
	protected String getSymbol() {
		return null;
	}

	@Override
	public float operate(List<Float> operands) {
		float total = 0;
		for (Float f : operands) {
			total += f.floatValue();
		}
		return total;
	}
}
