package com.affymetrix.genometryImpl.operator;

import java.util.List;

public class SumOperator implements GraphOperator {
	@Override
	public String getName() {
		return "Sum";
	}

	@Override
	public String getSymbol() {
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

	@Override
	public int getOperandCountMin() {
		return 2;
	}

	@Override
	public int getOperandCountMax() {
		return Integer.MAX_VALUE;
	}

}
