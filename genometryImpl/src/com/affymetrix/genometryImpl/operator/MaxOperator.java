package com.affymetrix.genometryImpl.operator;

import java.util.List;

public class MaxOperator implements GraphOperator {
	@Override
	public String getName() {
		return "Max";
	}

	@Override
	public String getSymbol() {
		return null;
	}

	@Override
	public float operate(List<Float> operands) {
		float max = Float.MIN_VALUE;
		for (Float f : operands) {
			max = Math.max(max, f);
		}
		return max;
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
